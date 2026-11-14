package com.frameout.app.trim

import com.frameout.engine.timeline.TrimRange

internal const val FOCUS_FILL = 0.4f
private const val NARROW_FACTOR = 2.5f
private const val GRAB_HALF_FACTOR = 1.75f

internal fun narrowWindow(geometry: TrackGeometry, range: TrimRange, handleWidth: Float): Boolean =
    geometry.xOf(range.endMs) - geometry.xOf(range.startMs) < handleWidth * NARROW_FACTOR

internal fun windowAnchor(geometry: TrackGeometry, downX: Float, range: TrimRange, handleWidth: Float): Float {
    val startX = geometry.xOf(range.startMs)
    val endX = geometry.xOf(range.endMs)
    val width = endX - startX
    val half = maxOf(width / 2f, handleWidth * GRAB_HALF_FACTOR)
    val centre = (startX + endX) / 2f
    return if (downX in (centre - half)..(centre + half)) downX - startX else width / 2f
}

internal fun slideStartMs(geometry: TrackGeometry, x: Float, anchor: Float, spanMs: Long, durationMs: Long): Long =
    geometry.msOf(x - anchor).coerceIn(0L, (durationMs - spanMs).coerceAtLeast(0L))

internal fun TimelineZoom.focusing(startMs: Long, endMs: Long, durationMs: Long, fill: Float): TimelineZoom {
    if (durationMs <= 0L) return this
    val fraction = ((endMs - startMs).toFloat() / durationMs).coerceAtLeast(MIN_FRACTION)
    val scale = (fill / fraction).coerceIn(TimelineZoom.MIN_SCALE, TimelineZoom.maxScaleFor(durationMs))
    if (scale <= TimelineZoom.MIN_SCALE) return TimelineZoom()
    val span = 1f / scale
    val centre = (startMs + endMs) / 2f / durationMs
    return TimelineZoom(scale, (centre - span / 2f).coerceIn(0f, (1f - span).coerceAtLeast(0f)))
}

private const val MIN_FRACTION = 0.000001f
