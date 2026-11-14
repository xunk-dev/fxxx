package com.frameout.app.viewer

import com.frameout.core.edit.FrameEdit
import com.frameout.core.quality.NormBox
import com.frameout.core.quality.TagHit
import com.frameout.core.quality.TagRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ViewerDetectionsTest {
    private fun box(i: Int) = NormBox(0.02f * i, 0.1f, 0.02f * i + 0.015f, 0.2f)

    @Test
    fun tagWithExactlyTheLegacyCapIsTreatedAsTruncated() {
        val capped = TagHit(TagRules.FACE, 0.9f, List(ViewerDetections.LEGACY_BOX_CAP) { box(it) })
        assertTrue(ViewerDetections.truncated(listOf(capped)))
    }

    @Test
    fun tagsBelowOrAboveTheLegacyCapAreComplete() {
        val few = TagHit(TagRules.FACE, 0.9f, List(3) { box(it) })
        val many = TagHit(TagRules.FACE, 0.9f, List(12) { box(it) })
        assertFalse(ViewerDetections.truncated(listOf(few, many)))
    }

    @Test
    fun liveHitsKeepTheirBoxesWithoutAnEdit() {
        val hits = listOf(TagHit("o:car", 0.8f, listOf(NormBox(0.2f, 0.2f, 0.6f, 0.6f))))
        val found = ViewerDetections.detectionsFrom(hits, emptyList(), null)
        assertEquals(1, found.size)
        assertEquals(NormBox(0.2f, 0.2f, 0.6f, 0.6f), found[0].box)
    }

    @Test
    fun editedFramesDropBoxesOutsideTheCrop() {
        val hits = listOf(TagHit("o:car", 0.8f, listOf(NormBox(0.0f, 0.0f, 0.2f, 0.2f))))
        val edit = FrameEdit(1, cropL = 0.5f, cropT = 0.5f, cropR = 1f, cropB = 1f)
        assertTrue(ViewerDetections.detectionsFrom(hits, emptyList(), edit).isEmpty())
    }

    @Test
    fun quietAndBoxlessTagsAreNotListed() {
        val hits = listOf(TagHit(TagRules.GROUP, 0.9f, listOf(box(1))), TagHit("o:cat", 0.9f, emptyList()))
        assertTrue(ViewerDetections.detectionsFrom(hits, emptyList(), null).isEmpty())
    }
}
