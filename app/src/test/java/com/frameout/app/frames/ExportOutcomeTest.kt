package com.frameout.app.frames

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportOutcomeTest {
    @Test
    fun declinedHasNothingToShow() {
        val outcome = ExportOutcome.declined()
        assertTrue(outcome.message.isBlank())
        assertNull(outcome.saved)
        assertNull(outcome.note)
    }

    @Test
    fun notesAreJoinedInOrderAndBlankOnesDropped() {
        assertEquals("a · b", ExportOutcome.joinNotes("a", null, " ", "b"))
        assertEquals("only", ExportOutcome.joinNotes(null, "only"))
        assertNull(ExportOutcome.joinNotes(null, "", "  "))
        assertNull(ExportOutcome.joinNotes())
    }
}
