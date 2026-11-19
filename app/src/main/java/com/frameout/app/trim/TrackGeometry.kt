package com.frameout.app.trim

import kotlin.math.roundToLong

internal class TrackGeometry(width: Float, val inset: Float, private val durationMs: Long, zoom: TimelineZoom = TimelineZoom()) {
    val trackWidth: Float = (width - inset * 2).coerceAtLeast(1f)
    val trackEnd: Float = inset + trackWidth
    private val viewStart: Float = zoom.startMs(durationMs)
    val viewSpan: Float = zoom.spanMs(durationMs).coerceAtLeast(1f)

    fun xOf(ms: Long): Float = inset + (ms.coerceIn(0L, durationMs) - viewStart) / viewSpan * trackWidth
    fun msOf(x: Float): Long = (viewStart + fractionOf(x) * viewSpan).roundToLong().coerceIn(0L, durationMs)
    fun fractionOf(x: Float): Float = ((x - inset) / trackWidth).coerceIn(0f, 1f)
}
