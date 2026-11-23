package com.frameout.app

import com.frameout.app.trim.Handle
import com.frameout.app.trim.nextPick
import com.frameout.app.trim.pinnedTime
import com.frameout.engine.timeline.TrimRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JogPickTest {
    @Test
    fun tappingAHandleSelectsIt() {
        assertEquals(Handle.Start, nextPick(Handle.None, Handle.Start, true))
        assertEquals(Handle.End, nextPick(Handle.Start, Handle.End, true))
    }

    @Test
    fun tappingTheSelectedHandleAgainReleasesIt() {
        assertEquals(Handle.None, nextPick(Handle.End, Handle.End, true))
    }

    @Test
    fun draggingASelectedHandleKeepsItSelected() {
        assertEquals(Handle.Start, nextPick(Handle.Start, Handle.Start, false))
    }

    @Test
    fun touchingAnythingElseReleasesTheSelection() {
        assertEquals(Handle.None, nextPick(Handle.Start, Handle.None, true))
        assertEquals(Handle.None, nextPick(Handle.End, Handle.None, false))
    }

    @Test
    fun pinnedTimeFollowsTheSelectedHandle() {
        val range = TrimRange(2_000, 9_000, false)
        assertEquals(2_000L, pinnedTime(Handle.Start, range))
        assertEquals(9_000L, pinnedTime(Handle.End, range))
        assertNull(pinnedTime(Handle.None, range))
    }
}
