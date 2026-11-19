package com.frameout.core.edit

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UnsavedEditsTest {
    @After
    fun reset() = UnsavedEdits.clear()

    @Test
    fun marksAndCleansFrames() {
        UnsavedEdits.mark(1L, true)
        UnsavedEdits.markAll(listOf(2L, 3L), true)
        assertEquals(listOf(1L, 2L, 3L), UnsavedEdits.snapshot())
        UnsavedEdits.clean(2L)
        assertFalse(UnsavedEdits.isUnsaved(2L))
        assertTrue(UnsavedEdits.isUnsaved(3L))
    }

    @Test
    fun versionOnlyTicksWhenTheSetChanges() {
        val start = UnsavedEdits.version.value
        UnsavedEdits.clean(9L)
        assertEquals(start, UnsavedEdits.version.value)
        UnsavedEdits.mark(9L, true)
        UnsavedEdits.mark(9L, true)
        assertEquals(start + 1, UnsavedEdits.version.value)
    }
}
