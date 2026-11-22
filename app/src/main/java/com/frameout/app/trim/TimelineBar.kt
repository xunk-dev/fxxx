package com.frameout.app.trim

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
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
import kotlin.math.abs
import kotlin.math.roundToLong
import com.frameout.engine.timeline.TrimRange
import com.frameout.core.util.safeCoerceIn

internal const val MIN_GAP_MS = 100L
internal const val MAX_STRIP_PX = 60_000
private const val EDGE_TICK_MS = 16L
private const val EDGE_SPEED = 0.02f
internal val HandleWidth = 18.dp
private val HitRadius = 28.dp
private val SnapRadius = 14.dp
private val EdgeZone = 28.dp
private val BarHeight = 60.dp
private const val NO_DRAG = -1L

internal enum class Handle { None, Start, End }

private enum class Gesture { Pending, Drag, Pinch }

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


private fun Modifier.timelineGestures(
    durationMs: Long,
    range: TrimRange,
    selecting: Boolean,
    slideWindow: Boolean,
    zoomOf: () -> TimelineZoom,
    onZoom: (TimelineZoom) -> Unit,
    positionOf: () -> Long,
    onSeek: (Long) -> Unit,
    onDragSeek: (Long) -> Unit,
    onRangeChange: (TrimRange) -> Unit,
    onScrub: (Boolean) -> Unit,
    onActive: (Handle) -> Unit,
    onGrab: () -> Unit,
    onTouch: (Boolean) -> Unit,
    onFreeze: (Long) -> Unit,
    snapTo: (Long) -> Long,
    frameCountOf: () -> Int,
    onPrecision: (Float) -> Unit
): Modifier = composed {
    val dur by rememberUpdatedState(durationMs)
    val current by rememberUpdatedState(range)
    val canSelect by rememberUpdatedState(selecting)
    val canSlide by rememberUpdatedState(slideWindow)
    val view by rememberUpdatedState(zoomOf)
    val setZoom by rememberUpdatedState(onZoom)
    val playhead by rememberUpdatedState(positionOf)
    val seek by rememberUpdatedState(onSeek)
    val dragSeek by rememberUpdatedState(onDragSeek)
    val change by rememberUpdatedState(onRangeChange)
    val scrub by rememberUpdatedState(onScrub)
    val activate by rememberUpdatedState(onActive)
    val grab by rememberUpdatedState(onGrab)
    val touch by rememberUpdatedState(onTouch)
    val freeze by rememberUpdatedState(onFreeze)
    val snap by rememberUpdatedState(snapTo)
    val frameCount by rememberUpdatedState(frameCountOf)
    val setPrecision by rememberUpdatedState(onPrecision)
    pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            touch(true)
            val pinned = playhead()
            val snapPx = SnapRadius.toPx()
            var snapped = false
            val handleWidth = HandleWidth.toPx()
            val edge = EdgeZone.toPx()
            val slop = viewConfiguration.touchSlop
            val width = size.width.toFloat()
            fun geometry() = TrackGeometry(width, handleWidth, dur, view())
            val trackDp = (width - handleWidth * 2f) / density
            fun maxScale() = TimelineZoom.maxScaleFor(dur, frameCount(), trackDp)
            val start = geometry()
            val narrow = canSlide && narrowWindow(start, current, handleWidth)
            val handle = if (canSelect && !narrow) pickHandle(down.position.x, start, current, HitRadius.toPx(), handleWidth) else Handle.None
            val slide = canSelect && canSlide && handle == Handle.None
            val playheadGrab = if (handle == Handle.None && !slide) {
                val off = down.position.x - start.xOf(playhead())
                if (kotlin.math.abs(off) <= HitRadius.toPx()) off else 0f
            } else 0f
            val slideSpan = current.endMs - current.startMs
            val slideAnchor = windowAnchor(start, down.position.x, current, handleWidth)
            if (handle != Handle.None) {
                activate(handle)
                grab()
            }
            fun slideTo(x: Float) {
                val first = snap(slideStartMs(geometry(), x, slideAnchor, slideSpan, dur)).safeCoerceIn(0L, (dur - slideSpan).coerceAtLeast(0L))
                change(current.copy(startMs = first, endMs = first + slideSpan, useFull = false))
                dragSeek(first)
            }
            var factor = ScrubPrecision.NORMAL
            var raw = when (handle) {
                Handle.Start -> current.startMs.toDouble()
                Handle.End -> current.endMs.toDouble()
                Handle.None -> start.msOf(down.position.x - playheadGrab).toDouble()
            }
            var prevX = down.position.x
            val centreY = size.height / 2f
            fun moveTo(x: Float, y: Float) {
                val g = geometry()
                val next = ScrubPrecision.forBar(abs(y - centreY) / density)
                if (next != factor) {
                    factor = next
                    setPrecision(next)
                }
                raw = (raw + (x - prevX) * g.viewSpan / g.trackWidth * factor).safeCoerceIn(0.0, dur.toDouble())
                prevX = x
                if (handle == Handle.Start) raw = raw.coerceAtMost((current.endMs - MIN_GAP_MS).toDouble())
                if (handle == Handle.End) raw = raw.coerceAtLeast((current.startMs + MIN_GAP_MS).toDouble())
                var ms = snap(raw.roundToLong())
                if (handle != Handle.None && factor == ScrubPrecision.NORMAL) {
                    val near = abs(g.xOf(ms) - g.xOf(pinned)) <= snapPx
                    if (near) ms = pinned
                    if (near != snapped) {
                        snapped = near
                        if (near) grab()
                    }
                }
                applyDrag(handle, ms, current, dragSeek, change)
            }
            var lastX = down.position.x
            var lastY = down.position.y
            val follow: (Float, Float) -> Unit = { x, y -> if (slide) slideTo(x) else moveTo(x, y) }
            fun edgeScroll(x: Float) {
                if (factor != ScrubPrecision.NORMAL) return
                val g = geometry()
                val intoStart = g.inset + edge - x
                val intoEnd = x - (g.trackEnd - edge)
                val push = when {
                    intoStart > 0f -> -(intoStart / edge)
                    intoEnd > 0f -> intoEnd / edge
                    else -> 0f
                }.coerceIn(-1.5f, 1.5f)
                if (push == 0f) return
                val before = view()
                val after = before.scrolledBy(push * EDGE_SPEED)
                if (after != before) {
                    setZoom(after)
                    raw = (raw + (after.startMs(dur) - before.startMs(dur))).safeCoerceIn(0.0, dur.toDouble())
                    follow(x, lastY)
                }
            }

            var mode = Gesture.Pending
            var scrubOn = false
            try {
                loop@ while (true) {
                    val watching = mode == Gesture.Drag && view().zoomed
                    val event = if (watching) withTimeoutOrNull(EDGE_TICK_MS) { awaitPointerEvent() } else awaitPointerEvent()
                    if (event == null) {
                        edgeScroll(lastX)
                        continue@loop
                    }
                    val fingers = event.changes.count { it.pressed }
                    if (fingers == 0) break@loop
                    if (mode != Gesture.Pinch && fingers >= 2) {
                        if (scrubOn) {
                            scrubOn = false
                            scrub(false)
                        }
                        activate(Handle.None)
                        mode = Gesture.Pinch
                    }
                    when (mode) {
                        Gesture.Pinch -> if (fingers >= 2) {
                            val g = geometry()
                            val zoomChange = event.calculateZoom()
                            val pan = event.calculatePan()
                            if (zoomChange != 1f || pan.x != 0f) {
                                val at = g.fractionOf(event.calculateCentroid(useCurrent = false).x)
                                setZoom(view().transform(at, pan.x / g.trackWidth, zoomChange, dur, maxScale()))
                            }
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                        Gesture.Pending -> {
                            val pointer = event.changes.firstOrNull { it.id == down.id } ?: break@loop
                            lastX = pointer.position.x
                            lastY = pointer.position.y
                            if ((pointer.position - down.position).getDistance() > slop) {
                                mode = Gesture.Drag
                                scrubOn = true
                                scrub(true)
                                if (slide) grab()
                                if (handle != Handle.None) freeze(pinned)
                                follow(lastX, lastY)
                                pointer.consume()
                            }
                        }
                        Gesture.Drag -> {
                            val pointer = event.changes.firstOrNull { it.id == down.id } ?: break@loop
                            lastX = pointer.position.x
                            lastY = pointer.position.y
                            follow(lastX, lastY)
                            pointer.consume()
                        }
                    }
                }
                if (mode == Gesture.Pending) when (handle) {
                    Handle.Start -> seek(current.startMs)
                    Handle.End -> seek(current.endMs)
                    Handle.None -> seek(snap(geometry().msOf(down.position.x)))
                }
            } finally {
                setPrecision(ScrubPrecision.NORMAL)
                if (scrubOn) scrub(false)
                if (handle != Handle.None && mode == Gesture.Drag) {
                    freeze(NO_DRAG)
                    seek(pinned)
                }
                activate(Handle.None)
                touch(false)
            }
        }
    }
}
