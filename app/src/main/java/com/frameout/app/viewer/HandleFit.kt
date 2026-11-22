package com.frameout.app.viewer

import androidx.compose.ui.geometry.Offset
import kotlin.math.max
import kotlin.math.min

internal object HandleFit {
    private const val RADIUS_FRACTION = 0.16f
    private const val MIN_RADIUS_DP = 4f
    private const val MIN_REACH_DP = 14f
    private const val REACH_FRACTION = 0.5f
    private const val CORE_FRACTION = 0.55f

    fun radius(minSidePx: Float, maxPx: Float, density: Float): Float {
        val floor = MIN_RADIUS_DP * density
        if (!minSidePx.isFinite() || maxPx <= floor) return maxPx
        return (minSidePx * RADIUS_FRACTION).coerceIn(floor, maxPx)
    }

    fun reach(minSidePx: Float, maxPx: Float, density: Float): Float {
        val floor = min(MIN_REACH_DP * density, maxPx)
        if (!minSidePx.isFinite()) return maxPx
        return (minSidePx * REACH_FRACTION).coerceIn(floor, max(floor, maxPx))
    }

    fun roomy(minSidePx: Float, maxReachPx: Float): Boolean = minSidePx.isFinite() && minSidePx >= maxReachPx * 2f

    fun grabs(at: Offset, handle: Offset, centre: Offset, reach: Float, roomy: Boolean): Boolean {
        if ((at - handle).getDistance() > reach) return false
        if (roomy) return true
        val handleSpan = (handle - centre).getDistance()
        return (at - centre).getDistance() >= handleSpan * CORE_FRACTION
    }
}
