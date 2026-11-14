package com.frameout.core.pro

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class Entitlements(context: Context, val enforced: Boolean) {
    private val appContext = context.applicationContext
    private val encrypted: SharedPreferences? = runCatching { ProPrefs.encrypted(context) }.getOrNull()

    internal val trusted: Boolean = encrypted != null
    internal val prefs: SharedPreferences =
        encrypted ?: context.applicationContext.getSharedPreferences(FALLBACK_FILE, Context.MODE_PRIVATE)

    val welcome = WelcomeGrant(PrefsGrantStore(prefs), trusted)
    val accentSeal = AccentSeal(PrefsGrantStore(prefs, "accent_seal"), trusted)
    @Volatile internal var vaultGranted = false
    internal val _accentForced = MutableStateFlow<String?>(null)
    val accentForced: StateFlow<String?> = _accentForced.asStateFlow()
    internal val _welcomeLock = MutableStateFlow<String?>(null)
    val welcomeLock: StateFlow<String?> = _welcomeLock.asStateFlow()

    @Volatile private var sessionActive = false
    @Volatile private var sessionPlan = ""

    @Volatile private var locked = false
    @Volatile private var strict = false
    private val tickets = HashMap<ProFeature, Long>()
    private val rewards = HashMap<ProFeature, Long>()
    private val triedItems = HashSet<String>()
    @Volatile private var afterReward: (() -> Unit)? = null
    @Volatile private var skipOfferShown = false
    internal val skipLock = Any()

    @Volatile internal var vault: TrialVault? = null
    @Volatile internal var vaultVerified = false
    @Volatile internal var vaultBusy = false
    @Volatile internal var vaultLastTry = 0L
    @Volatile internal var wipes = 0
    internal var wipeCounted = false
    internal val freshStore: Boolean = !prefs.getBoolean(KEY_SEEDED, false)

    internal val _skipTrial = MutableStateFlow(readSkipTrial())
    val skipTrial: StateFlow<Int> = _skipTrial.asStateFlow()

    private val _isPro = MutableStateFlow(false)
    val isPro: StateFlow<Boolean> = _isPro.asStateFlow()

    private val _paywall = MutableStateFlow<ProFeature?>(null)
    val paywall: StateFlow<ProFeature?> = _paywall.asStateFlow()

    private val _paywallAccent = MutableStateFlow<String?>(null)
    val paywallAccent: StateFlow<String?> = _paywallAccent.asStateFlow()

    private val _paywallAdEligible = MutableStateFlow(true)
    val paywallAdEligible: StateFlow<Boolean> = _paywallAdEligible.asStateFlow()
    private val _paywallDirect = MutableStateFlow(false)
    val paywallDirect: StateFlow<Boolean> = _paywallDirect.asStateFlow()

    private val _epoch = MutableStateFlow(0)
    val epoch: StateFlow<Int> = _epoch.asStateFlow()

    init {
        if (!prefs.getBoolean(KEY_SEEDED, false)) {
            prefs.edit().putBoolean(KEY_SEEDED, true).apply()
        }
        if (!prefs.contains(KEY_SMART_SINCE)) prefs.edit().putLong(KEY_SMART_SINCE, System.currentTimeMillis()).apply()
        _isPro.value = computePro(System.currentTimeMillis())
        ProAccess.source = this
    }

    private fun computePro(now: Long): Boolean {
        if (!enforced) return true
        if (locked) return false
        if (!trusted || strict) return sessionActive
        val highWater = prefs.getLong(KEY_HIGH_WATER, 0L)
        val fresh = ProFreshness.fresh(
            now = now,
            verifiedAt = prefs.getLong(KEY_VERIFIED, 0L),
            highWater = highWater,
            graceMs = ProCatalog.GRACE_DAYS * DAY_MS,
            skewMs = SKEW_MS
        )
        if (now > highWater) prefs.edit().putLong(KEY_HIGH_WATER, now).apply()
        return fresh && prefs.getBoolean(KEY_ACTIVE, false)
    }

    fun applyBillingResult(active: Boolean, plan: String) {
        val now = System.currentTimeMillis()
        sessionActive = active
        sessionPlan = if (active) plan else ""
        if (trusted) {
            prefs.edit().putBoolean(KEY_ACTIVE, active).putString(KEY_PLAN, sessionPlan)
                .putLong(KEY_VERIFIED, now).putLong(KEY_HIGH_WATER, now).apply()
        }
        _isPro.value = computePro(now)
    }

    fun applyIntegrity(tampered: Boolean, hooked: Boolean) {
        if ((!tampered || locked) && (!hooked || strict)) return
        if (tampered) locked = true
        if (hooked) strict = true
        _isPro.value = computePro(System.currentTimeMillis())
    }

    val plan: String get() = if (trusted) prefs.getString(KEY_PLAN, "") ?: "" else sessionPlan

    fun shouldAutoRestore(): Boolean = enforced && !_isPro.value && !prefs.getBoolean(KEY_AUTO_RESTORED, false)

    fun markAutoRestored() { prefs.edit().putBoolean(KEY_AUTO_RESTORED, true).apply() }

    internal fun mono(): Long = SystemClock.elapsedRealtime()

    fun permits(feature: ProFeature): Boolean {
        if (locked) return false
        if (_isPro.value) return true
        val until = synchronized(tickets) { tickets[feature] } ?: return false
        return mono() < until
    }

    fun trialsLeft(feature: ProFeature): Int =
        (ProCatalog.TRIALS_PER_FEATURE - prefs.getInt(trialKey(feature), 0)).coerceAtLeast(0)

    fun tryOnce(feature: ProFeature, item: String = ""): Boolean {
        if (_isPro.value) return true
        val tag = feature.key + ":" + item
        if (item.isNotEmpty() && tag in triedItems) return true
        if (trialsLeft(feature) <= 0) return false
        prefs.edit().putInt(trialKey(feature), prefs.getInt(trialKey(feature), 0) + 1).apply()
        if (item.isNotEmpty()) triedItems.add(tag)
        synchronized(tickets) { tickets[feature] = mono() + TICKET_MS }
        return true
    }

    fun grantTicket(feature: ProFeature, durationMs: Long) {
        if (locked) return
        if (skipOfferShown && feature == ProFeature.SmartExtract) { armSkipTrial(); return }
        val until = mono() + durationMs
        synchronized(tickets) { tickets[feature] = until }
        synchronized(rewards) { rewards[feature] = until }
        _epoch.value = _epoch.value + 1
    }

    fun spendTicket(feature: ProFeature): Boolean {
        if (locked) return false
        val live = synchronized(tickets) { val until = tickets.remove(feature); until != null && mono() < until }
        if (!live) return false
        synchronized(rewards) { rewards.remove(feature) }
        _epoch.value = _epoch.value + 1
        return true
    }

    @Volatile private var exportPassUntil = 0L

    fun grantExportPass() {
        if (locked) return
        synchronized(tickets) { exportPassUntil = mono() + TICKET_MS }
    }

    @Volatile private var exportBatch = 0

    fun exportPermitted(): Boolean {
        if (!enforced || _isPro.value) return true
        if (locked) return false
        return synchronized(tickets) {
            if (exportBatch > 0) return@synchronized true
            val live = exportPassUntil > mono()
            exportPassUntil = 0L
            live
        }
    }

    fun beginExportBatch(): Boolean {
        if (!exportPermitted()) return false
        synchronized(tickets) { exportBatch++ }
        return true
    }

    fun endExportBatch() {
        synchronized(tickets) { if (exportBatch > 0) exportBatch-- }
    }

    fun allows(feature: ProFeature, tier: ProTier): Boolean = when {
        !enforced -> true
        tier == ProTier.Free -> true
        _isPro.value -> true
        tier == ProTier.Ad -> permits(feature)
        else -> false
    }

    fun rewarded(feature: ProFeature): Boolean {
        val until = synchronized(rewards) { rewards[feature] } ?: return false
        return mono() < until
    }

    fun gate(feature: ProFeature, free: Boolean, item: String, action: () -> Unit) {
        if (!enforced || free || tryOnce(feature, item)) action() else showPaywall(feature)
    }

    private fun readSkipTrial(): Int =
        if (!trusted) SKIP_USED else runCatching { prefs.getInt(KEY_SKIP, SKIP_NEW) }.getOrDefault(SKIP_USED)

    private fun skipEligible(): Boolean = enforced && trusted && !locked && !strict && vaultVerified && !riskBlocked()

    private fun riskBlocked(): Boolean = DeviceRisk.blocked(appContext, prefs.getLong(KEY_HIGH_WATER, 0L), SKEW_MS, wipes)

    fun attachVault(v: TrialVault) {
        vault = v
        syncVault(force = true)
    }

    fun welcomeSpent(): Boolean = vaultGranted

    @Volatile internal var trialStart = 0L
    @Volatile internal var trialLast = 0L

    fun syncVault(force: Boolean = false) = syncVaultImpl(force)

    fun noteTrialPick(key: String): Boolean = noteTrialPickImpl(key)

    fun trialExpired(): Boolean = trialExpiredImpl()

    fun settleTrial(current: String): Boolean = settleTrialImpl(current)

    fun noteWelcomeCommitted() = syncVault(force = true)

    fun smartStampSince(): Long = prefs.getLong(KEY_SMART_SINCE, Long.MAX_VALUE)

    val skipOfferActive: Boolean get() = skipOfferShown

    fun skipTrialArmed(): Boolean = skipEligible() && _skipTrial.value == SKIP_ARMED

    private fun skipTrialOffered(): Boolean = skipEligible() && _skipTrial.value == SKIP_NEW

    private fun armSkipTrial() = synchronized(skipLock) {
        if (skipTrialOffered() && prefs.edit().putInt(KEY_SKIP, SKIP_ARMED).commit()) { _skipTrial.value = SKIP_ARMED; flushVault() }
    }

    fun consumeSkipTrial(): Boolean = synchronized(skipLock) {
        if (!skipTrialArmed() || !prefs.edit().putInt(KEY_SKIP, SKIP_USED).commit()) return false
        _skipTrial.value = SKIP_USED
        flushVault()
        true
    }

    fun gateSkip(action: () -> Unit) {
        when {
            !enforced || _isPro.value || skipTrialArmed() -> action()
            skipTrialOffered() -> { showPaywall(ProFeature.SmartExtract, adEligible = true, after = action); skipOfferShown = true }
            else -> { syncVault(); showPaywall(ProFeature.SmartExtract, adEligible = false) }
        }
    }

    fun takeAfterReward(): (() -> Unit)? = afterReward.also { afterReward = null }

    fun showPaywall(feature: ProFeature, adEligible: Boolean = true, accentKey: String? = null, after: (() -> Unit)? = null, direct: Boolean = false) {
        afterReward = after
        _paywallDirect.value = direct
        skipOfferShown = false
        _paywallAdEligible.value = adEligible
        _paywallAccent.value = accentKey
        _paywall.value = feature
    }

    fun dismissPaywall() {
        afterReward = null
        skipOfferShown = false
        _paywallDirect.value = false
        _paywall.value = null
        _paywallAccent.value = null
    }

    private fun trialKey(feature: ProFeature) = "trial_" + feature.key

    companion object {
        const val SKIP_NEW = 0
        const val SKIP_ARMED = 1
        const val SKIP_USED = 2
    }
}
