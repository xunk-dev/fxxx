package com.frameout.core.collage

import com.frameout.core.pro.ProCatalog
import com.frameout.core.pro.ProTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BlendLayoutTest {
    private val grid = CollageTemplates.all.first { it.id == "2x2" }

    private fun compute(blend: CollageBlend, strength: Int = 100, gap: Float = 8f) =
        BlendLayout.compute(grid, blend, strength, 1000f, 1000f, gap)

    @Test
    fun idsAreUniqueAndResolve() {
        assertEquals(CollageBlend.all.size, CollageBlend.all.map { it.id }.toSet().size)
        CollageBlend.all.forEach { assertEquals(it, CollageBlend.fromId(it.id)) }
        assertEquals(CollageBlend.None, CollageBlend.fromId("missing"))
    }

    @Test
    fun everyBlendHasATier() {
        CollageBlend.all.forEach { b ->
            val tier = ProCatalog.blendTier(b.id)
            if (b == CollageBlend.None) assertEquals(ProTier.Free, tier) else assertTrue(b.id, tier != ProTier.Free)
        }
        assertTrue(CollageBlend.all.any { ProCatalog.blendTier(it.id) == ProTier.Ad })
        assertTrue(CollageBlend.all.any { ProCatalog.blendTier(it.id) == ProTier.Pro })
    }

    @Test
    fun collageTierTakesTheHigherOfLayoutAndBlend() {
        assertEquals(ProTier.Free, ProCatalog.collageTier("2x2", "none"))
        assertEquals(ProTier.Ad, ProCatalog.collageTier("3x3", "none"))
        assertEquals(ProTier.Ad, ProCatalog.collageTier("2x2", "wave"))
        assertEquals(ProTier.Pro, ProCatalog.collageTier("2x2", "heart"))
    }

    @Test
    fun polygonsStayInsideTheCanvas() {
        CollageBlend.all.filter { it.family == BlendFamily.Edge || it.family == BlendFamily.Cell }.forEach { b ->
            compute(b).forEach { look ->
                assertNotNull(look.polygon)
                val p = look.polygon!!
                for (i in p.indices step 2) {
                    assertTrue(b.id, p[i] in 0f..1000f && p[i + 1] in 0f..1000f)
                }
            }
        }
    }

    @Test
    fun outerBorderStaysStraightForEdgeStyles() {
        val first = compute(CollageBlend.Wave, 100, 8f)[0].polygon!!
        for (i in first.indices step 2) {
            if (first[i + 1] < 9f) assertEquals(8f, first[i + 1], 1.5f)
        }
    }

    @Test
    fun fadeIgnoresGapAndRampsOnlyTheLaterCell() {
        val looks = compute(CollageBlend.Feather, 100, 40f)
        assertNull(looks[0].polygon)
        assertTrue(!looks[0].ramps.any || looks[0].ramps.left == 0f)
        assertTrue(looks[1].ramps.left > 0f)
        assertTrue(looks[3].ramps.left > 0f && looks[3].ramps.top > 0f)
        assertEquals(0f, looks[0].bounds.left, 0.01f)
    }

    @Test
    fun zeroStrengthMakesCellsPlainRects() {
        compute(CollageBlend.Feather, 0).forEach { assertTrue(!it.ramps.any) }
        val circle = compute(CollageBlend.Squircle, 0)[0]
        val b = circle.bounds
        circle.polygon!!.toList().chunked(2).forEach { (x, y) ->
            assertTrue(x >= b.left - 0.01f && x <= b.right + 0.01f && y >= b.top - 0.01f && y <= b.bottom + 0.01f)
        }
    }

    @Test
    fun layoutsCoverTheCanvasWithoutOverlap() {
        CollageTemplates.all.filter { it.id.startsWith("p") }.forEach { t ->
            val area = t.cells.sumOf { (it.w * it.h).toDouble() }
            assertEquals(t.id, 1.0, area, 0.005)
        }
    }
}
