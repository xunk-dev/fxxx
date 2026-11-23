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

    @Test fun indexAtReturnsDisplayedFrame() = assertEquals(1, FramePtsIndex.indexAt(index, 50L))

    @Test fun indexAtEmptyIsMinusOne() = assertEquals(-1, FramePtsIndex.indexAt(LongArray(0), 10L))

    @Test fun nearestPicksCloserFrame() {
        assertEquals(1, FramePtsIndex.nearestIndex(index, 45L))
        assertEquals(2, FramePtsIndex.nearestIndex(index, 50L))
        assertEquals(2, FramePtsIndex.nearestIndex(index, 55L))
        assertEquals(0, FramePtsIndex.nearestIndex(index, 10L))
        assertEquals(3, FramePtsIndex.nearestIndex(index, 5_000L))
    }

    @Test fun snapReturnsFrameMillisecond() {
        assertEquals(33L, FramePtsIndex.snapMs(index, 40L))
        assertEquals(66L, FramePtsIndex.snapMs(index, 60L))
        assertEquals(77L, FramePtsIndex.snapMs(LongArray(0), 77L))
    }

    @Test fun snapIsIdempotent() {
        for (ms in 0L..120L) {
            val once = FramePtsIndex.snapMs(index, ms)
            assertEquals(once, FramePtsIndex.snapMs(index, once))
        }
    }

    @Test fun snappedPositionDisplaysTheSnappedFrame() {
        for (ms in 0L..120L) {
            val snapped = FramePtsIndex.snapMs(index, ms)
            assertEquals(FramePtsIndex.nearestIndex(index, ms), FramePtsIndex.indexAt(index, snapped))
        }
    }

    @Test fun shiftMovesWholeFramesAndClamps() {
        assertEquals(66L, FramePtsIndex.shiftMs(index, 33L, 1))
        assertEquals(0L, FramePtsIndex.shiftMs(index, 33L, -1))
        assertEquals(100L, FramePtsIndex.shiftMs(index, 33L, 50))
        assertEquals(0L, FramePtsIndex.shiftMs(index, 33L, -50))
    }

    @Test fun averageFrameDuration() = assertEquals(33_333L, FramePtsIndex.averageFrameUs(index))
}
