package com.frameout.core.quality

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportanceVisualTest {
    private fun flat(value: Int) = ByteArray(FrameSignature.SIZE) { value.toByte() }

    private fun checker() = ByteArray(FrameSignature.SIZE) { if (it % 2 == 0) 0 else 200.toByte() }

    private fun frame(i: Int, sig: ByteArray?, tone: Float = 1f) =
        ScoredFrame(i.toLong(), i, 100.0, sig = sig, tone = tone)

    @Test
    fun blackAndWhiteFramesGetTheLowestToneScore() {
        assertEquals(FrameTone.MIN, FrameTone.quality(IntArray(64 * 64) { 0 }, 64, 64), 1e-6f)
        assertEquals(FrameTone.MIN, FrameTone.quality(IntArray(64 * 64) { 255 }, 64, 64), 1e-6f)
    }

    @Test
    fun aFlatGreyFrameIsPenalisedForLackOfContrast() {
        assertEquals(0.35f, FrameTone.quality(IntArray(64 * 64) { 128 }, 64, 64), 1e-4f)
    }

    @Test
    fun aWellExposedFrameKeepsFullScore() {
        val gradient = IntArray(64 * 64) { (it % 64) * 255 / 63 }
        assertEquals(1f, FrameTone.quality(gradient, 64, 64), 1e-4f)
    }

    @Test
    fun aCutBetweenVeryDifferentLooksStartsANewShot() {
        val frames = (0 until 20).map { frame(it, if (it < 10) flat(100) else checker()) }
        val shots = ShotSegments.split(frames)
        assertEquals(2, shots.count)
        assertEquals(10, shots.sizes[0])
        assertEquals(10, shots.sizes[1])
    }

    @Test
    fun anUnchangingVideoIsOneShot() {
        val shots = ShotSegments.split((0 until 20).map { frame(it, flat(100)) })
        assertEquals(1, shots.count)
    }

    @Test
    fun theFrameClosestToTheShotAverageIsTheMostTypical() {
        val odd = ByteArray(FrameSignature.SIZE) { 140.toByte() }
        val frames = (0 until 9).map { frame(it, flat(100)) } + frame(9, odd)
        val typical = ShotSegments.typicality(frames, ShotSegments.Shots(IntArray(10), intArrayOf(10)))
        assertTrue(typical[0] > typical[9])
        assertEquals(0.0, typical[9], 1e-9)
    }

    @Test
    fun picksCoverEveryShotInsteadOfStackingOneScene() {
        val frames = (0 until 40).map { frame(it, if (it < 20) flat(100) else checker()) }
        val picks = FrameImportance.pick(frames, emptyMap())
        assertTrue(picks.any { it < 20 })
        assertTrue(picks.any { it >= 20 })
        assertTrue(picks.size <= FrameImportance.maxPicks(40))
    }

    @Test
    fun darkOrBlownFramesAreNotPicked() {
        val frames = (0 until 40).map { frame(it, null, tone = if (it in 10..14) FrameTone.MIN else 1f) }
        val picks = FrameImportance.pick(frames, emptyMap())
        assertFalse(picks.any { it in 10L..14L })
    }
}
