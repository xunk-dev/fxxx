package com.frameout.app.trim

import com.frameout.designsystem.motion.frameSpring
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.TrimStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.glass.frameGlow
import com.frameout.designsystem.haptics.FrameHaptic
import com.frameout.designsystem.haptics.rememberFrameHaptics
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameCardEdge
import com.frameout.designsystem.theme.frameControl
import com.frameout.engine.player.PlaybackSpeeds

private val SheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
private val INFO_MIN_HEIGHT = 64.dp
private val GAP = 12.dp
private val SIDE = 16.dp

@Composable
internal fun TrimPanel(env: TrimEnv, maxHeight: Dp, modifier: Modifier = Modifier, landscape: Boolean = false) {
    if (landscape) LandscapePanel(env, maxHeight, modifier) else SheetPanel(env, maxHeight, modifier)
}

@Composable
private fun Modifier.panelSurface(shape: Shape): Modifier {
    val glowAlpha = if (FrameTheme.colors.isDark) 0.20f else 0.10f
    val tint = FrameTheme.colors.primary.copy(alpha = if (FrameTheme.colors.isDark) 0.12f else 0.07f)
    return this
        .frameGlow(FrameTheme.colors.primary, { glowAlpha }, 14.dp, { 0.12f })
        .background(FrameTheme.colors.surface.copy(alpha = 0.94f), shape)
        .background(Brush.verticalGradient(listOf(tint, Color.Transparent)), shape)
        .border(1.dp, frameCardEdge(FrameTheme.colors.primary, 0.50f), shape)
}

@Composable
private fun LandscapePanel(env: TrimEnv, maxHeight: Dp, modifier: Modifier) {
    Column(
        modifier
            .heightIn(max = maxHeight)
            .panelSurface(FrameShapes.lg)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(start = SIDE, end = SIDE, top = SIDE, bottom = 14.dp),
        Arrangement.spacedBy(GAP)
    ) {
        PanelHeader(env)
        LiveTimeline(env)
        RangeEditor(env)
        TransportRow(env)
        OutputInfo(env)
        PanelActions(env)
    }
}

@Composable
private fun SheetPanel(env: TrimEnv, maxHeight: Dp, modifier: Modifier) {
    val scope = rememberCoroutineScope()
    val haptics = rememberFrameHaptics()
    val fold = remember { PanelFold(scope) { haptics.perform(FrameHaptic.Tap) } }
    Column(
        modifier
            .heightIn(max = maxHeight)
            .graphicsLayer { translationY = (-fold.overshoot * fold.natural * 0.6f).coerceIn(-14.dp.toPx(), 28.dp.toPx()) }
            .panelSurface(SheetShape)
            .navigationBarsPadding()
    ) {
        PanelGrabber(fold)
        Column(Modifier.padding(horizontal = SIDE)) {
            PanelHeader(env)
            Spacer(Modifier.height(GAP))
            LiveTimeline(env)
        }
        Foldable(fold, Modifier.weight(1f, fill = false).padding(horizontal = SIDE)) {
            RangeEditor(env)
            OutputInfo(env)
        }
        Column(Modifier.padding(start = SIDE, end = SIDE, top = GAP, bottom = 14.dp), Arrangement.spacedBy(GAP)) {
            TransportRow(env)
            PanelActions(env)
        }
    }
}

@Composable
private fun PanelHeader(env: TrimEnv) {
    val ui by env.controller.state.collectAsState()
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        ControlCluster {
            if (ui.hasAudio && !ui.animated) {
                MuteKey(ui.muted, env.controller)
                Box(Modifier.width(1.dp).height(16.dp).background(FrameTheme.colors.onSurfaceVariant.copy(alpha = 0.22f)))
            }
            SpeedKey(env.player.speed, env.controller::cycleSpeed)
        }
        SnapshotButton(env)
    }
}

@Composable
private fun ControlCluster(content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.height(KeyHeight).frameControl(FrameShapes.pill).clip(FrameShapes.pill),
        Arrangement.Center,
        Alignment.CenterVertically,
        content = content
    )
}

private val KeyHeight = 40.dp
private val KeyInset = 4.dp
private val KeyPill = KeyHeight - KeyInset * 2

@Composable
private fun SpeedKey(speed: Float, onClick: () -> Unit) {
    val label = PlaybackSpeeds.label(speed)
    val active = speed != PlaybackSpeeds.NORMAL
    val tint = if (active) FrameTheme.colors.primary else FrameTheme.colors.onSurfaceVariant
    val description = FrameStrings.playbackSpeed(label)
    Box(
        Modifier
            .height(KeyHeight)
            .widthIn(min = 44.dp)
            .clip(FrameShapes.pill)
            .frameClickable(onClick = onClick)
            .semantics { contentDescription = description }
            .padding(horizontal = KeyInset),
        Alignment.Center
    ) {
        Row(
            Modifier
                .animateContentSize(frameSpring())
                .height(KeyPill)
                .widthIn(min = KeyPill)
                .clip(FrameShapes.pill)
                .background(if (active) FrameTheme.colors.primary.copy(alpha = 0.18f) else Color.Transparent)
                .padding(horizontal = if (active) 10.dp else 0.dp),
            Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            Alignment.CenterVertically
        ) {
            Image(painterResource(FrameIconsExtra.Speed), null, Modifier.size(if (active) 18.dp else 20.dp), colorFilter = ColorFilter.tint(tint))
            if (active) FrameText(label, style = FrameTheme.typography.label, color = tint, maxLines = 1)
        }
    }
}

@Composable
private fun LiveTimeline(env: TrimEnv) {
    val position by env.controller.position.collectAsState()
    TimelineBar(
        env.player.durationMs, position, env.clamped, true, env.controller::seekTo, env.onRange, env.thumbs,
        modifier = Modifier.fillMaxWidth(), onScrub = env.controller::scrub, slideWindow = env.output.limited,
        onDragSeek = env.controller::scrubTo
    )
}

@Composable
private fun OutputInfo(env: TrimEnv) {
    if (env.output == TrimOutput.Frames) return
    Box(Modifier.fillMaxWidth().heightIn(min = INFO_MIN_HEIGHT), Alignment.Center) {
        if (env.output.limited) OutputHint(env) else FrameText(
            TrimStrings.outDesc(outputKind(env.output)),
            style = FrameTheme.typography.bodySmall,
            color = FrameTheme.colors.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}
