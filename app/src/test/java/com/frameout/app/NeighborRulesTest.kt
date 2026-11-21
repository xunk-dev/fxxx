package com.frameout.app

import com.frameout.app.trim.NeighborRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NeighborRulesTest {
    @Test
    fun windowIsFiveEitherSideAndClampedToTheClip() {
        assertEquals(95..105, NeighborRules.range(100, 1000))
        assertEquals(0..5, NeighborRules.range(0, 1000))
        assertEquals(994..999, NeighborRules.range(999, 1000))
        assertEquals(0..2, NeighborRules.range(1, 3))
    }

    @Test
    fun steppingInsideTheWindowKeepsIt() {
        assertTrue(NeighborRules.keepsWindow(103, 100, true))
        assertTrue(NeighborRules.keepsWindow(95, 100, true))
        assertFalse(NeighborRules.keepsWindow(106, 100, true))
        assertFalse(NeighborRules.keepsWindow(100, 100, false))
        assertFalse(NeighborRules.keepsWindow(100, -1, true))
    }

    @Test
    fun sharpnessIsRelativeToTheBestFrame() {
        assertEquals(100, NeighborRules.relativePercent(80.0, 80.0))
        assertEquals(50, NeighborRules.relativePercent(40.0, 80.0))
        assertEquals(0, NeighborRules.relativePercent(10.0, 0.0))
        assertTrue(NeighborRules.isBest(80.0, 80.0))
        assertFalse(NeighborRules.isBest(79.0, 80.0))
        assertFalse(NeighborRules.isBest(0.0, 0.0))
    }
}
