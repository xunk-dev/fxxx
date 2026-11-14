package com.frameout.core.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageSearchEnginesTest {
    @Test
    fun googleLensIsTheOnlyEngine() {
        assertEquals(listOf(ImageSearchEngines.LENS_ID), ImageSearchEngines.all.map { it.id })
        assertEquals(ImageSearchEngines.LENS_ID, ImageSearchEngines.lens.id)
        assertNull(ImageSearchEngines.byId("yandex_images"))
        assertNull(ImageSearchEngines.byId("other_apps"))
    }

    @Test
    fun lensUsesHttpsAndSupportsShare() {
        val lens = ImageSearchEngines.lens
        assertTrue(lens.url.startsWith("https://"))
        assertTrue(lens.supportsShare)
        assertTrue("com.google.ar.lens" in lens.packages)
    }

    @Test
    fun descriptionsAreLocalised() {
        assertTrue(ImageSearchEngines.lens.desc("ar") != ImageSearchEngines.lens.desc("en"))
    }
}
