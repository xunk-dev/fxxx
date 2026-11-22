package com.frameout.app

import com.frameout.app.collage.previewSideFor
import com.frameout.core.collage.CollageTemplates
import com.frameout.core.collage.compacted
import com.frameout.core.collage.snapped
import com.frameout.core.collage.toPx
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CollageTemplateTest {
    @Test
    fun suggestPicksSmallestTemplateThatFits() {
        assertEquals("2x1", CollageTemplates.suggest(0).id)
        assertEquals("2x1", CollageTemplates.suggest(2).id)
        assertEquals("2x2", CollageTemplates.suggest(4).id)
        assertEquals("film_strip", CollageTemplates.suggest(5).id)
        assertEquals("2x3", CollageTemplates.suggest(6).id)
        assertEquals("contact_sheet", CollageTemplates.suggest(99).id)
    }

    @Test
    fun maxCellsIsLargestTemplateAndExceedsNine() {
        assertEquals(CollageTemplates.all.maxOf { it.cells.size }, CollageTemplates.maxCells)
        assertTrue(CollageTemplates.maxCells > 9)
        assertEquals("p10a", CollageTemplates.suggest(10).id)
        assertEquals("4x3", CollageTemplates.suggest(11).id)
        assertEquals("4x4", CollageTemplates.suggest(16).id)
        assertEquals("contact_sheet", CollageTemplates.suggest(17).id)
        assertEquals("contact_sheet", CollageTemplates.suggest(CollageTemplates.maxCells).id)
    }

    @Test
    fun previewSideShrinksAsCellsGrow() {
        assertTrue(previewSideFor(4) > previewSideFor(9))
        assertTrue(previewSideFor(9) > previewSideFor(20))
    }

    @Test
    fun templateIdsAreUnique() {
        val ids = CollageTemplates.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun outerMarginEqualsGapAndInnerGapIsFullGap() {
        val grid = CollageTemplates.all.first { it.id == "2x2" }
        val a = grid.cells[0].toPx(1000f, 1000f, 20f)
        val b = grid.cells[1].toPx(1000f, 1000f, 20f)
        assertEquals(20f, a.left, 0.01f)
        assertEquals(20f, a.top, 0.01f)
        assertEquals(20f, b.left - a.right, 0.01f)
        assertEquals(980f, b.right, 0.01f)
    }

    @Test
    fun cellsNeverCollapseEvenWithHugeGap() {
        CollageTemplates.all.forEach { t ->
            t.cells.forEach { c -> assertTrue(c.toPx(100f, 100f, 400f).width >= 1f) }
        }
    }

    @Test
    fun snappedEdgesAbutExactlyWithoutGap() {
        val grid = CollageTemplates.all.first { it.id == "3x3" }
        val a = grid.cells[0].toPx(1000f, 1000f, 0f).snapped()
        val b = grid.cells[1].toPx(1000f, 1000f, 0f).snapped()
        assertEquals(a.right, b.left, 0f)
    }

    @Test
    fun snappedInnerGapIsUniformForIntegerGap() {
        val grid = CollageTemplates.all.first { it.id == "3x3" }
        listOf(0, 1, 3, 8).forEach { gap ->
            val a = grid.cells[0].toPx(1001f, 1001f, gap.toFloat()).snapped()
            val b = grid.cells[1].toPx(1001f, 1001f, gap.toFloat()).snapped()
            val c = grid.cells[2].toPx(1001f, 1001f, gap.toFloat()).snapped()
            assertEquals(gap.toFloat(), b.left - a.right, 0f)
            assertEquals(gap.toFloat(), c.left - b.right, 0f)
        }
    }

    @Test
    fun compactedLeavesNoHoleForAnyTemplateAndCount() {
        CollageTemplates.all.forEach { t ->
            for (n in 1 until t.cells.size) {
                val c = t.compacted(n)
                assertEquals("${t.id} $n", n, c.cells.size)
                val area = c.cells.sumOf { (it.w * it.h).toDouble() }
                assertEquals("${t.id} $n", 1.0, area, 0.001)
                c.cells.forEach { r ->
                    assertTrue(r.x >= -0.001f && r.y >= -0.001f && r.x + r.w <= 1.001f && r.y + r.h <= 1.001f)
                }
            }
        }
    }

    @Test
    fun compactedKeepsFullTemplateUntouched() {
        val t = CollageTemplates.all.first { it.id == "2x2" }
        assertEquals(t, t.compacted(4))
        assertEquals(t, t.compacted(9))
    }

    @Test
    fun threeFramesGetAFreeTemplateWithNoEmptyCell() {
        val t = CollageTemplates.suggest(3)
        assertEquals(3, t.cells.size)
        assertTrue(com.frameout.core.pro.ProCatalog.collageIsFree(t.id))
    }
}
