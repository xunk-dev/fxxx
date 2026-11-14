package com.frameout.core.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactSheetPreviewTest {
    private val wide = 16f / 9f

    @Test
    fun emptySelectionHasNoPagesAndStillReportsOneRowOfSize() {
        val summary = ContactSheetPreview.summarize(0, wide, ContactSheetLayout.Options())
        assertTrue(summary.empty)
        assertEquals(0, summary.pages)
        assertEquals(0, summary.firstCount)
        assertEquals(0, summary.totalRows)
        assertTrue(summary.height > 0)
        assertTrue(summary.hasRoom)
    }

    @Test
    fun sizeMatchesTheRealLayoutPlan() {
        val options = ContactSheetLayout.Options(columns = 4)
        val summary = ContactSheetPreview.summarize(24, wide, options)
        val plan = ContactSheetLayout.plan(24, wide, options)
        assertEquals(plan.sheetHeight(0), summary.height)
        assertEquals(plan.sheets.size, summary.pages)
        assertEquals(6, summary.totalRows)
        assertEquals(24, summary.firstCount)
    }

    @Test
    fun megapixelsFollowWidthTimesHeight() {
        val summary = ContactSheetPreview.summarize(8, wide, ContactSheetLayout.Options(columns = 2))
        assertEquals(summary.width.toLong() * summary.height / 1_000_000f, summary.megapixels, 0.0001f)
    }

    @Test
    fun manyFramesSplitAcrossSheetsAndFirstSheetIsFull() {
        val summary = ContactSheetPreview.summarize(437, 9f / 16f, ContactSheetLayout.Options(columns = 3))
        assertTrue(summary.pages > 1)
        assertEquals(summary.perSheet, summary.firstCount)
        assertFalse(summary.hasRoom)
    }

    @Test
    fun tooManyAndTinyFlagsFollowTheLayout() {
        assertTrue(ContactSheetPreview.summarize(20000, 9f / 16f, ContactSheetLayout.Options(columns = 2)).tooMany)
        assertTrue(ContactSheetPreview.summarize(10, wide, ContactSheetLayout.Options(columns = 8)).tiny)
        assertFalse(ContactSheetPreview.summarize(10, wide, ContactSheetLayout.Options(columns = 4)).tiny)
    }
}
