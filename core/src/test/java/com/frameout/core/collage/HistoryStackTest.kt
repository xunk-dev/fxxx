package com.frameout.core.collage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryStackTest {
    @Test
    fun emptyStackCannotMove() {
        val h = HistoryStack<Int>()
        assertFalse(h.canUndo)
        assertFalse(h.canRedo)
        assertNull(h.undone(1))
        assertNull(h.redone(1))
    }

    @Test
    fun undoThenRedoRoundTrips() {
        var h = HistoryStack<Int>().commit(1).commit(2)
        val (a, afterUndo) = h.undone(3)!!
        assertEquals(2, a)
        h = afterUndo
        assertTrue(h.canRedo)
        val (b, afterRedo) = h.redone(a)!!
        assertEquals(3, b)
        assertEquals(listOf(1, 2), afterRedo.undo)
        assertFalse(afterRedo.canRedo)
    }

    @Test
    fun newEditClearsRedo() {
        val (_, h) = HistoryStack<Int>().commit(1).undone(2)!!
        assertTrue(h.canRedo)
        assertFalse(h.commit(5).canRedo)
    }

    @Test
    fun limitDropsOldest() {
        var h = HistoryStack<Int>(limit = 3)
        (1..6).forEach { h = h.commit(it) }
        assertEquals(listOf(4, 5, 6), h.undo)
    }

    @Test
    fun limitAppliesToRedoToo() {
        var h = HistoryStack(undo = (1..3).toList(), limit = 3)
        var current = 10
        repeat(3) { val (t, n) = h.undone(current)!!; current = t; h = n }
        assertEquals(3, h.redo.size)
        assertEquals(listOf(10, 3, 2), h.redo)
    }
}
