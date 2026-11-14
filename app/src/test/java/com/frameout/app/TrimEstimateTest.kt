package com.frameout.app

import com.frameout.app.trim.EstimateKind
import com.frameout.app.trim.estimateFrames
import com.frameout.app.trim.estimateKind
import com.frameout.app.trim.isExactEstimate
import com.frameout.engine.extract.ExtractMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrimEstimateTest {
    private val hour = 3_600_000L

    @Test
    fun sceneAndSlidesAreNotCountedPerCandidate() {
        assertTrue(estimateFrames(hour, ExtractMode.SceneChange) < 2_000)
        assertTrue(estimateFrames(hour, ExtractMode.Slides) < 500)
    }

    @Test
    fun timedModeRoundsUpLikeTheEngine() {
        assertEquals(11, estimateFrames(10_500L, ExtractMode.EveryNMillis(1000L)))
        assertEquals(10, estimateFrames(10_000L, ExtractMode.EveryNMillis(1000L)))
    }

    @Test
    fun everyNFramesRoundsUpBecauseTheFirstFrameIsAlwaysTaken() {
        assertEquals(4, estimateFrames(1_000L, ExtractMode.EveryNFrames(8)))
        assertEquals(3, estimateFrames(1_000L, ExtractMode.EveryNFrames(10)))
    }

    @Test
    fun knownFrameRateGivesTheExactClipLength() {
        assertEquals(5, estimateFrames(167L, ExtractMode.EveryFrame, 30f))
        assertEquals(60, estimateFrames(2_000L, ExtractMode.EveryFrame, 30f))
    }

    @Test
    fun onlySmartModesAreFlaggedAsApproximate() {
        assertTrue(isExactEstimate(ExtractMode.FixedCount(5)))
        assertTrue(!isExactEstimate(ExtractMode.SceneChange))
        assertTrue(!isExactEstimate(ExtractMode.Slides))
    }

    @Test
    fun fixedCountNeverExceedsAvailableFrames() {
        assertEquals(30, estimateFrames(1_000L, ExtractMode.FixedCount(500)))
        assertEquals(10, estimateFrames(60_000L, ExtractMode.FixedCount(10)))
    }

    @Test
    fun skippingOptionsTurnTheCountIntoAnUpperBound() {
        assertEquals(EstimateKind.Exact, estimateKind(ExtractMode.EveryFrame, false, false, false))
        assertEquals(EstimateKind.UpperBound, estimateKind(ExtractMode.EveryFrame, true, false, false))
        assertEquals(EstimateKind.UpperBound, estimateKind(ExtractMode.EveryNFrames(3), false, true, false))
        assertEquals(EstimateKind.UpperBound, estimateKind(ExtractMode.FixedCount(5), false, false, true))
        assertEquals(EstimateKind.Approx, estimateKind(ExtractMode.SceneChange, true, true, true))
    }
}
