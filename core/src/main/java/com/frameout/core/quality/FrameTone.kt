package com.frameout.core.quality

import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
object FrameTone {
    const val MIN = 0.05f

    private const val CRUSHED = 12
    private const val BLOWN = 245
    private const val SAMPLES = 4096
    private const val CLIP_FREE = 0.5
    private const val CLIP_FULL = 0.95
    private const val FLAT_STD = 45.0
    private const val FLAT_FLOOR = 0.35

    fun quality(luma: IntArray, width: Int, height: Int): Float {
        if (width < 1 || height < 1 || luma.size < width.toLong() * height) return 1f
        val step = max(1, sqrt(width.toDouble() * height / SAMPLES).toInt())
        var count = 0
        var crushed = 0
        var blown = 0
        var sum = 0.0
        var sumSq = 0.0
        var y = 0
        while (y < height) {
            var x = 0
            while (x < width) {
                val v = luma[y * width + x]
                count++
                if (v <= CRUSHED) crushed++
                if (v >= BLOWN) blown++
                sum += v
                sumSq += v.toDouble() * v
                x += step
            }
            y += step
        }
        if (count == 0) return 1f
        val mean = sum / count
        val std = sqrt(max(0.0, sumSq / count - mean * mean))
        return score(crushed.toDouble() / count, blown.toDouble() / count, std)
    }

    internal fun score(crushedShare: Double, blownShare: Double, std: Double): Float {
        val clipped = max(crushedShare, blownShare)
        val t = ((clipped - CLIP_FREE) / (CLIP_FULL - CLIP_FREE)).coerceIn(0.0, 1.0)
        val exposure = 1.0 - t * t * (3.0 - 2.0 * t)
        val contrast = FLAT_FLOOR + (1.0 - FLAT_FLOOR) * min(1.0, std / FLAT_STD)
        return (exposure * contrast).coerceIn(MIN.toDouble(), 1.0).toFloat()
    }
}
