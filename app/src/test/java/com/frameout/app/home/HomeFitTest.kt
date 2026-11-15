package com.frameout.app.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeFitTest {
    @Test
    fun noAdKeepsOriginalHeights() {
        val fit = fitHomeHeights(172f, 178f, 96f, 300f, 0f)
        assertEquals(172f, fit.pro, 0f)
        assertEquals(178f, fit.tall, 0f)
        assertEquals(96f, fit.short, 0f)
    }

    @Test
    fun roomyScreenKeepsOriginalHeights() {
        val fit = fitHomeHeights(172f, 178f, 96f, 2000f, 86f)
        assertEquals(96f, fit.short, 0f)
        assertEquals(178f, fit.tall, 0f)
    }

    @Test
    fun tightScreenShrinksCompareFirstThenTiles() {
        val fit = fitHomeHeights(172f, 178f, 96f, 738f, 86f)
        assertEquals(66f, fit.short, 0f)
        assertTrue(fit.tall < 178f)
        assertEquals(172f, fit.pro, 0f)
        val total = fit.pro + 2f * fit.tall + fit.short + 16f + 24f + 24f + 24f + 86f
        assertTrue(total <= 738.01f)
    }

    @Test
    fun neverShrinksBelowMinimums() {
        val fit = fitHomeHeights(172f, 178f, 96f, 100f, 86f)
        assertEquals(66f, fit.short, 0f)
        assertEquals(144f, fit.tall, 0f)
        assertEquals(140f, fit.pro, 0f)
    }
}
