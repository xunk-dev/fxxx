package com.frameout.app

import com.frameout.app.trim.FingerVelocity
import com.frameout.app.trim.ScrubGlide
import com.frameout.app.trim.ScrubMotion
import com.frameout.app.trim.ScrubProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScrubMotionTest {
    private val density = 2f

    private fun drag(motion: ScrubMotion, dpPerS: Double, seconds: Double, startX: Float = 0f): Float {
        val steps = (seconds * 120).toInt()
        val dxPx = (dpPerS * density / 120.0).toFloat()
        var x = startX
        var t = 1_000L
        repeat(steps) {
            t = 1_000L + ((it + 1) * 1000L / 120)
            x += dxPx
            motion.move(t, x, dxPx, density)
        }
        return x
    }

    @Test
    fun slowDragReachesEveryFrame() {
        val motion = ScrubMotion(1000, 500)
        drag(motion, 20.0, 1.0)
        assertTrue(motion.offset in 1..3)
    }

    @Test
    fun fastDragCoversManyFrames() {
        val motion = ScrubMotion(1000, 0)
        drag(motion, 900.0, 0.4)
        assertTrue(motion.offset > 100)
    }

    @Test
    fun offsetStaysInsideClip() {
        val motion = ScrubMotion(100, 90)
        drag(motion, 900.0, 2.0)
        assertEquals(99, motion.frame)
        val back = ScrubMotion(100, 10)
        drag(back, -900.0, 2.0)
        assertEquals(0, back.frame)
    }

    @Test
    fun slowReleaseDoesNotGlide() {
        val motion = ScrubMotion(1000, 500)
        drag(motion, 30.0, 0.5)
        assertNull(motion.release(1_500L, density))
    }

    @Test
    fun fastReleaseGlidesForwardAndEndsOnAFrame() {
        val motion = ScrubMotion(2000, 100)
        drag(motion, 800.0, 0.3)
        val glide = motion.release(1_000L + 36L, density)
        assertNotNull(glide)
        val end = glide!!.at(glide.durationS + 1.0)
        assertEquals(Math.rint(end), end, 0.0)
        assertTrue(end > 100.0 + motion.offset)
    }

    @Test
    fun unindexedClipNeverGlides() {
        val motion = ScrubMotion(0, -1)
        drag(motion, 800.0, 0.3)
        assertNull(motion.release(1_036L, density))
    }

    @Test
    fun gainGrowsWithSpeed() {
        val slow = ScrubProfile.videoFramesPerDp(10.0, 600)
        val fast = ScrubProfile.videoFramesPerDp(900.0, 600)
        assertEquals(1.0 / ScrubProfile.VIDEO_SLOW_DP_PER_FRAME, slow, 1e-9)
        assertTrue(fast > slow * 10)
    }

    @Test
    fun timelineSlowSpeedNeverSkipsFrames() {
        val factor = ScrubProfile.timelineFactor(10.0, 33.3, 5000.0)
        assertTrue(factor < 0.01)
        assertEquals(1.0, ScrubProfile.timelineFactor(900.0, 33.3, 5000.0), 1e-9)
    }

    @Test
    fun velocityIgnoresStaleSamples() {
        val v = FingerVelocity()
        v.add(0L, 0f)
        v.add(20L, 40f)
        assertEquals(2000f, v.pxPerS(20L), 1f)
        assertEquals(0f, v.pxPerS(500L), 0f)
    }

    @Test
    fun glideRefusesTinyOrBackwardsMoves() {
        assertNull(ScrubGlide.plan(10.0, 0.0, 0.4, 0.0, 100.0, 0.5) { it })
        assertNull(ScrubGlide.plan(100.0, 500.0, 0.4, 0.0, 100.0, 0.5) { it })
    }

    @Test
    fun slowingDownTakesEffectAtOnce() {
        val motion = ScrubMotion(2000, 1000)
        var x = drag(motion, 900.0, 0.3)
        val before = motion.offset
        val dxPx = 2f
        motion.move(2_000L, x + dxPx, dxPx, density)
        motion.move(2_030L, x + 2 * dxPx, dxPx, density)
        assertTrue(motion.offset - before <= 2)
    }

    @Test
    fun edgeIsDetected() {
        val motion = ScrubMotion(100, 50)
        assertTrue(motion.atEdge(0))
        assertTrue(motion.atEdge(99))
        assertTrue(!motion.atEdge(50))
    }
}
