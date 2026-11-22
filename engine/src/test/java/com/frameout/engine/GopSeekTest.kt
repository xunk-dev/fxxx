package com.frameout.engine

import com.frameout.engine.decode.GopSeek
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GopSeekTest {
    private val none = Long.MIN_VALUE

    @Test
    fun freshDecoderMustSeek() = assertFalse(GopSeek.canDecodeForward(none, 5_000_000L, 0L, false))

    @Test
    fun targetInsideTheCurrentGopDecodesForward() =
        assertTrue(GopSeek.canDecodeForward(12_000_000L, 20_000_000L, 10_000_000L, false))

    @Test
    fun targetInALaterGopSeeksBecauseItsKeyframeIsAhead() =
        assertFalse(GopSeek.canDecodeForward(12_000_000L, 90_000_000L, 80_000_000L, false))

    @Test
    fun targetBehindTheDecoderSeeksBack() =
        assertFalse(GopSeek.canDecodeForward(50_000_000L, 20_000_000L, 10_000_000L, false))

    @Test
    fun drainedDecoderMustSeek() =
        assertFalse(GopSeek.canDecodeForward(12_000_000L, 20_000_000L, 10_000_000L, true))

    @Test
    fun unknownKeyframeFallsBackToSeeking() =
        assertFalse(GopSeek.canDecodeForward(12_000_000L, 20_000_000L, -1L, false))

    @Test
    fun decoderExactlyOnTheKeyframeDecodesForward() =
        assertTrue(GopSeek.canDecodeForward(10_000_000L, 15_000_000L, 10_000_000L, false))
}
