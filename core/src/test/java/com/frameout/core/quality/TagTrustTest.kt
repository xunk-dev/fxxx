package com.frameout.core.quality

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TagTrustTest {
    private fun families(vararg hits: TagHit) = TagFamilies.withFamilies(hits.toList()).filter { TagFamilies.isFamily(it.tag) }.map { it.tag }.toSet()

    private fun kept(vararg hits: TagHit) = SceneReasoner.refine(hits.toList()).map { it.tag }.toSet()

    @Test
    fun weightsFollowTheTrustLevelAndIgnoreCase() {
        assertEquals(0f, TagTrust.weight(" Pattern "), 0f)
        assertEquals(TagTrust.AMBIGUOUS_WEIGHT, TagTrust.weight("WALL"), 0f)
        assertEquals(1f, TagTrust.weight("Car"), 0f)
        assertEquals(TagTrust.HOME_GOOD_WEIGHT, TagTrust.objectWeight("o:home_good"), 0f)
        assertEquals(1f, TagTrust.objectWeight("o:food"), 0f)
    }

    @Test
    fun abstractLabelNeverCreatesAFamily() {
        assertTrue(families(TagHit("l:Pattern", 0.95f), TagHit("l:Fun", 0.95f), TagHit("l:Event", 0.95f)).isEmpty())
    }

    @Test
    fun aLoneAmbiguousLabelIsNotEnoughButTwoAgreeingAre() {
        assertTrue(families(TagHit("l:Wheel", 0.9f)).isEmpty())
        assertEquals(setOf(TagFamilies.key("vehicles")), families(TagHit("l:Wheel", 0.9f), TagHit("l:Tire", 0.9f)))
    }

    @Test
    fun aConcreteLabelAloneCreatesItsFamily() {
        assertEquals(setOf(TagFamilies.key("vehicles")), families(TagHit("l:Car", 0.75f)))
        assertTrue(families(TagHit("l:Car", 0.65f)).isEmpty())
    }

    @Test
    fun homeGoodGuessNeedsMoreConfidenceToFormAFamily() {
        assertTrue(families(TagHit("o:home_good", 0.8f)).isEmpty())
        assertEquals(setOf(TagFamilies.key("home")), families(TagHit("o:home_good", 0.9f)))
    }

    @Test
    fun reasonerDropsAbstractLabelsEvenWhenConfident() {
        assertEquals(setOf("l:Sky"), kept(TagHit("l:Event", 0.97f), TagHit("l:Pattern", 0.99f), TagHit("l:Sky", 0.9f)))
    }

    @Test
    fun reasonerKeepsAmbiguousLabelsOnlyWhenVerySure() {
        assertFalse("l:Wall" in kept(TagHit("l:Wall", 0.8f)))
        assertTrue("l:Wall" in kept(TagHit("l:Wall", 0.9f)))
    }

    @Test
    fun reasonerNeedsStrongerEvidenceForTheCatchAllHomeGuess() {
        assertFalse("o:home_good" in kept(TagHit("o:home_good", 0.7f)))
        assertTrue("o:home_good" in kept(TagHit("o:home_good", 0.8f)))
        assertFalse("o:place" in kept(TagHit("o:place", 0.6f)))
    }
}
