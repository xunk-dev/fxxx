package com.frameout.core.quality

import com.frameout.core.model.ProjectStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanTimingTest {
    @Test
    fun onlyACompletedExtractionStartsTheScan() {
        assertTrue(ScanTiming.mayAutoStart(ProjectStatus.Completed.name))
    }

    @Test
    fun everyOtherStatusWaits() {
        ProjectStatus.entries.filter { it != ProjectStatus.Completed }.forEach {
            assertFalse(it.name, ScanTiming.mayAutoStart(it.name))
        }
    }

    @Test
    fun missingProjectNeverStarts() {
        assertFalse(ScanTiming.mayAutoStart(null))
        assertFalse(ScanTiming.mayAutoStart(""))
        assertFalse(ScanTiming.mayAutoStart("SUCCEEDED"))
    }

    @Test
    fun runningAutoScanHaltsWhenExtractionLeavesCompleted() {
        assertTrue(ScanTiming.shouldHalt(ProjectStatus.Extracting.name, running = true, manual = false))
        assertTrue(ScanTiming.shouldHalt(ProjectStatus.Paused.name, running = true, manual = false))
    }

    @Test
    fun manualScanIsNeverHalted() {
        assertFalse(ScanTiming.shouldHalt(ProjectStatus.Extracting.name, running = true, manual = true))
    }

    @Test
    fun nothingToHaltWhenIdleOrStillCompleted() {
        assertFalse(ScanTiming.shouldHalt(ProjectStatus.Extracting.name, running = false, manual = false))
        assertFalse(ScanTiming.shouldHalt(ProjectStatus.Completed.name, running = true, manual = false))
    }

    @Test
    fun unfinishedExtractionStatusesWaitForExtraction() {
        assertTrue(ScanTiming.waitsForExtraction(ProjectStatus.Queued.name))
        assertTrue(ScanTiming.waitsForExtraction(ProjectStatus.Indexing.name))
        assertTrue(ScanTiming.waitsForExtraction(ProjectStatus.Extracting.name))
        assertFalse(ScanTiming.waitsForExtraction(ProjectStatus.Completed.name))
        assertTrue(ScanTiming.waitsForExtraction(ProjectStatus.Paused.name))
        assertFalse(ScanTiming.waitsForExtraction(ProjectStatus.Cancelled.name))
        assertFalse(ScanTiming.waitsForExtraction(ProjectStatus.Failed.name))
        assertFalse(ScanTiming.waitsForExtraction(null))
    }
}
