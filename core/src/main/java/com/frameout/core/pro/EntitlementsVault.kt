package com.frameout.core.pro

import android.os.SystemClock

internal fun Entitlements.syncVaultImpl(force: Boolean = false) {
    val v = vault ?: return
    if (!enforced || !trusted) return
    val now = mono()
    synchronized(skipLock) {
        if (vaultBusy) return
        if (!force && vaultVerified && !prefs.getBoolean(KEY_VAULT_DIRTY, false)) return
        if (!force && now - vaultLastTry < VAULT_RETRY_MS) return
        vaultBusy = true
        vaultLastTry = now
    }
    val started = runCatching { v.read { bytes -> onVaultRead(bytes) } }.isSuccess
    if (!started) synchronized(skipLock) { vaultBusy = false }
}

internal fun Entitlements.onVaultRead(bytes: ByteArray?) {
    synchronized(skipLock) {
        vaultBusy = false
        if (bytes == null) return
        val snap = TrialLedger.decode(bytes)
        val remote = snap?.state
        if (snap?.granted == true) {
            vaultGranted = true
            welcome.exhaust()
            val remembered = snap.accent
            if (remembered != null && ProCatalog.welcomePickable(remembered) && (!welcome.committed || welcome.key.value == WelcomeGrant.FALLBACK)) restoreWelcomeImpl(remembered)
            if (welcome.committed && !prefs.getBoolean(KEY_GRANT_SYNCED, false)) revokeSpentGrant()
        }
        val local = _skipTrial.value
        val merged = TrialLedger.merge(local, remote)
        val knownWipes = maxOf(snap?.wipes ?: 0, prefs.getInt(KEY_WIPES, 0))
        val newWipes = if (freshStore && snap != null && !wipeCounted) knownWipes + 1 else knownWipes
        wipeCounted = true
        val wipesChanged = newWipes != (snap?.wipes ?: 0)
        wipes = newWipes
        prefs.edit().putInt(KEY_WIPES, newWipes).apply()
        if (merged != local) {
            if (!prefs.edit().putInt(KEY_SKIP, merged).commit()) return
            _skipTrial.value = merged
        }
        vaultVerified = true
        val vaultBehind = remote == null && merged != TrialLedger.NEW || remote != null && remote != merged
        if (vaultBehind || wipesChanged || welcome.spent && !vaultGranted || prefs.getBoolean(KEY_VAULT_DIRTY, false)) flushVault()
    }
}

internal fun Entitlements.rememberedAccent(): String? =
    _welcomeLock.value ?: welcome.key.value?.takeIf { welcome.committed && it != WelcomeGrant.FALLBACK }

internal fun Entitlements.restoreWelcomeImpl(key: String) {
    prefs.edit().putBoolean(KEY_GRANT_SYNCED, true).apply()
    if (!welcome.restore(key)) return
    accentSeal.set(key)
    _welcomeLock.value = key
    _accentForced.value = key
}

internal fun Entitlements.flushVault() {
    val v = vault ?: return
    val state = _skipTrial.value
    prefs.edit().putBoolean(KEY_VAULT_DIRTY, true).apply()
    runCatching {
        v.write(TrialLedger.encode(state, wipes, welcome.spent, rememberedAccent())) { ok ->
            if (!ok) return@write
            if (welcome.spent) {
                vaultGranted = true
                prefs.edit().putBoolean(KEY_GRANT_SYNCED, true).apply()
            }
            if (_skipTrial.value == state) prefs.edit().putBoolean(KEY_VAULT_DIRTY, false).apply() else flushVault()
        }
    }
}

internal fun Entitlements.welcomeClaimable(): Boolean =
    enforced && welcome.open.value && !vaultGranted && (vault == null || vaultVerified)

internal fun Entitlements.noteTrialPickImpl(key: String): Boolean {
    val now = SystemClock.elapsedRealtime()
    if (welcome.trial.value) {
        trialLast = now
        return true
    }
    if (ProCatalog.accentIsFree(key)) return true
    if (!welcomeClaimable() || !welcome.beginTrial(vaultGranted)) return false
    trialStart = now
    trialLast = now
    noteWelcomeCommitted()
    return true
}

internal fun Entitlements.trialExpiredImpl(): Boolean {
    if (!welcome.trial.value) return false
    val now = SystemClock.elapsedRealtime()
    return now - trialLast > TRIAL_IDLE_MS || now - trialStart > TRIAL_CAP_MS
}

internal fun Entitlements.settleTrialImpl(current: String): Boolean {
    if (!welcome.trial.value) return false
    val applied = accentSeal.key() ?: current
    val chosen = welcome.settle(applied) ?: return false
    accentSeal.set(chosen)
    if (chosen != current) _accentForced.value = chosen
    noteWelcomeCommitted()
    return true
}

internal fun Entitlements.revokeSpentGrant() {
    prefs.edit().putBoolean(KEY_GRANT_SYNCED, true).apply()
    val old = welcome.revoke() ?: return
    if (accentSeal.key() != old) return
    accentSeal.set(WelcomeGrant.FALLBACK)
    _accentForced.value = WelcomeGrant.FALLBACK
}
