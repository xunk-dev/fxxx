package com.frameout.app.viewer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import kotlinx.coroutines.withTimeoutOrNull
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.haptics.FrameHaptic
import com.frameout.designsystem.haptics.rememberFrameHaptics
import com.frameout.designsystem.theme.FrameGradients
import com.frameout.designsystem.theme.FrameTheme

private val ItemH = 36.dp
private val PopPadV = 6.dp
private val PopGap = 10.dp
private val PopW = 56.dp

@Composable
internal fun RowScopePlayButton(
    playing: Boolean,
    fps: Int,
    icon: Int,
    label: String,
    onPlay: () -> Unit,
    onSetFps: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberFrameHaptics()
    val density = LocalDensity.current
    val longPressMs = LocalViewConfiguration.current.longPressTimeoutMillis
    val currentFps by rememberUpdatedState(fps)
    val setFps by rememberUpdatedState(onSetFps)
    val tap by rememberUpdatedState(onPlay)
    var open by remember { mutableStateOf(false) }
    var hover by remember { mutableIntStateOf(-1) }
    val speeds = PLAY_SPEEDS
    val itemPx = with(density) { ItemH.toPx() }
    val padPx = with(density) { PopPadV.toPx() }
    val gapPx = with(density) { PopGap.toPx() }
    val titlePx = with(density) { 18.dp.toPx() }

    Box(modifier) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .then(if (playing) Modifier.background(FrameGradients.Fab) else Modifier.border(1.5.dp, FrameGradients.Fab, CircleShape))
                .semantics(mergeDescendants = true) {
                    onClick(label) { tap(); true }
                    onLongClick(FrameStrings.playbackFps(currentFps)) { setFps(nextPlayFps(currentFps)); true }
                }
                .pointerInput(longPressMs) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val early = withTimeoutOrNull(longPressMs) { listOf(waitForUpOrCancellation()) }
                        if (early != null) {
                            if (early[0] != null) { haptics.perform(FrameHaptic.Tap); tap() }
                            return@awaitEachGesture
                        }
                        haptics.perform(FrameHaptic.LongPress)
                        val start = speeds.indexOf(currentFps).coerceAtLeast(0)
                        hover = start
                        open = true
                        val zone = -(gapPx + padPx)
                        val id: PointerId = down.id
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            val change = event.changes.firstOrNull { it.id == id } ?: break
                            val y = change.position.y
                            val idx = if (y > zone) start else {
                                ((zone - y) / itemPx).toInt().coerceIn(0, speeds.size - 1)
                            }
                            if (idx != hover) { hover = idx; haptics.perform(FrameHaptic.Tick) }
                            change.consume()
                            if (change.changedToUpIgnoreConsumed() || !change.pressed) break
                        }
                        val chosen = speeds[hover.coerceIn(0, speeds.size - 1)]
                        open = false
                        if (chosen != currentFps) { haptics.perform(FrameHaptic.Confirm); setFps(chosen) } else haptics.perform(FrameHaptic.DragEnd)
                        hover = -1
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Image(painterResource(icon), label, Modifier.size(20.dp), colorFilter = ColorFilter.tint(Color.White))
        }
        FpsBadge(fps, playing, Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-5).dp))
        if (open) {
            val popH = ItemH * speeds.size + PopPadV * 2 + 18.dp
            val offsetY = with(density) { -(popH.toPx() + gapPx).toInt() }
            Popup(
                alignment = Alignment.TopCenter,
                offset = IntOffset(0, offsetY),
                properties = PopupProperties(focusable = false, clippingEnabled = false)
            ) { FpsPicker(speeds, hover) }
        }
    }
}

@Composable
private fun FpsBadge(fps: Int, playing: Boolean, modifier: Modifier) {
    Box(
        modifier
            .clip(CircleShape)
            .background(if (playing) Color.White else Color(0xFF1E1E22))
            .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
            .padding(horizontal = 5.dp, vertical = 1.dp)
    ) {
        FrameText(
            "$fps",
            style = FrameTheme.typography.mono.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
            color = if (playing) Color(0xFF111111) else Color.White
        )
    }
}

@Composable
private fun FpsPicker(speeds: List<Int>, selected: Int) {
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = 520f)) }
    ViewerGlassSurface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
        modifier = Modifier
            .width(PopW)
            .graphicsLayer {
                alpha = appear.value.coerceIn(0f, 1f)
                scaleX = 0.82f + 0.18f * appear.value
                scaleY = 0.82f + 0.18f * appear.value
                transformOrigin = TransformOrigin(0.5f, 1f)
            }
    ) {
        Column(Modifier.fillMaxWidth().padding(vertical = PopPadV), horizontalAlignment = Alignment.CenterHorizontally) {
            FrameText(
                "FPS",
                style = FrameTheme.typography.label.copy(fontSize = 9.sp, letterSpacing = 1.2.sp),
                color = ViewerInk.text.copy(alpha = 0.55f),
                modifier = Modifier.height(18.dp)
            )
            for (i in speeds.indices.reversed()) FpsItem(speeds[i], i == selected)
        }
    }
}

@Composable
private fun FpsItem(value: Int, on: Boolean) {
    Box(
        Modifier
            .padding(horizontal = 5.dp)
            .fillMaxWidth()
            .height(ItemH)
            .graphicsLayer { val s = if (on) 1.06f else 1f; scaleX = s; scaleY = s }
            .clip(CircleShape)
            .then(if (on) Modifier.background(FrameGradients.Fab) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        FrameText(
            "$value",
            style = FrameTheme.typography.mono.copy(fontSize = if (on) 16.sp else 14.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Medium),
            color = if (on) Color.White else ViewerInk.text.copy(alpha = 0.72f)
        )
    }
}
