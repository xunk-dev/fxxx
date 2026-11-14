package com.frameout.core.quality

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FaceOutlineTest {
    @Test
    fun cropPointsLandOnTheirOwnPlaceInTheSource() {
        val p = FaceOutline.cropToSource(0.5f, 0.5f, 600, 300, 400, 200, 2000, 1000)
        assertEquals(0.4f, p.x, 0.0001f)
        assertEquals(0.4f, p.y, 0.0001f)
    }

    @Test
    fun cropOriginMapsToCropCornerNotToZero() {
        val p = FaceOutline.cropToSource(0f, 0f, 600, 300, 400, 200, 2000, 1000)
        assertEquals(0.3f, p.x, 0.0001f)
        assertEquals(0.3f, p.y, 0.0001f)
    }

    @Test
    fun targetIsExpressedInsideTheCrop() {
        val t = FaceOutline.cropTarget(NormBox(0.4f, 0.4f, 0.5f, 0.5f), 600, 300, 400, 200, 2000, 1000)
        assertEquals(0.5f, t.l, 0.0001f)
        assertEquals(0.5f, t.t, 0.0001f)
        assertEquals(0.9f, t.r, 0.0001f)
        assertEquals(1f, t.b, 0.0001f)
    }

    @Test
    fun pickPrefersTheFaceOnTheTargetOverTheBiggerNeighbour() {
        val target = NormBox(0.1f, 0.3f, 0.3f, 0.6f)
        val mine = NormBox(0.12f, 0.32f, 0.28f, 0.58f)
        val bigger = NormBox(0.5f, 0.1f, 0.95f, 0.9f)
        assertEquals("mine", FaceOutline.pick(listOf("bigger" to bigger, "mine" to mine), target))
    }

    @Test
    fun pickReturnsNothingWhenNoFaceIsOnTheTarget() {
        val target = NormBox(0.1f, 0.1f, 0.2f, 0.2f)
        assertNull(FaceOutline.pick(listOf("far" to NormBox(0.6f, 0.6f, 0.9f, 0.9f)), target))
    }
}
