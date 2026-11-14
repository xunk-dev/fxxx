package com.frameout.core.storage

import org.junit.Assert.assertEquals
import org.junit.Test

class AppFoldersTest {
    @Test
    fun allRootsShareOneName() {
        assertEquals("Pictures/FrameOut", AppFolders.PICTURES)
        assertEquals("Movies/FrameOut", AppFolders.MOVIES)
        assertEquals("Documents/FrameOut", AppFolders.DOCUMENTS)
    }

    @Test
    fun userLanguageSubfolderIsKept() {
        assertEquals("فيلم جديد", SaveLocation.folderName("فيلم جديد"))
        assertEquals("Documents/FrameOut/فيلم جديد", SaveLocation.join(AppFolders.DOCUMENTS, "فيلم جديد"))
    }
}
