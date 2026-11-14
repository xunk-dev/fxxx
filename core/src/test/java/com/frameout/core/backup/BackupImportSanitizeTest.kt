package com.frameout.core.backup

import com.frameout.core.edit.MarkCodec
import com.frameout.core.edit.ShapeKind
import com.frameout.core.edit.ShapeMark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupImportSanitizeTest {
    @Test
    fun lockedFilterIsStrippedOnImport() {
        val backup = BackupEdit(
            rotation = 0, flipH = false, flipV = false, cropL = 0f, cropT = 0f, cropR = 1f, cropB = 1f,
            brightness = 0f, contrast = 1f, saturation = 1f, isDeleted = false,
            marks = "", filter = "studio_glow_not_a_free_or_ad_filter", filterStrength = 1f
        )
        val entity = backup.toEntity(frameId = 1L)
        assertEquals("", entity.filter)
    }

    @Test
    fun freeFilterSurvivesImport() {
        val backup = BackupEdit(0, false, false, 0f, 0f, 1f, 1f, 0f, 1f, 1f, false, marks = "", filter = "sepia", filterStrength = 1f)
        val entity = backup.toEntity(frameId = 1L)
        assertEquals("sepia", entity.filter)
    }

    @Test
    fun lockedShapeMarkIsStrippedOnImport() {
        val proShape = ShapeMark(kind = ShapeKind.Star, color = 0, width = 4f, x1 = 0f, y1 = 0f, x2 = 1f, y2 = 1f)
        val backup = BackupEdit(0, false, false, 0f, 0f, 1f, 1f, 0f, 1f, 1f, false, marks = MarkCodec.encode(listOf(proShape)), filter = "", filterStrength = 1f)
        val entity = backup.toEntity(frameId = 1L)
        assertTrue("locked shape must not survive import", MarkCodec.decode(entity.marks).isEmpty())
    }

    @Test
    fun freeShapeMarkSurvivesImport() {
        val freeShape = ShapeMark(kind = ShapeKind.Rect, color = 0, width = 4f, x1 = 0f, y1 = 0f, x2 = 1f, y2 = 1f)
        val backup = BackupEdit(0, false, false, 0f, 0f, 1f, 1f, 0f, 1f, 1f, false, marks = MarkCodec.encode(listOf(freeShape)), filter = "", filterStrength = 1f)
        val entity = backup.toEntity(frameId = 1L)
        assertFalse(MarkCodec.decode(entity.marks).isEmpty())
    }

    @Test
    fun historyIsSanitizedTheSameWay() {
        val backup = BackupEdit(0, false, false, 0f, 0f, 1f, 1f, 0f, 1f, 1f, false, marks = "", filter = "studio_glow_not_a_free_or_ad_filter", filterStrength = 1f)
        val history = backup.toHistory(frameId = 1L)
        assertEquals("", history.filter)
    }
}
