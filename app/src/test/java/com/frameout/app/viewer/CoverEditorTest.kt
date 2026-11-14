package com.frameout.app.viewer

import com.frameout.core.edit.BrushCoverMark
import com.frameout.core.edit.CoverMark
import com.frameout.core.edit.CoverStyle
import com.frameout.core.edit.MarkPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoverEditorTest {
    private val rect = CoverMark(0.2f, 0.3f, 0.4f, 0.5f, CoverStyle.Blur)

    @Test
    fun shiftMovesRectangleByTheDelta() {
        val moved = shiftedCover(rect, 0.1f, -0.1f) as CoverMark
        assertEquals(0.3f, moved.left, 1e-5f)
        assertEquals(0.5f, moved.right, 1e-5f)
        assertEquals(0.2f, moved.top, 1e-5f)
        assertEquals(0.4f, moved.bottom, 1e-5f)
    }

    @Test
    fun shiftStopsAtTheImageEdge() {
        val moved = shiftedCover(rect, 5f, 5f) as CoverMark
        assertEquals(1f, moved.right, 1e-5f)
        assertEquals(1f, moved.bottom, 1e-5f)
        assertEquals(0.2f, moved.right - moved.left, 1e-5f)
        val back = shiftedCover(rect, -5f, -5f) as CoverMark
        assertEquals(0f, back.left, 1e-5f)
        assertEquals(0f, back.top, 1e-5f)
    }

    @Test
    fun shiftMovesEveryPenPointAndKeepsTheStroke() {
        val pen = BrushCoverMark(CoverStyle.Pixelate, 0.5f, 0.04f, listOf(MarkPoint(0.1f, 0.1f), MarkPoint(0.3f, 0.2f)))
        val moved = shiftedCover(pen, 0.1f, 0.2f) as BrushCoverMark
        assertEquals(0.2f, moved.points[0].x, 1e-5f)
        assertEquals(0.3f, moved.points[0].y, 1e-5f)
        assertEquals(0.4f, moved.points[1].x, 1e-5f)
        assertEquals(0.4f, moved.points[1].y, 1e-5f)
        assertEquals(CoverStyle.Pixelate, moved.style)
        assertEquals(0.04f, moved.width, 1e-6f)
    }

    @Test
    fun styleAndStrengthApplyToBothKinds() {
        val pen = BrushCoverMark(CoverStyle.Blur, 0.2f, 0.04f, listOf(MarkPoint(0.1f, 0.1f)))
        assertEquals(CoverStyle.Solid, (rect.withStyle(CoverStyle.Solid) as CoverMark).style)
        assertEquals(CoverStyle.Solid, (pen.withStyle(CoverStyle.Solid) as BrushCoverMark).style)
        assertEquals(0.9f, (rect.withStrength(0.9f) as CoverMark).strength, 1e-6f)
        assertEquals(0.9f, (pen.withStrength(0.9f) as BrushCoverMark).strength, 1e-6f)
    }

    @Test
    fun rectanglesAndPenStrokesCountAsCovers() {
        assertTrue(rect.isCover())
        assertTrue(BrushCoverMark(CoverStyle.Blur, 0.5f, 0.04f, emptyList()).isCover())
    }
}
