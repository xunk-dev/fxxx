package com.frameout.app.viewer

import com.frameout.core.edit.CoverMark
import com.frameout.core.edit.CoverStyle
import com.frameout.core.edit.DEFAULT_COVER_STRENGTH
import com.frameout.core.quality.TagGroup
import com.frameout.core.quality.TagNames
import com.frameout.core.quality.TagRules
import kotlin.math.max
import kotlin.math.min

internal object DetectionBlur {
    const val PRIVATE_STRENGTH = 0.92f
    private const val FACE_PAD = 0.18f
    private const val BODY_PAD = 0.04f
    private const val TEXT_PAD = 0.1f
    private const val CODE_PAD = 0.06f
    private const val OBJECT_PAD = 0.03f
    private const val SAME_IOU = 0.5f

    private val PRIVATE_GROUPS = setOf(TagGroup.People, TagGroup.Codes, TagGroup.Text)
    private val FACE_TAGS = setOf(TagRules.FACE, TagRules.SMILE, TagRules.EYES_CLOSED, TagRules.GOOD_FACE, TagRules.GROUP)

    fun isPrivate(detection: Detection): Boolean = detection.tags.any { TagNames.group(it) in PRIVATE_GROUPS }

    fun padding(tags: List<String>): Float = when {
        tags.any { it in FACE_TAGS } -> FACE_PAD
        tags.any { it == TagRules.BODY } -> BODY_PAD
        tags.any { TagNames.group(it) == TagGroup.Text } -> TEXT_PAD
        tags.any { TagNames.group(it) == TagGroup.Codes } -> CODE_PAD
        else -> OBJECT_PAD
    }

    fun cover(detection: Detection): CoverMark {
        val box = detection.box.padded(padding(detection.tags))
        val strength = if (isPrivate(detection)) PRIVATE_STRENGTH else DEFAULT_COVER_STRENGTH
        return CoverMark(box.l, box.t, box.r, box.b, CoverStyle.Blur, strength)
    }

    fun same(a: CoverMark, b: CoverMark): Boolean {
        val w = min(a.right, b.right) - max(a.left, b.left)
        val h = min(a.bottom, b.bottom) - max(a.top, b.top)
        if (w <= 0f || h <= 0f) return false
        val inter = w * h
        val union = area(a) + area(b) - inter
        return union > 0f && inter / union >= SAME_IOU
    }

    private fun area(mark: CoverMark): Float = (mark.right - mark.left) * (mark.bottom - mark.top)
}
