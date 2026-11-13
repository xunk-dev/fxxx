package com.frameout.engine

import com.frameout.engine.index.IndexReuse
import com.frameout.engine.index.ScanWindow
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanWindowTest {
    private val hour = 3_600_000_000L

    @Test
    fun unboundedRequestScansTheWholeTrack() {
        assertNull(ScanWindow.of(0L, 0L, hour))
        assertNull(ScanWindow.of(0L, hour / 1000L, hour))
    }

    @Test
    fun unknownDurationScansTheWholeTrack() = assertNull(ScanWindow.of(1000L, 2000L, 0L))

    @Test
    fun shortRangeOfAnHourIsWindowed() {
        val w = ScanWindow.of(600_000L, 720_000L, hour)!!
        assertEquals(600_000_000L, w.startUs)
        assertEquals(720_000_000L, w.endUs)
        assertTrue(w.fractionOf(hour) < 0.05)
    }

    @Test
    fun openEndedRangeRunsToTheEnd() {
        val w = ScanWindow.of(1_800_000L, 0L, hour)!!
        assertEquals(hour, w.endUs)
    }

    @Test
    fun almostWholeTrackIsNotWorthWindowing() = assertNull(ScanWindow.of(60_000L, 0L, hour))

    @Test
    fun invertedRangeIsRejected() = assertNull(ScanWindow.of(900_000L, 600_000L, hour))

    @Test
    fun endBeyondTheTrackIsClamped() {
        val w = ScanWindow.of(3_000_000L, 9_999_999_000L, hour)!!
        assertEquals(hour, w.endUs)
    }

    @Test
    fun scanStopsOnlyAfterTheReorderMargin() {
        val w = ScanWindow(10_000_000L, 20_000_000L)
        assertFalse(w.isPastEnd(20_000_000L))
        assertFalse(w.isPastEnd(20_000_000L + ScanWindow.TAIL_US))
        assertTrue(w.isPastEnd(20_000_000L + ScanWindow.TAIL_US + 1L))
    }

    @Test
    fun estimateShrinksWithTheWindow() {
        val w = ScanWindow(0L, hour / 10)
        assertEquals(10_800, w.scaleEstimate(108_000, hour))
        assertEquals(0, w.scaleEstimate(0, hour))
    }

    @Test
    fun encodeRoundTrips() {
        val w = ScanWindow(5L, 99L)
        assertEquals(w, ScanWindow.decode(w.encode()))
        assertNull(ScanWindow.decode("full"))
        assertNull(ScanWindow.decode("9:3"))
        assertNull(ScanWindow.decode("a:b"))
        assertNull(ScanWindow.decode(null))
    }

    @Test
    fun fullIndexServesAnyRequest() {
        assertTrue(IndexReuse.coversRequest(null, null))
        assertTrue(IndexReuse.coversRequest(null, ScanWindow(1L, 2L)))
    }

    @Test
    fun partialIndexServesOnlyContainedRequests() {
        val stored = ScanWindow(100L, 900L)
        assertTrue(IndexReuse.coversRequest(stored, ScanWindow(100L, 900L)))
        assertTrue(IndexReuse.coversRequest(stored, ScanWindow(200L, 800L)))
        assertFalse(IndexReuse.coversRequest(stored, ScanWindow(50L, 800L)))
        assertFalse(IndexReuse.coversRequest(stored, ScanWindow(200L, 950L)))
        assertFalse(IndexReuse.coversRequest(stored, null))
    }

    @Test
    fun windowMarkerRoundTripsAndClears() {
        val dir = File.createTempFile("win", "d").apply { delete(); mkdirs(); deleteOnExit() }
        assertNull(IndexReuse.readWindow(dir))
        IndexReuse.writeWindow(dir, ScanWindow(7L, 70L))
        assertEquals(ScanWindow(7L, 70L), IndexReuse.readWindow(dir))
        IndexReuse.writeWindow(dir, null)
        assertNull(IndexReuse.readWindow(dir))
        IndexReuse.writeWindow(dir, ScanWindow(1L, 2L))
        IndexReuse.clearMarker(dir)
        assertNull(IndexReuse.readWindow(dir))
        assertNotNull(dir)
    }
}
