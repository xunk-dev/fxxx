package com.frameout.app.trim

import kotlin.math.min

internal object ScrubProfile {
    const val SLOW_DP_PER_S = 40.0
    const val FAST_DP_PER_S = 900.0
    const val VIDEO_SLOW_DP_PER_FRAME = 12.0
    const val TIMELINE_SLOW_DP_PER_FRAME = 8.0
    const val VIDEO_SWIPE_DP = 360.0
    const val VIDEO_MAX_SWIPE_FRAMES = 600
    const val FRICTION_TAU_S = 0.42
    const val MIN_FLING_DP_PER_S = 160.0
    const val FALLBACK_FRAME_MS = 33.3

    fun blend(speedDpPerS: Double): Double {
        val t = ((speedDpPerS - SLOW_DP_PER_S) / (FAST_DP_PER_S - SLOW_DP_PER_S)).coerceIn(0.0, 1.0)
        return t * t * (3.0 - 2.0 * t)
    }

    fun videoFramesPerDp(speedDpPerS: Double, totalFrames: Int): Double {
        val slow = 1.0 / VIDEO_SLOW_DP_PER_FRAME
        val fast = maxOf(slow, min(totalFrames, VIDEO_MAX_SWIPE_FRAMES) / VIDEO_SWIPE_DP)
        return slow + (fast - slow) * blend(speedDpPerS)
    }

    fun timelineFactor(speedDpPerS: Double, frameMs: Double, directMsPerDp: Double): Double {
        if (directMsPerDp <= 0.0 || frameMs <= 0.0) return 1.0
        val slow = min(1.0, frameMs / TIMELINE_SLOW_DP_PER_FRAME / directMsPerDp)
        return slow + (1.0 - slow) * blend(speedDpPerS)
    }

    fun frameMsOf(durationMs: Long, frames: Int): Double =
        if (frames > 1 && durationMs > 0L) durationMs.toDouble() / frames else FALLBACK_FRAME_MS

    fun flings(speedDpPerS: Double): Boolean = speedDpPerS >= MIN_FLING_DP_PER_S
}
