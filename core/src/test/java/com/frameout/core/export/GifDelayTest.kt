package com.frameout.core.export

import org.junit.Assert.assertEquals
import org.junit.Test

class GifDelayTest {
    @Test
    fun wholeCentisecondFramesAreExact() {
        val delay = GifDelay(50)
        repeat(5) { assertEquals(5, delay.nextCentiseconds()) }
    }

    @Test
    fun fractionalFramesKeepTheTotalDuration() {
        val delay = GifDelay(83)
        val total = (1..10).sumOf { delay.nextCentiseconds() }
        assertEquals(83, total)
    }

    @Test
    fun neverGoesBelowTwoCentiseconds() {
        val delay = GifDelay(5)
        repeat(10) { assertEquals(true, delay.nextCentiseconds() >= 2) }
    }
}
