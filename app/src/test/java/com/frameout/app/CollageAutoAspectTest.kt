package com.frameout.app

import com.frameout.core.collage.CollageAspect
import com.frameout.core.collage.CollageAutoAspect
import com.frameout.core.collage.CollageTemplates
import org.junit.Assert.assertEquals
import org.junit.Test

class CollageAutoAspectTest {
    private val grid = CollageTemplates.all.first { it.id == "2x2" }

    @Test
    fun squareImagesKeepSquareCanvas() {
        assertEquals(CollageAspect.Template, CollageAutoAspect.choose(grid, List(4) { 1f }))
    }

    @Test
    fun wideImagesInGridChooseWideCanvas() {
        assertEquals(CollageAspect.Wide, CollageAutoAspect.choose(grid, List(4) { 16f / 9f }))
    }

    @Test
    fun tallImagesInGridChooseStoryCanvas() {
        assertEquals(CollageAspect.Story, CollageAutoAspect.choose(grid, List(4) { 9f / 16f }))
    }

    @Test
    fun noImagesFallBackToTemplate() {
        assertEquals(CollageAspect.Template, CollageAutoAspect.choose(grid, emptyList()))
    }
}
