package com.frameout.engine.decode

internal object CalibrationRule {
    const val ACCEPT = 4.5f

    const val MIN_GAIN = 0.75f

    const val MIN_DELTA = 3f

    const val MAX_ERROR = 60f

    const val MIN_LUMA_STD = 14f

    const val MIN_LUMA_SPREAD = 60f

    fun informative(signature: FloatArray): Boolean {
        val cells = signature.size / 3
        if (cells < 4) return false
        var sum = 0.0
        var lo = Float.MAX_VALUE
        var hi = -Float.MAX_VALUE
        val luma = FloatArray(cells) { i ->
            0.299f * signature[i * 3] + 0.587f * signature[i * 3 + 1] + 0.114f * signature[i * 3 + 2]
        }
        for (v in luma) {
            sum += v
            if (v < lo) lo = v
            if (v > hi) hi = v
        }
        val mean = sum / cells
        var sq = 0.0
        for (v in luma) sq += (v - mean) * (v - mean)
        val std = kotlin.math.sqrt(sq / cells).toFloat()
        return std >= MIN_LUMA_STD && hi - lo >= MIN_LUMA_SPREAD
    }

    fun pick(autoError: Float, candidateErrors: Map<Int, Float>): Int? {
        val best = candidateErrors.entries.minByOrNull { it.value }
        val lowest = if (best == null) autoError else minOf(autoError, best.value)
        if (lowest.isNaN() || lowest > MAX_ERROR) return null
        if (best == null) return ColorVariants.AUTO
        return if (best.value < autoError * MIN_GAIN && autoError - best.value > MIN_DELTA) best.key else ColorVariants.AUTO
    }

    fun meanAbsDifference(a: FloatArray, b: FloatArray): Float {
        if (a.isEmpty() || a.size != b.size) return Float.NaN
        var sum = 0.0
        for (i in a.indices) sum += kotlin.math.abs(a[i] - b[i])
        return (sum / a.size).toFloat()
    }
}
