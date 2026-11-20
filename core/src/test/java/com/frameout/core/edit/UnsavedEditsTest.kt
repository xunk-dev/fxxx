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

    @Test
    fun acknowledgedFramesStopPrompting() {
        UnsavedEdits.markAll(listOf(1L, 2L), true)
        assertEquals(listOf(1L, 2L), UnsavedEdits.pending())
        UnsavedEdits.acknowledge(listOf(1L))
        assertEquals(listOf(2L), UnsavedEdits.pending())
        assertEquals(listOf(1L, 2L), UnsavedEdits.snapshot())
    }

    @Test
    fun editingAgainAfterAcknowledgingPromptsAgain() {
        UnsavedEdits.mark(1L, true)
        UnsavedEdits.acknowledge(listOf(1L))
        val before = UnsavedEdits.version.value
        UnsavedEdits.mark(1L, true)
        assertEquals(listOf(1L), UnsavedEdits.pending())
        assertEquals(before + 1, UnsavedEdits.version.value)
    }

    @Test
    fun pendingSinceIgnoresOlderEdits() {
        UnsavedEdits.mark(1L, true)
        val start = UnsavedEdits.now()
        UnsavedEdits.mark(2L, true)
        assertEquals(listOf(2L), UnsavedEdits.pending(start))
        UnsavedEdits.mark(1L, true)
        assertEquals(setOf(1L, 2L), UnsavedEdits.pending(start).toSet())
    }

    @Test
    fun cleaningForgetsAcknowledgement() {
        UnsavedEdits.mark(1L, true)
        UnsavedEdits.acknowledge(listOf(1L))
        UnsavedEdits.clean(1L)
        UnsavedEdits.mark(1L, true)
        assertEquals(listOf(1L), UnsavedEdits.pending())
    }
}
