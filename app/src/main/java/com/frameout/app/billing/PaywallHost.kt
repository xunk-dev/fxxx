package com.frameout.app.billing

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import com.frameout.app.BuildConfig
import com.frameout.app.FrameOutApp
import com.frameout.app.applyAccent
import com.frameout.core.pro.ProCatalog
import com.frameout.core.pro.Entitlements
import com.frameout.core.pro.ProFeature
import com.frameout.designsystem.theme.FrameAccent
import androidx.compose.ui.platform.LocalContext
import com.frameout.designsystem.theme.resolveFrameAccent

@Composable
fun PaywallHost(activity: Activity, app: FrameOutApp) {
    val entitlements = app.container.entitlements
    val feature by entitlements.paywall.collectAsState()
    val pro by entitlements.isPro.collectAsState()
    val manager = app.billing
    LaunchedEffect(feature) {
        if (feature != null) {
            manager.clearNote()
            manager.start()
        }
    }
    var celebrating by rememberCelebrating(app, feature, pro)
    if (celebrating) ProUnlockedDialog { celebrating = false; entitlements.dismissPaywall() }
    var skipOffer by remember(feature) { mutableStateOf(false) }
    val adEligible by entitlements.paywallAdEligible.collectAsState()
    val tint = rememberPaywallTint(entitlements)
    val direct by entitlements.paywallDirect.collectAsState()
    var adRunning by remember(feature) { mutableStateOf(false) }
    val rewardOffer = adEligible && (feature?.let { com.frameout.app.ads.rewardOfferAvailable(it) } ?: false)
    feature?.takeIf { !celebrating }?.let { shown ->
        if (direct && !skipOffer && (rewardOffer || adRunning)) {
            com.frameout.app.ads.DirectReward(shown, onStart = { adRunning = true }, onPro = { entitlements.dismissPaywall() }, onFinished = { entitlements.takeAfterReward()?.invoke(); entitlements.dismissPaywall() })
        } else if (rewardOffer && !skipOffer) {
            RetintedTheme(tint) {
                com.frameout.app.ads.RewardOffer(shown, onPro = { skipOffer = true }, onFinished = { entitlements.takeAfterReward()?.invoke(); entitlements.dismissPaywall() })
            }
        } else if (BuildConfig.REVENUECAT_PAYWALL && RevenueCatSync.isActive) {
            RevenueCatPaywall(onDismiss = entitlements::dismissPaywall)
        } else {
            PaywallSheetContent(shown, entitlements, manager, tint, activity)
        }
    }
}

@Composable
private fun PaywallSheetContent(
    shown: ProFeature,
    entitlements: Entitlements,
    manager: BillingManager,
    tint: FrameAccent?,
    activity: Activity
) {
    CompositionLocalProvider(LocalPaywallSample provides PaywallSample.image) {
        PaywallSheet(
            feature = shown,
            trialsLeft = if (shown == ProFeature.General || ProCatalog.TRIALS_PER_FEATURE == 0) null else entitlements.trialsLeft(shown),
            manager = manager,
            initialAccent = tint,
            onLaunch = { offer -> manager.launch(activity, offer) },
            onDismiss = entitlements::dismissPaywall
        )
    }
}

@Composable
private fun rememberPaywallTint(entitlements: Entitlements): FrameAccent? {
    val accentKey by entitlements.paywallAccent.collectAsState()
    val context = LocalContext.current
    return remember(accentKey) { accentKey?.let { resolveFrameAccent(it, context) } }
}

@Composable
private fun rememberCelebrating(app: FrameOutApp, feature: ProFeature?, pro: Boolean): MutableState<Boolean> {
    val entitlements = app.container.entitlements
    val celebrating = remember { mutableStateOf(false) }
    var wasPro by remember { mutableStateOf(pro) }
    LaunchedEffect(pro) {
        if (pro && !wasPro && feature != null) {
            entitlements.paywallAccent.value?.let { app.applyAccent(it) }
            celebrating.value = true
        } else if (pro && feature != null) entitlements.dismissPaywall()
        wasPro = pro
    }
    return celebrating
}
