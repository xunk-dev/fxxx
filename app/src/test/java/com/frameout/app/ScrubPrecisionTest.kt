package com.frameout.app

import com.frameout.app.trim.ScrubPrecision
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScrubPrecisionTest {
    @Test
    fun barGetsFinerAsTheFingerMovesAway() {
        assertEquals(1f, ScrubPrecision.forBar(0f), 0f)
        assertEquals(1f, ScrubPrecision.forBar(56f), 0f)
        assertEquals(0.5f, ScrubPrecision.forBar(80f), 0f)
        assertEquals(0.2f, ScrubPrecision.forBar(150f), 0f)
        assertEquals(ScrubPrecision.BAR_FINEST, ScrubPrecision.forBar(400f), 0f)
    }

    @Test
    fun barTierIgnoresDirection() {
        assertEquals(ScrubPrecision.forBar(150f), ScrubPrecision.forBar(-150f), 0f)
    }

    @Test
    fun surfaceNeedsMoreTravelPerFrameAsTheFingerMovesAway() {
        assertEquals(10f, ScrubPrecision.dpPerFrame(0f), 0f)
        assertEquals(24f, ScrubPrecision.dpPerFrame(100f), 0f)
        assertEquals(ScrubPrecision.SURFACE_FINEST_DP, ScrubPrecision.dpPerFrame(300f), 0f)
        assertEquals(ScrubPrecision.dpPerFrame(-100f), ScrubPrecision.dpPerFrame(100f), 0f)
    }

    @Test
    fun finestTierStillMovesOneWholeFrame() {
        assertEquals(1, ScrubPrecision.framesFor(ScrubPrecision.SURFACE_FINEST_DP, ScrubPrecision.SURFACE_FINEST_DP))
        assertEquals(0, ScrubPrecision.framesFor(ScrubPrecision.SURFACE_FINEST_DP * 0.4f, ScrubPrecision.SURFACE_FINEST_DP))
        assertEquals(-2, ScrubPrecision.framesFor(-20f, 10f))
        assertTrue(ScrubPrecision.framesFor(5f, 0f) == 0)
    }

    @Test
    fun labelsAreEmptyOnlyAtNormalSpeed() {
        assertEquals("", ScrubPrecision.barLabel(1f))
        assertTrue(ScrubPrecision.barLabel(0.5f).isNotEmpty())
        assertTrue(ScrubPrecision.barLabel(ScrubPrecision.BAR_FINEST).isNotEmpty())
        assertEquals("", ScrubPrecision.surfaceLabel(10f))
        assertTrue(ScrubPrecision.surfaceLabel(24f).isNotEmpty())
        assertTrue(ScrubPrecision.surfaceLabel(64f).isNotEmpty())
    }
}
