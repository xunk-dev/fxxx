package com.frameout.app.ads

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.glass.GlassSurface
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.frameout.app.BuildConfig
import com.frameout.app.FrameOutApp
import com.frameout.core.ads.AdStrings
import com.frameout.core.pro.ProFeature
import com.frameout.designsystem.components.DialogTone
import com.frameout.designsystem.components.FrameConfirmDialog
import com.frameout.designsystem.icons.FrameIconsExtra

@Composable
private fun rememberHub(): AdsHub = (LocalContext.current.applicationContext as FrameOutApp).ads

private const val AD_INNER_PAD_DP = 4
private const val AD_LABEL_STRIP_DP = 14
private const val AD_CARD_RADIUS_DP = 14
internal const val AD_CARD_INSET_DP = 24 + 2 * AD_INNER_PAD_DP

internal fun bannerSlotHeightDp(hub: AdsHub, widthDp: Int): Int {
    val bannerWidth = (widthDp - (AD_CARD_INSET_DP - 24)).coerceAtLeast(120)
    return hub.bannerHeightDp(bannerWidth) + AD_LABEL_STRIP_DP + AD_INNER_PAD_DP + 8
}

@Composable
fun AdBannerSlot(reserve: Boolean = false, horizontalPad: Dp = 12.dp) {
    if (!com.frameout.app.LocalTabActive.current) return
    val hub = rememberHub()
    val state by hub.bannerState.collectAsState()
    if (state == BannerState.Hidden) {
        if (reserve && hub.active()) {
            BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = horizontalPad, vertical = 4.dp)) {
                val bannerWidth = (maxWidth.value.toInt() - (AD_CARD_INSET_DP - 24)).coerceAtLeast(120)
                val height = hub.bannerHeightDp(bannerWidth) + AD_LABEL_STRIP_DP + AD_INNER_PAD_DP
                Spacer(Modifier.fillMaxWidth().height(height.dp))
            }
        }
        return
    }
    val colors = FrameTheme.colors
    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = horizontalPad, vertical = 4.dp)) {
        val bannerWidth = (maxWidth.value.toInt() - (AD_CARD_INSET_DP - 24)).coerceAtLeast(120)
        val reserved = remember(bannerWidth) { hub.bannerHeightDp(bannerWidth) }
        GlassSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(AD_CARD_RADIUS_DP.dp)) {
            Box(Modifier.fillMaxWidth()) {
                Row(Modifier.align(Alignment.TopStart).padding(start = 10.dp, top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(4.dp).clip(CircleShape).background(colors.primary))
                    Spacer(Modifier.width(5.dp))
                    FrameText(AdStrings.label(), style = FrameTheme.typography.label.copy(fontSize = 9.sp, lineHeight = 11.sp), color = colors.onSurfaceVariant, maxLines = 1)
                }
                Box(
                    Modifier.fillMaxWidth()
                        .padding(start = AD_INNER_PAD_DP.dp, end = AD_INNER_PAD_DP.dp, top = AD_LABEL_STRIP_DP.dp, bottom = AD_INNER_PAD_DP.dp)
                        .heightIn(min = reserved.dp)
                        .clip(RoundedCornerShape((AD_CARD_RADIUS_DP - AD_INNER_PAD_DP).dp)),
                    Alignment.Center
                ) {
                    AdBannerView(Modifier.fillMaxWidth(), BuildConfig.ADMOB_BANNER_ID, bannerWidth, hub::onBannerResult)
                }
            }
        }
    }
}

@Composable
fun AdsHost() {
    val app = LocalContext.current.applicationContext as FrameOutApp
    val hub = app.ads
    val blocked by hub.blockedNotice.collectAsState()
    if (!blocked) return
    FrameConfirmDialog(
        title = AdStrings.blockedTitle(),
        body = AdStrings.blockedBody(),
        confirmLabel = AdStrings.rewardPro(),
        dismissLabel = AdStrings.blockedClose(),
        onConfirm = {
            hub.dismissBlockedNotice()
            app.container.entitlements.showPaywall(ProFeature.General)
        },
        onDismiss = hub::dismissBlockedNotice,
        tone = DialogTone.Info,
        icon = FrameIconsExtra.Warning
    )
}

@Composable
fun RewardOffer(feature: ProFeature, onPro: () -> Unit, onFinished: () -> Unit) {
    val hub = rememberHub()
    FrameConfirmDialog(
        title = AdStrings.rewardTitle(),
        body = if (com.frameout.core.pro.ProAccess.source?.skipOfferActive == true) AdStrings.rewardOnceBody() else AdStrings.rewardBody(),
        confirmLabel = AdStrings.rewardWatch(),
        dismissLabel = AdStrings.rewardPro(),
        onConfirm = { hub.showReward(feature) { earned -> if (earned) onFinished() else onPro() } },
        onDismiss = onPro,
        tone = DialogTone.Info
    )
}

@Composable
fun rewardOfferAvailable(feature: ProFeature): Boolean {
    val hub = rememberHub()
    val ready by hub.rewardReady.collectAsState()
    return ready && feature != ProFeature.General && hub.rewardAvailable()
}
