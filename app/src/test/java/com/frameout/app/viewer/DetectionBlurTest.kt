package com.frameout.app.viewer

import com.frameout.core.edit.CoverMark
import com.frameout.core.edit.CoverStyle
import com.frameout.core.edit.DEFAULT_COVER_STRENGTH
import com.frameout.core.edit.MarkSpace
import com.frameout.core.quality.NormBox
import com.frameout.core.quality.TagRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DetectionBlurTest {
    private val face = Detection(NormBox(0.4f, 0.4f, 0.6f, 0.6f), listOf(TagRules.FACE, TagRules.SMILE))
    private val thing = Detection(NormBox(0.4f, 0.4f, 0.6f, 0.6f), listOf("o:car"))

    @Test
    fun faceCoverIsWiderThanTheDetectionAndAlwaysBlur() {
        val cover = DetectionBlur.cover(face)
        assertEquals(CoverStyle.Blur, cover.style)
        assertTrue(cover.left < face.box.l && cover.top < face.box.t && cover.right > face.box.r && cover.bottom > face.box.b)
    }

    @Test
    fun privateContentGetsStrongerBlurThanObjects() {
        assertEquals(DetectionBlur.PRIVATE_STRENGTH, DetectionBlur.cover(face).strength, 0f)
        assertEquals(DEFAULT_COVER_STRENGTH, DetectionBlur.cover(thing).strength, 0f)
        assertTrue(DetectionBlur.isPrivate(Detection(face.box, listOf("t:text"))))
        assertTrue(DetectionBlur.isPrivate(Detection(face.box, listOf("b:url"))))
        assertFalse(DetectionBlur.isPrivate(thing))
    }

    @Test
    fun coverNeverLeavesTheImage() {
        val edge = Detection(NormBox(0f, 0f, 0.1f, 0.1f), listOf(TagRules.FACE))
        val cover = DetectionBlur.cover(edge)
        assertTrue(cover.left >= 0f && cover.top >= 0f && cover.right <= 1f && cover.bottom <= 1f)
    }

    @Test
    fun bodyBoxIsPaddedLessThanAFace() {
        val body = DetectionBlur.padding(listOf(TagRules.BODY))
        assertTrue(body < DetectionBlur.padding(listOf(TagRules.FACE)))
    }

    @Test
    fun sameDetectsTheSeededCoverAndIgnoresDistantOnes() {
        val seeded = DetectionBlur.cover(face)
        assertTrue(DetectionBlur.same(seeded, seeded.copy(left = seeded.left + 0.005f)))
        assertFalse(DetectionBlur.same(seeded, CoverMark(0.7f, 0.7f, 0.9f, 0.9f, CoverStyle.Blur)))
        assertFalse(DetectionBlur.same(seeded, CoverMark(0.45f, 0.45f, 0.5f, 0.5f, CoverStyle.Blur)))
    }

    @Test
    fun seededCoverSurvivesRotationAndCropRoundTrip() {
        val space = MarkSpace(1000, 600, rotation = 90, flipH = true, cropL = 0.1f, cropT = 0.05f, cropR = 0.9f, cropB = 0.95f)
        val cover = DetectionBlur.cover(face)
        val back = space.toOutput(space.toSource(cover)) as CoverMark
        assertTrue(DetectionBlur.same(cover, back))
        assertEquals(cover.left, back.left, 0.01f)
        assertEquals(cover.bottom, back.bottom, 0.01f)
    }
}
