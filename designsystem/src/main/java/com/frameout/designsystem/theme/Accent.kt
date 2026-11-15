package com.frameout.designsystem.theme

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Immutable
data class FrameAccent(
    val key: String,
    val a: Color,
    val b: Color,
    val c: Color,
    val darkPrimary: Color,
    val darkOnPrimary: Color,
    val darkContainer: Color,
    val lightPrimary: Color,
    val lightOnPrimary: Color,
    val lightContainer: Color,
    val surfaceTint: Float,
    val neutral: Boolean = false,
    val lightA: Color = a,
    val lightB: Color = b,
    val lightC: Color = c,
    val previewA: Color = a,
    val previewB: Color = b,
    val previewC: Color = c
)

private fun palette(
    key: String,
    stops: LongArray,
    dark: LongArray,
    light: LongArray,
    tint: Float = 0.22f,
    neutral: Boolean = false,
    lightStops: LongArray = stops,
    preview: LongArray = stops
) = FrameAccent(
    key = key,
    a = Color(stops[0]),
    b = Color(stops[1]),
    c = Color(stops[2]),
    darkPrimary = Color(dark[0]),
    darkOnPrimary = Color(dark[1]),
    darkContainer = Color(dark[2]),
    lightPrimary = Color(light[0]),
    lightOnPrimary = Color(light[1]),
    lightContainer = Color(light[2]),
    surfaceTint = tint,
    neutral = neutral,
    lightA = Color(lightStops[0]),
    lightB = Color(lightStops[1]),
    lightC = Color(lightStops[2]),
    previewA = Color(preview[0]),
    previewB = Color(preview[1]),
    previewC = Color(preview[2])
)

object FrameAccents {
    const val DYNAMIC = "dynamic"

    val Indigo = palette(
        key = "indigo",
        stops = longArrayOf(0xFF6366F1, 0xFF8B5CF6, 0xFF06B6D4),
        dark = longArrayOf(0xFF9A9DFF, 0xFF0B0A5C, 0xFF2A2C7A),
        light = longArrayOf(0xFF4347C9, 0xFFF7F7FF, 0xFFD5D6F8),
        tint = 0.22f
    )
    val Ocean = palette(
        key = "ocean",
        stops = longArrayOf(0xFF0EA5E9, 0xFF3B82F6, 0xFF22D3EE),
        dark = longArrayOf(0xFF7CC4FF, 0xFF002A47, 0xFF0B3A66),
        light = longArrayOf(0xFF0369A1, 0xFFF7FAFF, 0xFFC9E1F7)
    )
    val Emerald = palette(
        key = "emerald",
        stops = longArrayOf(0xFF10B981, 0xFF14B8A6, 0xFF84CC16),
        dark = longArrayOf(0xFF52E0C4, 0xFF00382E, 0xFF0B4A3F),
        light = longArrayOf(0xFF0D6D65, 0xFFF6FFFC, 0xFFBFE8DD)
    )
    val Sunset = palette(
        key = "sunset",
        stops = longArrayOf(0xFFF97316, 0xFFEF4444, 0xFFEC4899),
        dark = longArrayOf(0xFFFFA07A, 0xFF4A1A00, 0xFF5A2208),
        light = longArrayOf(0xFFB43A0A, 0xFFFFFAF7, 0xFFF6D6C3)
    )
    val Rose = palette(
        key = "rose",
        stops = longArrayOf(0xFFEC4899, 0xFFF43F5E, 0xFFA855F7),
        dark = longArrayOf(0xFFFF8FB1, 0xFF4D0A24, 0xFF6B1A3A),
        light = longArrayOf(0xFFBE185D, 0xFFFFF8FB, 0xFFF6CCDC)
    )
    val Gold = palette(
        key = "gold",
        stops = longArrayOf(0xFFF59E0B, 0xFFF97316, 0xFFFACC15),
        dark = longArrayOf(0xFFFFD27A, 0xFF3D2A00, 0xFF5C4100),
        light = longArrayOf(0xFF92400E, 0xFFFFFCF4, 0xFFF3DDAA)
    )

    val Crimson = palette(
        key = "crimson",
        stops = longArrayOf(0xFFEF4444, 0xFFBE123C, 0xFFFB7185),
        dark = longArrayOf(0xFFFF8A8A, 0xFF4A0709, 0xFF6B1216),
        light = longArrayOf(0xFFB91C1C, 0xFFFFF8F8, 0xFFF6CFCF)
    )
    val Lime = palette(
        key = "lime",
        stops = longArrayOf(0xFF84CC16, 0xFFA3E635, 0xFF22C55E),
        dark = longArrayOf(0xFFC2F15A, 0xFF1F3300, 0xFF2F4A06),
        light = longArrayOf(0xFF3F6212, 0xFFFBFFF4, 0xFFDCEDBA)
    )
    val Violet = palette(
        key = "violet",
        stops = longArrayOf(0xFFA855F7, 0xFFD946EF, 0xFF8B5CF6),
        dark = longArrayOf(0xFFD0A8FF, 0xFF2A0F5C, 0xFF42207A),
        light = longArrayOf(0xFF7E22CE, 0xFFFCF8FF, 0xFFE6D5FA)
    )
    val Mocha = palette(
        key = "mocha",
        stops = longArrayOf(0xFFA47551, 0xFF8B5E3C, 0xFFD4A373),
        dark = longArrayOf(0xFFE0B48F, 0xFF3A2010, 0xFF55341D),
        light = longArrayOf(0xFF7A4A28, 0xFFFFFAF5, 0xFFEBD5C0)
    )

    val Black = palette(
        key = "black",
        stops = longArrayOf(0xFF9A9A9A, 0xFF8C8C8C, 0xFFBDBDBD),
        dark = longArrayOf(0xFFF2F2F2, 0xFF0F0F0F, 0xFF3E3E3E),
        light = longArrayOf(0xFF141414, 0xFFF7F7F7, 0xFFDADADA),
        neutral = true,
        lightStops = longArrayOf(0xFF2B2B2B, 0xFF4A4A4A, 0xFF6B6B6B),
        preview = longArrayOf(0xFF4A4A4A, 0xFF1F1F1F, 0xFF050505)
    )

    val All: List<FrameAccent> = listOf(Indigo, Ocean, Black, Emerald, Sunset, Crimson, Lime, Rose, Gold, Violet, Mocha)

    fun byKey(key: String): FrameAccent = All.firstOrNull { it.key == key } ?: Indigo

    fun dynamicSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
}

@RequiresApi(Build.VERSION_CODES.S)
private fun dynamicAccent(context: Context): FrameAccent {
    fun sys(id: Int) = Color(context.getColor(id))
    return FrameAccent(
        key = FrameAccents.DYNAMIC,
        a = sys(android.R.color.system_accent1_500),
        b = sys(android.R.color.system_accent2_400),
        c = sys(android.R.color.system_accent3_400),
        darkPrimary = sys(android.R.color.system_accent1_200),
        darkOnPrimary = sys(android.R.color.system_accent1_800),
        darkContainer = sys(android.R.color.system_accent1_700),
        lightPrimary = sys(android.R.color.system_accent1_600),
        lightOnPrimary = sys(android.R.color.system_accent1_0),
        lightContainer = sys(android.R.color.system_accent1_100),
        surfaceTint = 0.22f
    )
}

fun resolveFrameAccent(key: String, context: Context): FrameAccent {
    if (key == FrameAccents.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        return dynamicAccent(context)
    }
    return FrameAccents.byKey(key)
}

@Composable
fun rememberFrameAccent(key: String): FrameAccent {
    val context = LocalContext.current
    return remember(key) { resolveFrameAccent(key, context) }
}
