package com.frameout.app.trim

import com.frameout.core.util.safeCoerceIn

data class TimelineZoom(val scale: Float = 1f, val start: Float = 0f) {
    val zoomed: Boolean get() = scale > MIN_SCALE
    val span: Float get() = 1f / scale
    val end: Float get() = start + span

    fun startMs(durationMs: Long): Float = start * durationMs
    fun spanMs(durationMs: Long): Float = span * durationMs

    fun transform(anchor: Float, pan: Float, zoom: Float, durationMs: Long, maxScale: Float = maxScaleFor(durationMs)): TimelineZoom {
        val next = (scale * zoom).safeCoerceIn(MIN_SCALE, maxScale.coerceAtLeast(MIN_SCALE))
        if (next <= MIN_SCALE) return TimelineZoom()
        val a = anchor.coerceIn(0f, 1f)
        val under = start + a * span
        val nextSpan = 1f / next
        return TimelineZoom(next, clampStart(under - (a + pan) * nextSpan, nextSpan))
    }

    fun scrolledBy(fraction: Float): TimelineZoom {
        if (!zoomed) return this
        return copy(start = clampStart(start + fraction * span, span))
    }

    fun revealing(ms: Long, durationMs: Long): TimelineZoom {
        if (!zoomed || durationMs <= 0L) return this
        val at = ms.safeCoerceIn(0L, durationMs).toFloat() / durationMs
        if (at >= start && at <= end) return this
        return copy(start = clampStart(at - span / 2f, span))
    }

    companion object {
        const val MIN_SCALE = 1f
        const val MAX_SCALE = 60f

        const val MIN_VISIBLE_MS = 1_000L

        const val MIN_FRAME_DP = 12f
        const val MAX_FRAME_SCALE = 50_000f

        fun maxScaleFor(durationMs: Long): Float =
            (durationMs.toFloat() / MIN_VISIBLE_MS).coerceIn(MIN_SCALE, MAX_SCALE)

        fun maxScaleFor(durationMs: Long, frames: Int, trackDp: Float): Float {
            val byTime = maxScaleFor(durationMs)
            if (frames <= 0 || trackDp <= 0f) return byTime
            val visibleFrames = (trackDp / MIN_FRAME_DP).coerceAtLeast(2f)
            return maxOf(byTime, (frames / visibleFrames).coerceIn(MIN_SCALE, MAX_FRAME_SCALE))
        }

        private fun clampStart(start: Float, span: Float): Float =
            start.safeCoerceIn(0f, (1f - span).coerceAtLeast(0f))
    }
}
