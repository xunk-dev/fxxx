package com.frameout.app

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.frameout.app.billing.ProGold
import com.frameout.designsystem.theme.ContrastGuard
import com.frameout.designsystem.theme.FrameAccents
import com.frameout.designsystem.theme.FrameColorScheme
import com.frameout.designsystem.theme.darkColorScheme
import com.frameout.designsystem.theme.lightColorScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BlackThemeContrastTest {
    private val black = FrameAccents.Black
    private val dark = darkColorScheme(amoled = false, accent = black)
    private val amoled = darkColorScheme(amoled = true, accent = black)
    private val light = lightColorScheme(black)

    private fun ratio(a: Color, b: Color) = ContrastGuard.ratio(a, b)

    private fun surfaces(s: FrameColorScheme) = listOf(s.background, s.surface, s.container, s.containerHigh)

    @Test
    fun darkModeIsGraphiteNotPureBlack() {
        assertTrue(dark.background != Color.Black)
        assertEquals(Color(0xFF121212), dark.background)
        assertEquals(Color.Black, amoled.background)
    }

    @Test
    fun darkSurfacesGetLighterWithElevation() {
        for (s in listOf(dark, amoled)) {
            val l = surfaces(s).map { it.luminance() }
            assertTrue(l.zipWithNext().all { (a, b) -> b > a })
        }
    }

    @Test
    fun darkTextMeetsAaOnEverySurface() {
        for (s in listOf(dark, amoled)) {
            for (bg in surfaces(s)) {
                assertTrue(ratio(s.onSurface, bg) >= ContrastGuard.TEXT)
                assertTrue(ratio(s.onSurfaceVariant, bg) >= ContrastGuard.TEXT)
            }
            assertTrue(ratio(s.primary, s.background) >= ContrastGuard.TEXT)
            assertTrue(ratio(s.primary, s.primaryContainer) >= ContrastGuard.TEXT)
            assertTrue(ratio(s.onPrimary, s.primary) >= ContrastGuard.TEXT)
        }
    }

    @Test
    fun darkTextIsOffWhiteNotPureWhite() {
        assertTrue(dark.onSurface != Color.White)
        assertTrue(amoled.onSurface != Color.White)
    }

    @Test
    fun darkOutlineIsVisibleAgainstCanvas() {
        for (s in listOf(dark, amoled)) assertTrue(ratio(s.outline, s.background) >= ContrastGuard.BOUNDARY)
    }

    @Test
    fun selectedChipFillIsDistinctFromContainer() {
        for (s in listOf(dark, amoled)) assertTrue(ratio(s.primaryContainer, s.container) >= 1.3f)
    }

    @Test
    fun darkAccentStopsReadAsIconsAndCarryWhiteWhenDeepened() {
        for (s in listOf(dark, amoled)) {
            for (c in listOf(s.accentA, s.accentB, s.accentC)) assertTrue(ratio(c, s.containerHigh) >= ContrastGuard.BOUNDARY)
            assertTrue(ratio(Color.White, lerp(s.accentA, Color.Black, 0.26f)) >= ContrastGuard.TEXT)
            assertTrue(ratio(Color.White, lerp(s.accentB, Color.Black, 0.36f)) >= ContrastGuard.TEXT)
        }
    }

    @Test
    fun lightModeAccentStopsAreVisibleOnWhite() {
        for (c in listOf(light.accentA, light.accentB, light.accentC)) {
            assertTrue(ratio(c, light.container) >= ContrastGuard.BOUNDARY)
            assertTrue(ratio(c, light.background) >= ContrastGuard.BOUNDARY)
        }
        assertTrue(ratio(Color.White, lerp(light.accentA, Color.Black, 0.26f)) >= ContrastGuard.TEXT)
    }

    @Test
    fun outlinesAreVisibleOnEverySurfaceTheySitOn() {
        for (s in listOf(dark, amoled)) for (bg in surfaces(s)) assertTrue(ratio(s.outline, bg) >= ContrastGuard.BOUNDARY)
    }

    @Test
    fun proBadgeUsesGoldWithReadableInkOnBlack() {
        val stops = listOf(ProGold.Light, Color(0xFFF3C262), ProGold.Deep)
        for (stop in stops) {
            assertTrue(ratio(ProGold.Ink, stop) >= ContrastGuard.TEXT)
            for (s in listOf(dark, amoled, light)) assertTrue(ratio(stop, s.background) >= 1.2f)
        }
        for (s in listOf(dark, amoled)) assertTrue(ratio(ProGold.Deep, s.containerHigh) >= ContrastGuard.BOUNDARY)
    }

    @Test
    fun darkBrandAndFabFillsKeepWhiteReadableAndStayVisible() {
        val stops = listOf(Color(0xFF707070), Color(0xFF6B6B6B), Color(0xFF666666))
        for (c in stops) {
            assertTrue(ratio(Color.White, c) >= ContrastGuard.TEXT)
            for (s in listOf(dark, amoled)) assertTrue(ratio(c, s.background) >= ContrastGuard.BOUNDARY)
        }
    }

    @Test
    fun blackSwatchPreviewLooksBlack() {
        assertTrue(black.previewB.luminance() < 0.1f)
        assertTrue(black.previewC.luminance() < 0.05f)
    }
}
