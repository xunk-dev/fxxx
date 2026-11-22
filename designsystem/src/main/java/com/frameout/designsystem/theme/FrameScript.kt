package com.frameout.designsystem.theme

import androidx.compose.ui.text.font.FontFamily
import java.util.Locale

object FrameScript {
    val plexLanguages: Set<String> = setOf("ar", "ckb", "fa", "ur", "en", "de", "es", "fr", "id", "it", "ms", "nb", "nl", "pt", "sv", "fil")

    private val lineScale: Map<String, Float> = mapOf(
        "th" to 1.25f, "hi" to 1.2f, "bn" to 1.2f, "vi" to 1.15f,
        "ja" to 1.1f, "ko" to 1.1f, "zh" to 1.1f
    )

    val rtlLanguages: Set<String> = setOf("ar", "fa", "ckb", "ur")

    fun normalize(language: String): String {
        val code = language.lowercase(Locale.ROOT).substringBefore('-').substringBefore('_')
        return when (code) {
            "in" -> "id"
            "no", "nn" -> "nb"
            "tl" -> "fil"
            else -> code
        }
    }

    fun usesPlex(language: String): Boolean = normalize(language) in plexLanguages

    fun lineHeightScale(language: String): Float = lineScale[normalize(language)] ?: 1f

    fun isTall(language: String): Boolean = lineHeightScale(language) > 1f

    fun family(language: String): FontFamily = if (usesPlex(language)) FrameFontFamily else FontFamily.SansSerif
}
