package com.frameout.app.trim

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import com.frameout.core.util.safeCoerceIn
import com.frameout.engine.timeline.TrimRange
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs
import kotlin.math.roundToLong

internal fun Modifier.timelineGestures(
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
    onGlideStop: () -> Unit,
    onFling: (Long, Double) -> Boolean
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
    val glideStop by rememberUpdatedState(onGlideStop)
    val fling by rememberUpdatedState(onFling)
    pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            glideStop()
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
            var factor = 1.0
            val velocity = FingerVelocity()
            val frameMs = ScrubProfile.frameMsOf(dur, frameCount())
            var raw = when (handle) {
                Handle.Start -> current.startMs.toDouble()
                Handle.End -> current.endMs.toDouble()
                Handle.None -> start.msOf(down.position.x - playheadGrab).toDouble()
            }
            var prevX = down.position.x
            var lastMs = pinned
            fun moveTo(x: Float, t: Long) {
                val g = geometry()
                if (handle == Handle.None) {
                    velocity.add(t, x)
                    val directMsPerDp = g.viewSpan.toDouble() / g.trackWidth * density
                    factor = ScrubProfile.timelineFactor(abs(velocity.pxPerS(t)) / density.toDouble(), frameMs, directMsPerDp)
                }
                raw = (raw + (x - prevX) * g.viewSpan / g.trackWidth * factor).safeCoerceIn(0.0, dur.toDouble())
                prevX = x
                if (handle == Handle.Start) raw = raw.coerceAtMost((current.endMs - MIN_GAP_MS).toDouble())
                if (handle == Handle.End) raw = raw.coerceAtLeast((current.startMs + MIN_GAP_MS).toDouble())
                var ms = snap(raw.roundToLong())
                if (handle != Handle.None) {
                    val near = abs(g.xOf(ms) - g.xOf(pinned)) <= snapPx
                    if (near) ms = pinned
                    if (near != snapped) {
                        snapped = near
                        if (near) grab()
                    }
                }
                lastMs = ms
                applyDrag(handle, ms, current, dragSeek, change)
            }
            var lastX = down.position.x
            var lastT = down.uptimeMillis
            var releaseT = lastT
            val follow: (Float, Long) -> Unit = { x, t -> if (slide) slideTo(x) else moveTo(x, t) }
            fun edgeScroll(x: Float) {
                if (handle == Handle.None && factor < 1.0) return
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
                    follow(x, lastT)
                }
            }

            var mode = Gesture.Pending
            var scrubOn = false
            var flung = false
            try {
                loop@ while (true) {
                    val watching = mode == Gesture.Drag && view().zoomed
                    val event = if (watching) withTimeoutOrNull(EDGE_TICK_MS) { awaitPointerEvent() } else awaitPointerEvent()
                    if (event == null) {
                        edgeScroll(lastX)
                        continue@loop
                    }
                    val fingers = event.changes.count { it.pressed }
                    if (fingers == 0) {
                        releaseT = event.changes.maxOf { it.uptimeMillis }
                        break@loop
                    }
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
                            lastT = pointer.uptimeMillis
                            if ((pointer.position - down.position).getDistance() > slop) {
                                mode = Gesture.Drag
                                scrubOn = true
                                scrub(true)
                                if (slide) grab()
                                if (handle != Handle.None) freeze(pinned)
                                follow(lastX, lastT)
                                pointer.consume()
                            }
                        }
                        Gesture.Drag -> {
                            val pointer = event.changes.firstOrNull { it.id == down.id } ?: break@loop
                            lastX = pointer.position.x
                            lastT = pointer.uptimeMillis
                            follow(lastX, lastT)
                            pointer.consume()
                        }
                    }
                }
                if (mode == Gesture.Pending) when (handle) {
                    Handle.Start -> seek(current.startMs)
                    Handle.End -> seek(current.endMs)
                    Handle.None -> seek(snap(geometry().msOf(down.position.x)))
                }
                if (mode == Gesture.Drag && handle == Handle.None && !slide) {
                    val msPerPx = geometry().let { it.viewSpan.toDouble() / it.trackWidth }
                    val dpPerS = velocity.pxPerS(releaseT) / density.toDouble()
                    if (ScrubProfile.flings(abs(dpPerS))) flung = fling(lastMs, dpPerS * density * msPerPx * factor)
                }
            } finally {
                if (scrubOn && !flung) scrub(false)
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
