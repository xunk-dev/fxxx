package com.frameout.core.quality

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TopShotTest {
    private fun frame(i: Int, score: Double = 100.0, sig: ByteArray? = null, tone: Float = 1f) =
        ScoredFrame(i.toLong(), i, score, sig = sig, tone = tone)

    private fun burst(n: Int = 40, score: (Int) -> Double = { 100.0 }) = (0 until n).map { frame(it, score(it)) }

    @Test
    fun aClearlySharperNeighbourReplacesTheOriginalPick() {
        val frames = burst { if (it == 21) 200.0 else 100.0 }
        assertEquals(listOf(21L), TopShot.refine(frames, listOf(20L), emptyMap()))
    }

    @Test
    fun aSmallDifferenceKeepsTheOriginalPick() {
        val frames = burst { if (it == 21) 105.0 else 100.0 }
        assertEquals(listOf(20L), TopShot.refine(frames, listOf(20L), emptyMap()))
    }

    @Test
    fun openEyesAndASmileBeatASharperFrameWithABlinkingFace() {
        val frames = burst { if (it == 20) 130.0 else 100.0 }
        val faces = mapOf(20L to 0.1f, 22L to 0.95f)
        assertEquals(listOf(22L), TopShot.refine(frames, listOf(20L), faces))
    }

    @Test
    fun neverLeavesTheMomentWindow() {
        val frames = burst { if (it == 39) 1000.0 else 100.0 }
        val refined = TopShot.refine(frames, listOf(10L), emptyMap())
        assertTrue(kotlin.math.abs(refined.single() - 10L) <= TopShot.MAX_RADIUS)
    }

    @Test
    fun neverLeavesItsShot() {
        val a = ByteArray(FrameSignature.SIZE) { 100 }
        val b = ByteArray(FrameSignature.SIZE) { if (it % 2 == 0) 0 else 200.toByte() }
        val frames = (0 until 40).map { frame(it, if (it == 21) 500.0 else 100.0, if (it <= 20) a else b) }
        assertEquals(listOf(20L), TopShot.refine(frames, listOf(20L), emptyMap()))
    }

    @Test
    fun aBlownOutFrameIsNotChosenEvenIfItScoresHigher() {
        val frames = (0 until 40).map { frame(it, if (it == 21) 300.0 else 100.0, tone = if (it == 21) FrameTone.MIN else 1f) }
        assertEquals(listOf(20L), TopShot.refine(frames, listOf(20L), emptyMap()))
    }

    @Test
    fun twoPicksNeverEndUpOnTheSameFrame() {
        val frames = burst { if (it == 21) 300.0 else 100.0 }
        val refined = TopShot.refine(frames, listOf(20L, 22L), emptyMap())
        assertEquals(2, refined.toSet().size)
    }

    @Test
    fun candidatesCoverOnlyTheWindowsAroundThePicks() {
        val frames = burst()
        val set = TopShot.candidates(frames, listOf(10L, 30L))
        assertTrue(10L in set && 30L in set)
        assertTrue(20L !in set)
    }

    @Test
    fun noPicksMeansNothingToRefine() {
        assertEquals(emptyList<Long>(), TopShot.refine(burst(), emptyList(), emptyMap()))
    }
}
