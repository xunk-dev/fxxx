package com.frameout.app

import com.frameout.app.trim.ClipNotes
import com.frameout.app.trim.ClipSampling
import com.frameout.app.trim.LimitedRange
import com.frameout.app.trim.OutputLock
import com.frameout.app.trim.TrimDirty
import com.frameout.app.trim.TrimLimits
import com.frameout.app.trim.TrimLock
import com.frameout.app.trim.TrimOutput
import com.frameout.app.trim.TrimSnapshot
import com.frameout.core.pro.ProFeature
import com.frameout.core.pro.ProTier
import com.frameout.engine.timeline.TrimRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrimSessionTest {
    private val full = TrimRange(0, 60_000, true)

    @Test
    fun untouchedStateIsNotDirty() {
        val opened = TrimSnapshot(full, TrimOutput.Frames)
        assertFalse(TrimDirty.changed(opened, TrimSnapshot(full, TrimOutput.Frames)))
    }

    @Test
    fun noBaselineYetIsNotDirty() {
        assertFalse(TrimDirty.changed(null, TrimSnapshot(TrimRange(1_000, 5_000, false), TrimOutput.Gif)))
    }

    @Test
    fun movingTheRangeIsDirty() {
        val opened = TrimSnapshot(full, TrimOutput.Frames)
        assertTrue(TrimDirty.changed(opened, TrimSnapshot(TrimRange(2_000, 60_000, false), TrimOutput.Frames)))
        assertTrue(TrimDirty.changed(opened, TrimSnapshot(TrimRange(0, 30_000, false), TrimOutput.Frames)))
    }

    @Test
    fun changingTheOutputIsDirty() {
        val opened = TrimSnapshot(full, TrimOutput.Frames)
        assertTrue(TrimDirty.changed(opened, TrimSnapshot(full, TrimOutput.Video)))
    }

    @Test
    fun theUseFullFlagAloneIsNotAChange() {
        val opened = TrimSnapshot(full, TrimOutput.Frames)
        assertFalse(TrimDirty.changed(opened, TrimSnapshot(TrimRange(0, 60_000, false), TrimOutput.Frames)))
    }

    @Test
    fun draggingBackToTheStartingRangeIsClean() {
        val start = TrimRange(5_000, 20_000, false)
        val opened = TrimSnapshot(start, TrimOutput.Video)
        val moved = TrimSnapshot(TrimRange(6_000, 20_000, false), TrimOutput.Video)
        assertTrue(TrimDirty.changed(opened, moved))
        assertFalse(TrimDirty.changed(opened, TrimSnapshot(start.copy(), TrimOutput.Video)))
    }

    @Test
    fun freeOutputsNeverLockEvenWhenNothingIsAllowed() {
        assertNull(TrimLock.check(TrimOutput.Frames) { _, _ -> false })
        assertNull(TrimLock.check(TrimOutput.Video) { _, _ -> false })
    }

    @Test
    fun adOutputsLockUntilAllowed() {
        listOf(TrimOutput.Live, TrimOutput.Gif, TrimOutput.Webp).forEach { output ->
            val lock = TrimLock.check(output) { _, _ -> false }
            assertEquals(OutputLock(output, ProTier.Ad, ProFeature.AdvancedExport, TrimOutput.Video), lock)
            assertNull(TrimLock.check(output) { feature, tier -> feature == ProFeature.AdvancedExport && tier == ProTier.Ad })
        }
    }

    @Test
    fun freeFallbackIsVideoForLockedOutputsAndIdentityForFreeOnes() {
        assertEquals(TrimOutput.Video, TrimLock.freeFallback(TrimOutput.Live))
        assertEquals(TrimOutput.Video, TrimLock.freeFallback(TrimOutput.Gif))
        assertEquals(TrimOutput.Video, TrimLock.freeFallback(TrimOutput.Webp))
        assertEquals(TrimOutput.Frames, TrimLock.freeFallback(TrimOutput.Frames))
        assertEquals(TrimOutput.Video, TrimLock.freeFallback(TrimOutput.Video))
    }

    @Test
    fun adLockReportsOneAdItemAndNoPro() {
        val report = TrimLock.check(TrimOutput.Gif) { _, _ -> false }!!.toReport()
        assertEquals(0, report.pro)
        assertEquals(1, report.ad)
        assertNull(report.proFeature)
        assertEquals(setOf(ProFeature.AdvancedExport), report.adFeatures)
        assertFalse(report.mixed)
    }

    @Test
    fun proLockReportsProFeatureAndNoAdFeatures() {
        val report = OutputLock(TrimOutput.Gif, ProTier.Pro, ProFeature.AdvancedExport, TrimOutput.Video).toReport()
        assertEquals(1, report.pro)
        assertEquals(0, report.ad)
        assertEquals(ProFeature.AdvancedExport, report.proFeature)
        assertTrue(report.adFeatures.isEmpty())
    }

    @Test
    fun notesAreJoinedAndBlankOnesDropped() {
        assertEquals("Saved", ClipNotes.join("Saved", emptyList()))
        assertEquals("Saved", ClipNotes.join("Saved", listOf(null, "", "  ")))
        assertEquals("Saved · A · B", ClipNotes.join("Saved", listOf("A", null, "B")))
    }

    @Test
    fun cutsOnlyWhenTheRangeIsLongerThanTheLimit() {
        val gif = TrimOutput.Gif.maxMs
        assertTrue(TrimLimits.cuts(TrimRange(0, gif + 1, false), gif))
        assertFalse(TrimLimits.cuts(TrimRange(0, gif, false), gif))
        assertFalse(TrimLimits.cuts(TrimRange(0, 600_000, false), TrimOutput.Video.maxMs))
    }

    @Test
    fun limitedOutputKeepsTheOriginalRangeForLater() {
        val original = TrimRange(0, 60_000, false)
        val switched = TrimLimits.switchTo(original, null, TrimOutput.Gif.maxMs, 20_000L)
        assertEquals(TrimRange(20_000, 28_000, false), switched.range)
        assertEquals(original, switched.stash)
        val back = TrimLimits.switchTo(switched.range, switched.stash, TrimOutput.Video.maxMs, 0L)
        assertEquals(LimitedRange(original, null), back)
    }

    @Test
    fun switchingBetweenLimitedOutputsKeepsTheEarliestRange() {
        val original = TrimRange(0, 60_000, false)
        val gif = TrimLimits.switchTo(original, null, TrimOutput.Gif.maxMs, 20_000L)
        val live = TrimLimits.switchTo(gif.range, gif.stash, TrimOutput.Live.maxMs, 25_000L)
        assertEquals(3_000L, live.range.spanMs())
        assertEquals(original, live.stash)
    }

    @Test
    fun switchingWithNothingToCutChangesNothing() {
        val short = TrimRange(1_000, 3_000, false)
        assertEquals(LimitedRange(short, null), TrimLimits.switchTo(short, null, TrimOutput.Gif.maxMs, 0L))
        assertEquals(LimitedRange(short, null), TrimLimits.switchTo(short, null, TrimOutput.Video.maxMs, 0L))
    }

    @Test
    fun gifIsReportedAsReducedOnlyWhenTheBudgetShrinksARealDownscale() {
        assertEquals(547, ClipSampling.gifReducedSide(1080, 1080, 160))
        assertNull(ClipSampling.gifReducedSide(1080, 1080, 20))
        assertNull(ClipSampling.gifReducedSide(400, 400, 160))
        assertNull(ClipSampling.gifReducedSide(1920, 1080, 160))
    }
}
