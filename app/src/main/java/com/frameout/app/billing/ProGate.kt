package com.frameout.app.billing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.frameout.app.FrameOutApp
import com.frameout.core.pro.Entitlements
import com.frameout.core.pro.ProCatalog
import com.frameout.core.pro.ProFeature
import com.frameout.core.pro.ProTier

@Stable
class ProGate(
    private val entitlements: Entitlements,
    val isPro: Boolean,
    private val skipArmed: Boolean = false,
    val welcomeKey: String? = null,
    val welcomeOpen: Boolean = false
) {
    val enforced: Boolean get() = entitlements.enforced

    fun locked(free: Boolean): Boolean = enforced && !isPro && !free

    fun locked(feature: ProFeature, tier: ProTier): Boolean = !entitlements.allows(feature, tier)

    fun use(feature: ProFeature, tier: ProTier, autoRun: Boolean = false, action: () -> Unit) {
        if (entitlements.allows(feature, tier)) action()
        else entitlements.showPaywall(feature, adEligible = tier == ProTier.Ad, after = if (autoRun && tier == ProTier.Ad) action else null)
    }

    fun run(feature: ProFeature, item: String = "", accentKey: String? = null, action: () -> Unit) {
        if (entitlements.rewarded(feature) || entitlements.tryOnce(feature, item)) action() else entitlements.showPaywall(feature, accentKey = accentKey)
    }

    fun needsWelcomeClaim(key: String): Boolean =
        enforced && !isPro && welcomeOpen

    fun tryWelcomePick(key: String): Boolean = entitlements.noteTrialPick(key)

    fun skipUsable(): Boolean = !enforced || isPro || skipArmed

    fun skip(action: () -> Unit) = entitlements.gateSkip(action)

    fun rewarded(feature: ProFeature): Boolean = entitlements.rewarded(feature)

    fun showPro() = entitlements.showPaywall(ProFeature.General)

    fun showProFor(feature: ProFeature, accentKey: String) = entitlements.showPaywall(feature, adEligible = false, accentKey = accentKey)

    fun require(feature: ProFeature, action: () -> Unit) {
        if (entitlements.permits(feature)) action() else entitlements.showPaywall(feature)
    }

    fun gated(feature: ProFeature, free: Boolean, item: String = "", action: () -> Unit) {
        if (free) action() else run(feature, item, action = action)
    }
}

@Composable
fun rememberProLocked(): Boolean = rememberProGate().locked(false)

@Composable
fun rememberProGate(): ProGate {
    val app = LocalContext.current.applicationContext as FrameOutApp
    val entitlements = app.container.entitlements
    val pro by entitlements.isPro.collectAsState()
    val epoch by entitlements.epoch.collectAsState()
    val trial by entitlements.skipTrial.collectAsState()
    val welcome by entitlements.welcome.key.collectAsState()
    val welcomeOpen by entitlements.welcome.open.collectAsState()
    return remember(entitlements, pro, epoch, trial, welcome, welcomeOpen) {
        ProGate(entitlements, pro, trial == Entitlements.SKIP_ARMED, welcome, welcomeOpen)
    }
}
