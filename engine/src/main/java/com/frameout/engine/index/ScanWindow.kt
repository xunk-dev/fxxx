package com.frameout.engine.index

data class ScanWindow(val startUs: Long, val endUs: Long) {
    fun isPastEnd(sampleTimeUs: Long): Boolean = sampleTimeUs > endUs + TAIL_US

    fun covers(other: ScanWindow): Boolean = other.startUs >= startUs && other.endUs <= endUs

    fun fractionOf(durationUs: Long): Double =
        if (durationUs <= 0L) 1.0 else ((endUs - startUs).toDouble() / durationUs).coerceIn(0.0, 1.0)

    fun scaleEstimate(frames: Int, durationUs: Long): Int =
        if (frames <= 0) 0 else (frames * fractionOf(durationUs)).toInt().coerceAtLeast(1)

    fun encode(): String = "$startUs:$endUs"

    companion object {
        const val TAIL_US = 2_000_000L

        const val MAX_FRACTION = 0.9

        fun of(startMs: Long, endMs: Long, trackDurationUs: Long): ScanWindow? {
            if (trackDurationUs <= 0L) return null
            val startUs = startMs.coerceAtLeast(0L) * 1000L
            val endUs = if (endMs > 0L) minOf(endMs * 1000L, trackDurationUs) else trackDurationUs
            if (startUs == 0L && endUs >= trackDurationUs) return null
            if (endUs <= startUs) return null
            val window = ScanWindow(startUs, endUs)
            return window.takeIf { it.fractionOf(trackDurationUs) < MAX_FRACTION }
        }

        fun decode(text: String?): ScanWindow? {
            val parts = text?.trim()?.split(':') ?: return null
            if (parts.size != 2) return null
            val start = parts[0].toLongOrNull() ?: return null
            val end = parts[1].toLongOrNull() ?: return null
            return if (end > start && start >= 0L) ScanWindow(start, end) else null
        }
    }
}
