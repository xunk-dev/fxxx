package com.frameout.engine.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FramePtsIndexTest {
    private val index = longArrayOf(0L, 33_333L, 66_666L, 100_000L)

    @Test fun forwardMovesToNextFrame() = assertEquals(66L, FramePtsIndex.step(index, 33L, true, 1))

    @Test fun forwardClampsToLastFrame() = assertEquals(100L, FramePtsIndex.step(index, 33L, true, 10))

    @Test fun forwardAtEndCannotMove() = assertNull(FramePtsIndex.step(index, 100L, true, 1))

    @Test fun backwardMovesToPreviousFrame() = assertEquals(33L, FramePtsIndex.step(index, 66L, false, 1))

    @Test fun backwardClampsToFirstFrame() = assertEquals(0L, FramePtsIndex.step(index, 66L, false, 10))

    @Test fun backwardAtStartCannotMove() = assertNull(FramePtsIndex.step(index, 0L, false, 1))

    @Test fun stepUsReturnsExactPts() = assertEquals(66_666L, FramePtsIndex.stepUs(index, 33L, true, 1))

    @Test fun displayedUsPicksFrameAtPosition() = assertEquals(33_333L, FramePtsIndex.displayedUs(index, 50L))

    @Test fun windowSpansBackAndForwardFrames() = assertEquals(0L to 66_666L, FramePtsIndex.window(index, 33L, 5, 1))

    @Test fun windowClampsToIndexBounds() = assertEquals(0L to 100_000L, FramePtsIndex.window(index, 50L, 99, 99))

    @Test fun emptyIndexHasNoWindow() = assertNull(FramePtsIndex.window(LongArray(0), 0L, 5, 5))
}
