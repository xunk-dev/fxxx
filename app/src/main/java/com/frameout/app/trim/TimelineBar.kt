package com.frameout.app.trim

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.frameout.designsystem.glass.frameGlow
import com.frameout.designsystem.haptics.FrameHaptic
import com.frameout.designsystem.haptics.rememberFrameHaptics
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameCardEdge
import com.frameout.designsystem.theme.frameControl
import com.frameout.designsystem.components.FrameText
import com.frameout.engine.timeline.TrimRange

internal const val MIN_GAP_MS = 100L
internal const val MAX_STRIP_PX = 60_000
internal const val EDGE_TICK_MS = 16L
internal const val EDGE_SPEED = 0.02f
internal val HandleWidth = 18.dp
internal val HitRadius = 28.dp
internal val SnapRadius = 14.dp
internal val EdgeZone = 28.dp
private val BarHeight = 60.dp
internal const val NO_DRAG = -1L

internal enum class Handle { None, Start, End }

internal enum class Gesture { Pending, Drag, Pinch }

internal class TimelinePaint(val primary: Color, val onPrimary: Color, val marker: Color, val queued: Color)

@Composable
fun TimelineBar(
    durationMs: Long,
    positionMs: Long,
    range: TrimRange,
    selecting: Boolean,
    onSeek: (Long) -> Unit,
    onRangeChange: (TrimRange) -> Unit,
    thumbs: List<Bitmap?>,
    markers: List<Long> = emptyList(),
    modifier: Modifier = Modifier,
    queued: List<Long> = emptyList(),
    onScrub: (Boolean) -> Unit = {},
    slideWindow: Boolean = false,
    onDragSeek: (Long) -> Unit = onSeek,
    snap: (Long) -> Long = { it },
    frames: () -> LongArray? = { null }
) {
    val colors = FrameTheme.colors
    val paint = TimelinePaint(colors.primary, colors.onPrimary, colors.success, colors.error)
    val dur = durationMs.coerceAtLeast(1)
    var active by remember { mutableStateOf(Handle.None) }
    var zoom by remember(dur) { mutableStateOf(TimelineZoom()) }
    var touching by remember { mutableStateOf(false) }
    var dragMs by remember { mutableLongStateOf(NO_DRAG) }
    var frozenMs by remember { mutableLongStateOf(NO_DRAG) }
    var precision by remember { mutableFloatStateOf(ScrubPrecision.NORMAL) }
    val zoomScale by remember(dur) { derivedStateOf { zoom.scale } }
    val dragSeek: (Long) -> Unit = { ms -> dragMs = ms; onDragSeek(ms) }
    val haptics = rememberFrameHaptics()
    LaunchedEffect(positionMs, dur) {
        if (!touching) zoom = zoom.revealing(positionMs, dur)
    }
    LaunchedEffect(slideWindow, dur) {
        if (slideWindow && !zoom.zoomed) zoom = TimelineZoom().focusing(range.startMs, range.endMs, dur, FOCUS_FILL)
    }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(modifier.height(BarHeight).frameGlow(colors.primary, { if (colors.isDark) 0.16f else 0.08f }, 10.dp, { 0.2f }).clip(FrameShapes.sm).background(colors.containerHigh).border(1.dp, frameCardEdge(colors.primary, 0.35f), FrameShapes.sm).systemGestureExclusion()) {
            ThumbStrip(thumbs, { zoomScale }, { zoom.start }, Modifier.fillMaxSize().padding(horizontal = HandleWidth))
            Canvas(
                Modifier
                    .fillMaxSize()
                    .timelineSemantics(durationMs, positionMs, range, onSeek, onRangeChange)
                    .timelineGestures(
                        dur, range, selecting, slideWindow, { zoom }, { zoom = it },
                        { if (touching && dragMs != NO_DRAG) dragMs else positionMs },
                        onSeek, dragSeek, onRangeChange, onScrub, { active = it }, { haptics.perform(FrameHaptic.Tap) },
                        { touching = it; if (!it) dragMs = NO_DRAG },
                        { frozenMs = it },
                        snap, { frames()?.size ?: 0 }, { precision = it }
                    )
            ) {
                val shown = if (frozenMs != NO_DRAG) frozenMs else if (touching && dragMs != NO_DRAG) dragMs else positionMs
                drawTimeline(dur, shown, range, selecting, active, markers, queued, paint, zoom, frames())
            }
            if (precision < ScrubPrecision.NORMAL) {
                FrameText(
                    ScrubPrecision.barLabel(precision),
                    style = FrameTheme.typography.label,
                    color = colors.onSurface,
                    maxLines = 1,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 6.dp).frameControl(FrameShapes.pill, selected = true, glowAlpha = 0.4f).padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }
            if (zoom.zoomed) {
                TimelineZoomBadge(zoom.scale, { zoom = TimelineZoom() }, Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = HandleWidth + 4.dp))
            }
        }
    }
}
