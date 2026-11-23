package com.frameout.app

import com.frameout.app.trim.SmartShot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartShotTest {
    private val frames = LongArray(100) { it * 33_333L }

    @Test
    fun windowHoldsAtMostEightFramesBeforeTheTap() {
        val tap = 2_000L
        val picked = SmartShot.candidates(frames, tap)
        assertTrue(picked.size <= SmartShot.MAX_FRAMES)
        val tapIndex = picked.first() + 1
        assertTrue(picked.all { it < tapIndex })
        assertTrue(picked.all { frames[it] >= (tap - SmartShot.REACTION_MS) * 1000L })
    }

    @Test
    fun noCandidatesAtTheStart() {
        assertTrue(SmartShot.candidates(frames, 0L).isEmpty())
        assertTrue(SmartShot.candidates(LongArray(0), 500L).isEmpty())
    }

    @Test
    fun keepsTapFrameUnlessClearlySharper() {
        val tap = SmartShot.Sample(10, 100.0, 120.0)
        val close = SmartShot.Sample(9, 105.0, 120.0)
        assertSame(tap, SmartShot.choose(tap, listOf(close)))
        val sharp = SmartShot.Sample(8, 130.0, 120.0)
        assertSame(sharp, SmartShot.choose(tap, listOf(close, sharp)))
    }

    @Test
    fun ignoresFramesFromAnotherShot() {
        val tap = SmartShot.Sample(10, 50.0, 40.0)
        val cut = SmartShot.Sample(9, 400.0, 200.0)
        assertSame(tap, SmartShot.choose(tap, listOf(cut)))
    }

    @Test
    fun prefersNewestWhenSharpnessTies() {
        val tap = SmartShot.Sample(10, 10.0, 100.0)
        val a = SmartShot.Sample(9, 50.0, 100.0)
        val b = SmartShot.Sample(8, 50.0, 100.0)
        assertEquals(9, SmartShot.choose(tap, listOf(a, b)).index)
    }
}
