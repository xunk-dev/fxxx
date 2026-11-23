package com.frameout.app.trim

internal class FingerVelocity {
    private val times = LongArray(SAMPLES)
    private val xs = FloatArray(SAMPLES)
    private var size = 0
    private var next = 0

    fun reset() {
        size = 0
        next = 0
    }

    fun add(timeMs: Long, x: Float) {
        times[next] = timeMs
        xs[next] = x
        next = (next + 1) % SAMPLES
        if (size < SAMPLES) size++
    }

    fun pxPerS(nowMs: Long): Float {
        if (size < 2) return 0f
        val newest = (next - 1 + SAMPLES) % SAMPLES
        if (nowMs - times[newest] > STALE_MS) return 0f
        val oldest = oldestWithinHorizon(newest)
        val dt = times[newest] - times[oldest]
        if (dt < MIN_DT_MS) return 0f
        return (xs[newest] - xs[oldest]) * 1000f / dt
    }

    private fun oldestWithinHorizon(newest: Int): Int {
        var oldest = newest
        for (k in 1 until size) {
            val i = (newest - k + SAMPLES) % SAMPLES
            if (times[newest] - times[i] > HORIZON_MS) break
            oldest = i
        }
        return oldest
    }

    private companion object {
        const val SAMPLES = 8
        const val HORIZON_MS = 100L
        const val STALE_MS = 60L
        const val MIN_DT_MS = 8L
    }
}
