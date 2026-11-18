package com.frameout.core.pro

import com.frameout.core.edit.CoverMark
import com.frameout.core.edit.CoverStyle
import com.frameout.core.edit.FrameEdit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProEditGuardReportTest {
    @Before
    fun noEntitlements() {
        ProAccess.source = null
    }

    @After
    fun cleanup() {
        ProAccess.source = null
    }

    private fun cover(style: CoverStyle) = CoverMark(0f, 0f, 1f, 1f, style)

    @Test
    fun freeEditsProduceNoReport() {
        assertNull(ProEditGuard.report(listOf(FrameEdit(1L), FrameEdit(2L, marks = listOf(cover(CoverStyle.Solid))))))
        assertNull(ProEditGuard.report(emptyList<FrameEdit>()))
    }

    @Test
    fun countsAcrossFramesAndSeparatesTiers() {
        val a = FrameEdit(1L, marks = listOf(cover(CoverStyle.Blur)))
        val b = FrameEdit(2L, marks = listOf(cover(CoverStyle.Pixelate), cover(CoverStyle.Blur)))
        val report = ProEditGuard.report(listOf(a, b))!!
        assertEquals(1, report.pro)
        assertEquals(2, report.ad)
        assertTrue(report.mixed)
        assertEquals(ProFeature.EditTools, report.proFeature)
        assertEquals(setOf(ProFeature.EditTools), report.adFeatures)
    }

    @Test
    fun singleEditOverloadMatchesTheListOverload() {
        val edit = FrameEdit(1L, marks = listOf(cover(CoverStyle.Blur)))
        val one = ProEditGuard.report(edit)!!
        assertEquals(0, one.pro)
        assertEquals(1, one.ad)
        assertFalse(one.mixed)
    }

    @Test
    fun sanitizeDropsEveryLockedMark() {
        val edit = FrameEdit(1L, marks = listOf(cover(CoverStyle.Solid), cover(CoverStyle.Blur), cover(CoverStyle.Pixelate)))
        assertEquals(listOf(cover(CoverStyle.Solid)), ProEditGuard.sanitize(edit).marks)
    }
}
