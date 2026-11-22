package com.frameout.app

import com.frameout.app.trim.SuggestedMoments
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestedMomentsTest {
    @Test
    fun picksTheSharpestSlotsAndKeepsThemApart() {
        val scores = listOf(1.0, 9.0, 8.5, 2.0, 1.0, 7.0, 1.0, 1.0)
        assertEquals(listOf(1, 5), SuggestedMoments.pick(scores, 2, 2))
    }

    @Test
    fun skipsEmptyAndFlatSlots() {
        assertEquals(listOf(2), SuggestedMoments.pick(listOf(null, 0.0, 4.0, null), 3, 1))
        assertTrue(SuggestedMoments.pick(emptyList(), 3, 1).isEmpty())
    }

    @Test
    fun neverReturnsMoreThanAsked() {
        val scores = List(40) { (it % 7).toDouble() + 1.0 }
        assertTrue(SuggestedMoments.pick(scores, 4, 2).size <= 4)
    }

    @Test
    fun waitsUntilMostThumbnailsHaveLoaded() {
        assertFalse(SuggestedMoments.ready(10, 40))
        assertTrue(SuggestedMoments.ready(40, 40))
        assertFalse(SuggestedMoments.ready(3, 3))
    }

    @Test
    fun slotTimeIsTheMiddleOfTheSlot() {
        assertEquals(5_000L, SuggestedMoments.timeOf(0, 10, 100_000L) - 0L)
        assertEquals(95_000L, SuggestedMoments.timeOf(9, 10, 100_000L))
    }
}
