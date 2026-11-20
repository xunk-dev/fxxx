package com.frameout.app

import com.frameout.core.db.FrameEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IncomingNoticeTest {
    @Test
    fun copyFailureWinsOverRejection() {
        val result = IncomingMedia.Stabilized(emptyList(), failed = 1, rejected = 2)
        assertEquals(R.string.incoming_copy_failed, result.noticeRes())
    }

    @Test
    fun rejectionAloneGetsItsOwnNotice() {
        assertEquals(R.string.incoming_unsupported, IncomingMedia.Stabilized(emptyList(), failed = 0, rejected = 1).noticeRes())
    }

    @Test
    fun cleanResultHasNoNotice() {
        assertNull(IncomingMedia.Stabilized(emptyList(), failed = 0).noticeRes())
    }

    private fun frames(count: Int) = List(count) { FrameEntity(id = it + 1L, projectId = 1L, index = it, ptsUs = 0L) }

    @Test
    fun singlePhotoOpensDirectly() {
        assertFalse(IncomingMedia.Stabilized(emptyList(), failed = 0, stills = frames(1)).needsChoice())
    }

    @Test
    fun severalPhotosAskWhatToDo() {
        assertTrue(IncomingMedia.Stabilized(emptyList(), failed = 0, stills = frames(2)).needsChoice())
    }

    @Test
    fun nothingSharedNeedsNoChoice() {
        assertFalse(IncomingMedia.Stabilized(emptyList(), failed = 0).needsChoice())
    }

    @Test
    fun noLaterNoticeWithoutVideos() {
        assertNull(IncomingMedia.Stabilized(emptyList(), failed = 0, stills = frames(3)).laterRes())
    }

    @Test
    fun choicesMatchTheSharedKind() {
        assertEquals(4, ShareChoice.Images.size)
        assertEquals(listOf(ShareChoice.Batch, ShareChoice.ExportVideo), ShareChoice.Videos)
    }
}
