package com.frameout.engine

import com.frameout.engine.extract.ExtractMode
import com.frameout.engine.extract.SeekPolicy
import com.frameout.engine.extract.SparsePlan
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SparsePlanTest {
    private val hour = 3_600_000_000L
    private val frames = 108_000

    private fun plan(mode: ExtractMode, start: Long = 0L, end: Long = hour) =
        SparsePlan.plan(mode, start, end, hour, 0L, frames)

    @Test
    fun threeFramesOfAnHourNeedNoIndex() {
        val p = plan(ExtractMode.FixedCount(3))!!
        assertEquals(3, p.count)
        val pts = (0 until 3).map { p.sample(it).ptsUs }
        assertEquals(0L, pts.first())
        assertTrue(pts.last() > hour - 100_000L && pts.last() < hour)
        assertTrue(pts.zipWithNext().all { (a, b) -> b > a })
    }

    @Test
    fun shortClipsAlwaysUseTheExactIndexSoTheCountIsHonoured() {
        assertNull(plan(ExtractMode.FixedCount(5), 0L, 10_000_000L))
        assertNull(plan(ExtractMode.FixedCount(5), 0L, 40_000_000L))
        assertNotNull(plan(ExtractMode.FixedCount(5), 0L, 120_000_000L))
    }

    @Test
    fun singleFrameTakesTheFirstSample() {
        assertEquals(0L, plan(ExtractMode.FixedCount(1))!!.sample(0).ptsUs)
    }

    @Test
    fun trimmedRangeIsRespected() {
        val p = plan(ExtractMode.FixedCount(4), 600_000_000L, 1_200_000_000L)!!
        assertEquals(600_000_000L, p.sample(0).ptsUs)
        assertTrue(p.sample(3).ptsUs < 1_200_000_000L)
    }

    @Test
    fun countNearFrameTotalFallsBackToExactIndex() {
        assertNull(plan(ExtractMode.FixedCount(frames)))
        assertNull(plan(ExtractMode.FixedCount(frames / 2)))
    }

    @Test
    fun everyNMillisCoversTheRange() {
        val p = plan(ExtractMode.EveryNMillis(1000))!!
        assertEquals(3600, p.count)
        assertEquals(0L, p.sample(0).ptsUs)
        assertEquals(3_599_000_000L, p.sample(3599).ptsUs)
    }

    @Test
    fun stepShorterThanAFewFramesFallsBackToExactIndex() {
        assertNull(plan(ExtractMode.EveryNMillis(50)))
    }

    @Test
    fun frameBasedAndKeyframeModesStayOnTheIndex() {
        assertNull(plan(ExtractMode.EveryFrame))
        assertNull(plan(ExtractMode.EveryNFrames(10)))
        assertNull(plan(ExtractMode.KeyframesOnly))
        assertNull(plan(ExtractMode.SceneChange))
    }

    @Test
    fun unknownFrameRateFallsBack() {
        assertNull(SparsePlan.plan(ExtractMode.FixedCount(3), 0L, hour, hour, 0L, 0))
    }

    @Test
    fun farTargetsAlwaysSeekAndCloseOnesDecodeThrough() {
        val far = plan(ExtractMode.FixedCount(3))!!
        assertTrue(SeekPolicy.needsSeek(far.sample(1), 0))
        val close = plan(ExtractMode.EveryNMillis(500))!!
        assertTrue(SeekPolicy.needsSeek(close.sample(0), -1))
        assertTrue(!SeekPolicy.needsSeek(close.sample(1), 0))
    }

    @Test
    fun resumeContinuesAfterTheCheckpointOrdinal() = runBlocking {
        val p = plan(ExtractMode.FixedCount(5))!!
        assertEquals(listOf(3, 4), p.flow(2).toList().map { it.index })
        assertEquals(5, p.flow(-1).toList().size)
        assertNotNull(p.sample(0))
    }
}
