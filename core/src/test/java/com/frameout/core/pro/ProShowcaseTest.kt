package com.frameout.core.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test

class ProShowcaseTest {
    @Test
    fun everyShowcaseKindHasExactlyOneFeature() {
        val mapped = ProFeature.entries.mapNotNull { ProShowcase.kindOf(it) }
        assertEquals(ShowcaseKind.entries.toSet(), mapped.toSet())
        assertEquals(ShowcaseKind.entries.size, mapped.size)
    }

    @Test
    fun onlyGeneralHasNoShowcase() {
        assertNull(ProShowcase.kindOf(ProFeature.General))
        ProFeature.entries.filter { it != ProFeature.General }.forEach { assertTrue(it.key, ProShowcase.kindOf(it) != null) }
    }

    @Test
    fun parseSplitsTrimsAndDropsEmpty() {
        assertEquals(listOf("A", "B", "C"), ProShowcase.parse(" A | B||C |"))
    }

    @Test
    fun onlyShapesHideExtraItems() {
        assertEquals(8, ProShowcase.hiddenCount(ShowcaseKind.Shapes))
        ShowcaseKind.entries.filter { it != ShowcaseKind.Shapes }.forEach { assertEquals(0, ProShowcase.hiddenCount(it)) }
    }

    @Test
    fun proFilterListExcludesFreeFilters() {
        val ids = ProShowcase.proFilterIds()
        assertTrue(ids.isNotEmpty())
        ids.forEach { assertFalse(it, ProCatalog.filterIsFree(it)) }
        assertFalse(ids.contains(""))
    }
}
