package com.frameout.app

import androidx.compose.ui.text.font.FontFamily
import com.frameout.core.util.AppLang
import com.frameout.designsystem.theme.FrameScript
import com.frameout.designsystem.theme.FrameFontFamily
import com.frameout.designsystem.theme.frameTypography
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameScriptTest {
    private val systemFontLanguages = setOf("cs", "pl", "ro", "tr", "ru", "uk", "vi", "th", "hi", "bn", "ja", "ko", "zh")
    private val tallLanguages = setOf("th", "hi", "bn", "vi")

    private fun supported() = AppLang.supported.map { FrameScript.normalize(it) }.toSet()

    @Test
    fun everySupportedLanguageHasExactlyOneFontChoice() {
        supported().forEach { assertTrue(it, FrameScript.usesPlex(it) != (it in systemFontLanguages)) }
        assertTrue(supported().containsAll(systemFontLanguages))
        assertTrue(supported().containsAll(FrameScript.plexLanguages))
    }

    @Test
    fun plexIsUsedOnlyWhereItHasTheGlyphs() {
        systemFontLanguages.forEach { assertSame(it, FontFamily.SansSerif, FrameScript.family(it)) }
        FrameScript.plexLanguages.forEach { assertSame(it, FrameFontFamily, FrameScript.family(it)) }
    }

    @Test
    fun rightToLeftLanguagesKeepTheBundledFont() {
        assertEquals(setOf("ar", "fa", "ckb", "ur"), FrameScript.rtlLanguages)
        FrameScript.rtlLanguages.forEach { assertTrue(it, FrameScript.usesPlex(it)) }
    }

    @Test
    fun legacyAndRegionalCodesNormalize() {
        assertEquals("id", FrameScript.normalize("in"))
        assertEquals("nb", FrameScript.normalize("no"))
        assertEquals("fil", FrameScript.normalize("tl"))
        assertEquals("zh", FrameScript.normalize("zh-TW"))
        assertEquals("pt", FrameScript.normalize("pt_BR"))
    }

    @Test
    fun tallScriptsGetMoreLineHeight() {
        val base = frameTypography("en")
        tallLanguages.forEach { lang ->
            val t = frameTypography(lang)
            assertTrue(lang, t.body.lineHeight.value > base.body.lineHeight.value)
            assertTrue(lang, t.label.lineHeight.value / t.label.fontSize.value >= 1.4f)
            assertTrue(lang, t.display.lineHeight.value / t.display.fontSize.value >= 1.3f)
            assertEquals(lang, base.mono.lineHeight, t.mono.lineHeight)
        }
    }

    @Test
    fun noLanguageGetsTightLineHeight() {
        supported().forEach { lang ->
            val t = frameTypography(lang)
            listOf(t.display, t.title, t.titleSmall, t.body, t.bodySmall, t.label).forEach {
                assertTrue(lang, it.lineHeight.value / it.fontSize.value >= 1.2f)
            }
        }
    }

    @Test
    fun latinAndArabicLanguagesKeepTheOriginalMetrics() {
        val base = frameTypography("en")
        listOf("ar", "fa", "de", "fr").forEach {
            assertEquals(it, base.body.lineHeight, frameTypography(it).body.lineHeight)
            assertFalse(it, FrameScript.isTall(it))
        }
    }
}
