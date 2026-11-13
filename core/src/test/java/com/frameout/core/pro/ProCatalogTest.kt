package com.frameout.core.pro

import com.frameout.core.storage.ImageFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProCatalogTest {
    @Test
    fun everyoneKeepsTheCoreFormats() {
        listOf(ImageFormat.Jpg, ImageFormat.Png, ImageFormat.WebpLossy).forEach { assertTrue(it.key, ProCatalog.formatIsFree(it)) }
    }

    @Test
    fun advancedFormatsNeedPro() {
        listOf(ImageFormat.JpgHdr, ImageFormat.PngHdr16, ImageFormat.Heic, ImageFormat.Avif)
            .forEach { assertFalse(it.key, ProCatalog.formatIsFree(it)) }
    }

    @Test
    fun everyClassicFilterIsFree() {
        listOf("", "mono", "sepia", "vivid", "dramatic", "chrome").forEach { assertTrue(it, ProCatalog.filterIsFree(it)) }
    }

    @Test
    fun studioFiltersArePaidAndOrderedFreeThenAdThenPro() {
        val paid = com.frameout.core.edit.FilterPresets.All.map { it.id }.filter { !ProCatalog.filterIsFree(it) }
        assertEquals(23, paid.size)
        assertEquals(ProTier.Ad, ProCatalog.filterTier("kodak"))
        assertEquals(ProTier.Pro, ProCatalog.filterTier("duo_ocean"))
        val tiers = ProCatalog.orderedFilters(com.frameout.core.edit.FilterPresets.All.map { it.id }).map { ProCatalog.filterTier(it).ordinal }
        assertEquals(tiers.sorted(), tiers)
    }

    @Test
    fun onlyBasicShapesAreFreeAndTheListIsTiered() {
        assertEquals(6, ProCatalog.orderedShapes().count { ProCatalog.shapeTier(it) == ProTier.Free })
        assertEquals(6, ProCatalog.orderedShapes().count { ProCatalog.shapeTier(it) == ProTier.Ad })
        assertEquals(10, ProCatalog.orderedShapes().count { ProCatalog.shapeTier(it) == ProTier.Pro })
        val tiers = ProCatalog.orderedShapes().map { ProCatalog.shapeTier(it).ordinal }
        assertEquals(tiers.sorted(), tiers)
        assertEquals(ProTier.Free, ProCatalog.shapeTier(com.frameout.core.edit.ShapeKind.Arrow))
    }

    @Test
    fun coverStylesAreTiered() {
        assertEquals(ProTier.Free, ProCatalog.coverTier(com.frameout.core.edit.CoverStyle.Solid))
        assertEquals(ProTier.Ad, ProCatalog.coverTier(com.frameout.core.edit.CoverStyle.Blur))
        assertEquals(ProTier.Pro, ProCatalog.coverTier(com.frameout.core.edit.CoverStyle.Pixelate))
    }

    @Test
    fun freeCollagesAreTheThreeSimpleGrids() {
        assertEquals(setOf("2x1", "1x2", "2x2"), ProCatalog.freeCollages)
        assertFalse(ProCatalog.collageIsFree("3x3"))
    }

    @Test
    fun onlyBlackIsFreeByDefault() {
        assertTrue(ProCatalog.accentIsFree("black"))
        assertFalse(ProCatalog.accentIsFree("indigo"))
        assertFalse(ProCatalog.accentIsFree("ocean"))
        assertFalse(ProCatalog.accentIsFree("emerald"))
        assertFalse(ProCatalog.accentIsFree("rose"))
        assertFalse(ProCatalog.accentIsFree("dynamic"))
        assertTrue(ProCatalog.accentIsProOnly("gold"))
        assertFalse(ProCatalog.accentIsProOnly("black"))
        assertTrue(ProCatalog.accentIsProOnly("dynamic"))
        assertFalse(ProCatalog.accentIsProOnly("emerald"))
    }

    @Test
    fun grantedAccentIsFreeAndNothingElseIs() {
        assertTrue(ProCatalog.accentIsFree("gold", "gold"))
        assertTrue(ProCatalog.accentIsFree("black", "gold"))
        assertFalse(ProCatalog.accentIsFree("rose", "gold"))
        assertFalse(ProCatalog.accentIsFree("dynamic", "gold"))
        assertFalse(ProCatalog.accentIsFree("gold", null))
    }

    @Test
    fun welcomeCanPickAnyBrandColorButNotTheSystemOne() {
        assertTrue(ProCatalog.accentKeys.all(ProCatalog::welcomePickable))
        assertFalse(ProCatalog.welcomePickable("dynamic"))
        assertFalse(ProCatalog.welcomePickable("pink"))
        assertFalse(ProCatalog.welcomePickable(""))
    }

    @Test
    fun featureKeysAreUnique() {
        val keys = ProFeature.entries.map { it.key }
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun trialAndGraceLimitsAreSane() {
        assertEquals(0, ProCatalog.TRIALS_PER_FEATURE)
        assertTrue(ProCatalog.GRACE_DAYS in 1..60)
    }
}
