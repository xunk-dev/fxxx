package com.frameout.app.trim

internal object ScrubPrecision {
    const val NORMAL = 1f
    private val BAR_STEPS = listOf(56f to NORMAL, 130f to 0.5f, 210f to 0.2f)
    const val BAR_FINEST = 0.05f

    private val SURFACE_STEPS = listOf(70f to 10f, 150f to 24f)
    const val SURFACE_FINEST_DP = 64f

    fun forBar(distanceDp: Float): Float {
        val d = kotlin.math.abs(distanceDp)
        return BAR_STEPS.firstOrNull { d <= it.first }?.second ?: BAR_FINEST
    }

    fun dpPerFrame(distanceDp: Float): Float {
        val d = kotlin.math.abs(distanceDp)
        return SURFACE_STEPS.firstOrNull { d <= it.first }?.second ?: SURFACE_FINEST_DP
    }

    fun barLabel(factor: Float): String = when {
        factor >= NORMAL -> ""
        factor >= 0.5f -> "\u00BD\u00D7"
        factor >= 0.2f -> "\u2155\u00D7"
        else -> "1/20\u00D7"
    }

    fun surfaceLabel(dpPerFrame: Float): String = when {
        dpPerFrame <= SURFACE_STEPS.first().second -> ""
        dpPerFrame <= SURFACE_STEPS.last().second -> "\u00BD\u00D7"
        else -> "\u00BC\u00D7"
    }

    fun framesFor(travelPx: Float, pxPerFrame: Float): Int =
        if (pxPerFrame <= 0f) 0 else kotlin.math.round(travelPx / pxPerFrame).toInt()
}
