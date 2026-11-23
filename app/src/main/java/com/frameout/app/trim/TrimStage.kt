package com.frameout.app.trim

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.frameout.app.FrameOutApp
import com.frameout.app.ads.AdBannerSlot
import com.frameout.app.ads.bannerSlotHeightDp

private const val MAX_VIDEO_LOSS = 0.88f
private val MIN_VIDEO_HEIGHT = 170.dp
private val PLAYER_PAD_H = 32.dp
private val PLAYER_PAD_V = 16.dp

@Composable
internal fun TrimStage(env: TrimEnv, modifier: Modifier = Modifier) {
    val hub = (LocalContext.current.applicationContext as FrameOutApp).ads
    BoxWithConstraints(modifier) {
        val aspect = env.player.videoAspect
        val allow = hub.active() && aspect > 0f && run {
            val bannerH = bannerSlotHeightDp(hub, maxWidth.value.toInt()).dp
            val fullHeight = (maxWidth - PLAYER_PAD_H) / aspect
            val room = maxHeight - PLAYER_PAD_V
            val before = minOf(fullHeight, room)
            val after = minOf(fullHeight, room - bannerH)
            after >= MIN_VIDEO_HEIGHT && after >= before * MAX_VIDEO_LOSS
        }
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxWidth().animateContentSize()) { if (allow) AdBannerSlot() }
            PlayerArea(env.controller, aspect, Modifier.weight(1f).fillMaxWidth(), env.fx)
        }
    }
}
