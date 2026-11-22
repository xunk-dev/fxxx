package com.frameout.app.viewer

import androidx.compose.ui.geometry.Offset
import kotlin.math.max
import kotlin.math.min

/**
 * Size-aware handle metrics, shared by crop / shape / cover / text editors.
 *
 * Image editors (Photoshop, Figma, Snapseed, Lightroom) all follow the same rules:
 *  1. handles shrink as the target shrinks so they never swallow the content,
 *  2. the grab zone of a handle is reduced on the *inside* of a small box so the body
 *     stays draggable, but stays full-size on the *outside* so a tiny box can be enlarged,
 *  3. the middle of a small box always means "move".
 */
internal object HandleFit {
    private const val RADIUS_FRACTION = 0.16f
    private const val MIN_RADIUS_DP = 4f
    private const val MIN_REACH_DP = 14f
    private const val REACH_FRACTION = 0.5f
    private const val CORE_FRACTION = 0.55f

    /** Visual radius of a corner/end handle for a target whose shortest side is [minSidePx]. */
    fun radius(minSidePx: Float, maxPx: Float, density: Float): Float {
        val floor = MIN_RADIUS_DP * density
        if (!minSidePx.isFinite() || maxPx <= floor) return maxPx
        return (minSidePx * RADIUS_FRACTION).coerceIn(floor, maxPx)
    }

    /** Touch radius of a handle; never smaller than a usable finger target. */
    fun reach(minSidePx: Float, maxPx: Float, density: Float): Float {
        val floor = min(MIN_REACH_DP * density, maxPx)
        if (!minSidePx.isFinite()) return maxPx
        return (minSidePx * REACH_FRACTION).coerceIn(floor, max(floor, maxPx))
    }

    /** True when the box is large enough that handles can never overlap its centre. */
    fun roomy(minSidePx: Float, maxReachPx: Float): Boolean = minSidePx.isFinite() && minSidePx >= maxReachPx * 2f

    /**
     * Handle hit with a protected centre. On small targets a touch near the middle belongs to
     * the body (move), not to whichever handle happens to be nearest.
     */
    fun grabs(at: Offset, handle: Offset, centre: Offset, reach: Float, roomy: Boolean): Boolean {
        if ((at - handle).getDistance() > reach) return false
        if (roomy) return true
        val handleSpan = (handle - centre).getDistance()
        return (at - centre).getDistance() >= handleSpan * CORE_FRACTION
    }
}
