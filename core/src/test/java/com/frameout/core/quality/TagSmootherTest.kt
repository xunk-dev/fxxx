package com.frameout.core.quality

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TagSmootherTest {
    private fun frames(count: Int, extra: Map<Int, Map<String, Float>> = emptyMap()): List<Map<String, Float>> =
        List(count) { i -> mapOf("l:Tree" to 0.75f) + (extra[i] ?: emptyMap()) }

    @Test
    fun isolatedLowConfidenceTagIsDropped() {
        val weak = TagSmoother.weakTags(frames(9, mapOf(4 to mapOf("l:Bicycle" to 0.75f))))
        assertEquals(setOf("l:Bicycle"), weak[4])
        assertTrue(weak.filterIndexed { i, _ -> i != 4 }.all { it.isEmpty() })
    }

    @Test
    fun confidentTagSurvivesEvenWhenAlone() {
        assertTrue(TagSmoother.weakTags(frames(9, mapOf(4 to mapOf("l:Bicycle" to 0.9f)))).all { it.isEmpty() })
    }

    @Test
    fun tagSeenInEnoughNeighboursSurvives() {
        val extra = mapOf(3 to mapOf("l:Dog" to 0.7f), 5 to mapOf("l:Dog" to 0.7f), 4 to mapOf("l:Dog" to 0.7f))
        assertFalse("l:Dog" in TagSmoother.weakTags(frames(9, extra))[4])
    }

    @Test
    fun tooFewFramesAreLeftAlone() {
        assertTrue(TagSmoother.weakTags(frames(3, mapOf(1 to mapOf("l:Bicycle" to 0.7f)))).all { it.isEmpty() })
    }

    @Test
    fun peopleTextAndCodeTagsAreNotSmoothed() {
        assertFalse(TagSmoother.smoothable(TagRules.FACE))
        assertFalse(TagSmoother.smoothable(TagRules.TEXT))
        assertTrue(TagSmoother.smoothable("l:Car"))
        assertTrue(TagSmoother.smoothable("o:food"))
        assertTrue(TagSmoother.smoothable(TagFamilies.key("vehicles")))
    }
}
