package com.frameout.app

import com.frameout.app.trim.FrameTimeline
import com.frameout.engine.extract.ExtractMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FrameTimelineTest {
    private fun steady(fps: Int, seconds: Int, offsetUs: Long = 0L): FrameTimeline =
        FrameTimeline.of(LongArray(fps * seconds) { offsetUs + it * 1_000_000L / fps }, LongArray(seconds) { offsetUs + it * 1_000_000L })

    @Test
    fun countsOnlyRealFramesInsideTheHalfOpenRange() {
        val timeline = steady(30, 10)
        assertEquals(5, timeline.countIn(0L, 166L))
        assertEquals(6, timeline.countIn(0L, 167L))
        assertEquals(5, timeline.countIn(10L, 200L))
        assertEquals(30, timeline.countIn(1000L, 2000L))
    }

    @Test
    fun lowFrameRateSourceIsNotOvercounted() {
        val timeline = steady(20, 10)
        assertEquals(3, timeline.countIn(0L, 150L))
        assertEquals(4, timeline.countIn(0L, 151L))
    }

    @Test
    fun irregularTimestampsAreCountedAsTheyAre() {
        val timeline = FrameTimeline.of(longArrayOf(0L, 40_000L, 100_000L, 400_000L, 405_000L))
        assertEquals(3, timeline.countIn(0L, 300L))
        assertEquals(2, timeline.countIn(300L, 500L))
    }

    @Test
    fun unsortedDecodeOrderGivesTheSameCount() {
        val timeline = FrameTimeline.of(longArrayOf(0L, 99_000L, 33_000L, 66_000L, 165_000L, 132_000L))
        assertEquals(6, timeline.countIn(0L, 200L))
    }

    @Test
    fun everyNFramesCountsFromTheFirstFrameInTheRange() {
        val timeline = steady(30, 10)
        assertEquals(3, timeline.everyNth(0L, 300L, 4))
        assertEquals(5, timeline.everyNth(0L, 166L, 1))
    }

    @Test
    fun timeGridPicksTheFirstFrameAtOrAfterEachTarget() {
        val timeline = steady(30, 10)
        assertEquals(10, timeline.onTimeGrid(0L, 10_000L, 1000L))
        assertEquals(1, timeline.onTimeGrid(0L, 400L, 1000L))
    }

    @Test
    fun timeGridDoesNotRepeatAFrameWhenTheStepIsShorterThanTheFrameGap() {
        val timeline = steady(10, 5)
        assertEquals(50, timeline.onTimeGrid(0L, 5_000L, 20L))
    }

    @Test
    fun keyframesAreCountedInsideTheRangeOnly() {
        val timeline = steady(30, 10)
        assertEquals(3, timeline.keyframesIn(2000L, 5000L))
        assertEquals(0, timeline.keyframesIn(2100L, 2900L))
    }

    @Test
    fun fixedCountIsCappedByTheFramesThatExist() {
        val timeline = steady(30, 10)
        assertEquals(5, timeline.count(ExtractMode.FixedCount(50), 0L, 166L))
        assertEquals(10, timeline.count(ExtractMode.FixedCount(10), 0L, 5000L))
    }

    @Test
    fun smartModesHaveNoExactCount() {
        val timeline = steady(30, 10)
        assertNull(timeline.count(ExtractMode.SceneChange, 0L, 5000L))
        assertNull(timeline.count(ExtractMode.Slides, 0L, 5000L))
    }

    @Test
    fun streamsThatStartLateAreCountedByRealTimestamps() {
        val timeline = steady(30, 10, offsetUs = 500_000L)
        assertEquals(0, timeline.countIn(0L, 500L))
        assertEquals(15, timeline.countIn(0L, 1000L))
    }
}
