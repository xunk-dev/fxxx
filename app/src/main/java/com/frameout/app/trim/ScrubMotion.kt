package com.frameout.app.trim

import kotlin.math.abs
import kotlin.math.roundToInt

internal class ScrubMotion(private val totalFrames: Int, private val startFrame: Int) {
    private val velocity = FingerVelocity()
    private var travel = 0.0
    private var gain = 1.0 / ScrubProfile.VIDEO_SLOW_DP_PER_FRAME

    val indexed: Boolean = totalFrames > 0 && startFrame >= 0

    val offset: Int get() = travel.roundToInt()

    val frame: Int get() = if (indexed) (startFrame + offset).coerceIn(0, totalFrames - 1) else offset

    fun move(timeMs: Long, xPx: Float, dxPx: Float, density: Float): Int {
        velocity.add(timeMs, xPx)
        val speed = abs(velocity.pxPerS(timeMs)).toDouble() / density
        val target = ScrubProfile.videoFramesPerDp(speed, totalFrames)
        gain = if (target <= gain) target else gain + (target - gain) * GAIN_RISE
        travel += dxPx.toDouble() / density * gain
        if (indexed) travel = travel.coerceIn(-startFrame.toDouble(), (totalFrames - 1 - startFrame).toDouble())
        return offset
    }

    fun release(timeMs: Long, density: Float): ScrubGlide? {
        if (!indexed) return null
        val dpPerS = velocity.pxPerS(timeMs).toDouble() / density
        if (!ScrubProfile.flings(abs(dpPerS))) return null
        val framesPerS = dpPerS * ScrubProfile.videoFramesPerDp(abs(dpPerS), totalFrames)
        return ScrubGlide.plan(
            startFrame + travel,
            framesPerS,
            ScrubProfile.FRICTION_TAU_S,
            0.0,
            (totalFrames - 1).toDouble(),
            SETTLE_FRAMES
        ) { it.roundToInt().toDouble() }
    }

    fun offsetOf(frame: Int): Int = frame - startFrame

    fun atEdge(frame: Int): Boolean = indexed && (frame <= 0 || frame >= totalFrames - 1)

    private companion object {
        const val GAIN_RISE = 0.4
        const val SETTLE_FRAMES = 0.5
    }
}
