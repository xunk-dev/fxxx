package com.frameout.engine

import com.frameout.engine.edit.SourceBitrate
import org.junit.Assert.assertEquals
import org.junit.Test

class SourceBitrateTest {
    @Test
    fun targetAddsTwentyPercentHeadroom() {
        assertEquals(20_000_000, SourceBitrate.target(16_666_667L))
    }

    @Test
    fun targetIsClampedToSaneBounds() {
        assertEquals(1_000_000, SourceBitrate.target(100_000L))
        assertEquals(200_000_000, SourceBitrate.target(900_000_000L))
    }

    @Test
    fun estimateSubtractsAudioFromTheContainerAverage() {
        assertEquals(15_840_000L, SourceBitrate.estimate(20_000_000L, 10_000_000L, 160_000L))
    }

    @Test
    fun estimateIsZeroWithoutSizeOrDuration() {
        assertEquals(0L, SourceBitrate.estimate(0L, 10_000_000L, 0L))
        assertEquals(0L, SourceBitrate.estimate(1_000L, 0L, 0L))
    }
}
