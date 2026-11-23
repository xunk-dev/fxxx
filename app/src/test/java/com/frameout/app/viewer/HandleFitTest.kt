package com.frameout.app.viewer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HandleFitTest {
    private val d = 3f

    @Test fun radiusShrinksWithTarget() {
        val big = HandleFit.radius(400f, 9f * d, d)
        val small = HandleFit.radius(40f, 9f * d, d)
        assertEquals(9f * d, big, 0.001f)
        assertTrue(small < big)
        assertTrue(small >= 4f * d)
    }

    @Test fun reachNeverBelowFingerTarget() {
        assertEquals(14f * d, HandleFit.reach(10f, 28f * d, d), 0.001f)
        assertEquals(28f * d, HandleFit.reach(1000f, 28f * d, d), 0.001f)
    }

    @Test fun centreOfSmallBoxIsNotAHandle() {
        val centre = Offset(50f, 50f)
        val corner = Offset(40f, 40f)
        assertFalse(HandleFit.grabs(centre, corner, centre, 42f, roomy = false))
        assertTrue(HandleFit.grabs(Offset(30f, 30f), corner, centre, 42f, roomy = false))
        assertTrue(HandleFit.grabs(centre, corner, centre, 42f, roomy = true))
    }

    @Test fun tinyCropCentreMovesButOutsideStillResizes() {
        val img = Rect(Offset.Zero, Size(1000f, 1000f))
        val box = CropBox(0.50f, 0.50f, 0.56f, 0.56f) // 60px square
        val c = box.toRect(img)
        assertEquals(CropHandle.Move, hitCrop(c.center, img, box, 90f, 72f))
        val outside = Offset(c.right + 20f, c.bottom + 20f)
        assertEquals(CropHandle.BottomRight, hitCrop(outside, img, box, 90f, 72f))
    }
}
