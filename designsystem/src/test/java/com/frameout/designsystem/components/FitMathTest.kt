package com.frameout.designsystem.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FitMathTest {
    @Test
    fun floorIsTheLargerOfRelativeAndAbsolute() {
        assertEquals(0.6f, FitMath.floorScale(0.6f, 9f, 28f), 0f)
        assertEquals(0.75f, FitMath.floorScale(0.6f, 9f, 12f), 0f)
        assertEquals(1f, FitMath.floorScale(0.6f, 9f, 8f), 0f)
        assertEquals(0.7f, FitMath.floorScale(0.7f, 9f, Float.NaN), 0f)
    }

    @Test
    fun singleLineJumpsToTheWidthThatFits() {
        val next = FitMath.nextScale(1f, 0.6f, 100, 125f, true)
        assertTrue(next > 0.78f && next < 0.80f)
        assertEquals(0.6f, FitMath.nextScale(1f, 0.6f, 100, 200f, true), 0f)
    }

    @Test
    fun everyOverflowMakesProgressAndStopsAtTheFloor() {
        assertTrue(FitMath.nextScale(1f, 0.6f, 100, 101f, true) <= FitMath.STEP + 1e-4f)
        assertEquals(FitMath.STEP, FitMath.nextScale(1f, 0.6f, 100, 500f, false), 1e-4f)
        assertEquals(FitMath.STEP, FitMath.nextScale(1f, 0.6f, Int.MAX_VALUE, 500f, true), 1e-4f)
        assertEquals(0.6f, FitMath.nextScale(0.62f, 0.6f, 100, 500f, false), 0f)
        assertEquals(0.6f, FitMath.nextScale(0.6f, 0.6f, 100, 500f, true), 0f)
        var s = 1f
        var steps = 0
        while (!FitMath.atFloor(s, 0.6f) && steps < 50) {
            s = FitMath.nextScale(s, 0.6f, 100, 1000f * s, false)
            steps++
        }
        assertTrue(steps in 1..12)
    }

    @Test
    fun fontGrowsBackOnlyWhenTheRoomClearlyGrew() {
        assertFalse(FitMath.roomGrew(100, 107))
        assertTrue(FitMath.roomGrew(100, 109))
        assertFalse(FitMath.roomGrew(-1, 500))
        assertFalse(FitMath.roomGrew(100, Int.MAX_VALUE))
    }

    @Test
    fun onlyOrdinaryWordsCountAsBrokenInTheMiddle() {
        assertTrue(FitMath.splitsWord("Geschwindigkeitsbegrenzung ist", 10))
        assertTrue(FitMath.splitsWord("الاستفسارات والمساعدة", 5))
        assertFalse(FitMath.splitsWord("Hello world", 6))
        assertFalse(FitMath.splitsWord("IMG_20240101_123456.jpg", 9))
        assertFalse(FitMath.splitsWord("a/b/Storage/emulated", 10))
        assertFalse(FitMath.splitsWord("https://example.com/abc", 12))
        assertFalse(FitMath.splitsWord("a".repeat(80), 40))
        assertFalse(FitMath.splitsWord("abc", 0))
        assertFalse(FitMath.splitsWord("abc", 3))
    }

    @Test
    fun scriptsWithoutSpacesMayBreakAnywhere() {
        assertFalse(FitMath.splitsWord("这是一个很长的句子需要换行", 5))
        assertFalse(FitMath.splitsWord("สวัสดีครับทุกคน", 6))
    }

    @Test
    fun fitModesAreWhatTheyClaim() {
        assertFalse(FrameFit.None.active)
        assertTrue(FrameFit.Auto.active)
        assertTrue(FrameFit.ShrinkOnly.active && !FrameFit.ShrinkOnly.scroll)
        assertTrue(FrameFit.ScrollOnly.active && FrameFit.ScrollOnly.minScale == 1f)
    }
}
