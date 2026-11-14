package com.frameout.app.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.frameout.designsystem.theme.ContrastGuard
import com.frameout.designsystem.theme.FrameColorScheme
import com.frameout.designsystem.theme.FrameTheme

@Immutable
internal class HomeLook(
    val dark: Boolean,
    val neutral: Boolean,
    val title: Color,
    val desc: Color,
    val grabber: Color,
    val pro: ProLook,
    val tones: HomeTones
)

@Immutable
internal class ProLook(
    val stops: List<Color>,
    val ink: Color,
    val inkSoft: Color,
    val pillBg: Color,
    val pillInk: Color,
    val edgeTop: Color,
    val edgeBottom: Color,
    val glow: Color,
    val shadow: Color,
    val jewel: Boolean,
    val bloom: Color,
    val vignette: Color,
    val iconRamp: IconRamp,
    val accentGlow: Color
)

@Immutable
internal class HomeTones(
    val images: Color,
    val video: Color,
    val compare: Color,
    val collage: Color,
    val edit: Color,
    val pro: Color
)

@Composable
internal fun rememberHomeLook(): HomeLook {
    val c = FrameTheme.colors
    return remember(c) { buildHomeLook(c) }
}

private class Ramp(val stops: List<Color>, val ink: Color)

private const val DARK_DEEPEN = 0.40f
private const val SOFT_INK = 0.86f
private const val PRO_CONTRAST = 5.2f
private const val TONE_L_DARK = 0.74f
private const val TONE_L_LIGHT = 0.62f
private const val TONE_CHROMA_FLOOR = 0.12f
private const val TONE_CHROMA_CEIL = 0.20f
private const val TONE_HUE_SPREAD = 12f

private fun jewelRamp(c: FrameColorScheme): Ramp {
    val a = c.accentA.toOklch()
    val b = c.accentB.toOklch()
    val d = c.accentC.toOklch()
    val chroma = a.c.coerceIn(0.11f, 0.19f)
    val drift = (hueDelta(a.h, d.h) * 0.35f).coerceIn(-14f, 14f)
    val deepHue = a.h + (hueDelta(a.h, b.h) * 0.4f).coerceIn(-12f, 12f)
    val ink = Color.White
    val light = ContrastGuard.ensure(oklch(0.47f, chroma * 0.95f, a.h + drift), listOf(ink), PRO_CONTRAST, false)
    val mid = ContrastGuard.ensure(oklch(0.39f, chroma * 0.98f, a.h), listOf(ink), PRO_CONTRAST, false)
    val deep = ContrastGuard.ensure(oklch(0.31f, chroma * 0.85f, deepHue), listOf(ink), PRO_CONTRAST, false)
    return Ramp(listOf(light, mid, deep), ink)
}

private fun lightRamp(c: FrameColorScheme): Ramp {
    val a = c.accentA.toOklch()
    val b = c.accentB.toOklch()
    val d = c.accentC.toOklch()
    val chroma = a.c.coerceIn(0.10f, 0.17f)
    val drift = (hueDelta(a.h, d.h) * 0.25f).coerceIn(-10f, 10f)
    val deepHue = a.h + (hueDelta(a.h, b.h) * 0.3f).coerceIn(-10f, 10f)
    val ink = oklch(0.20f, 0.04f, a.h)
    val raw = listOf(oklch(0.82f, chroma * 0.85f, a.h + drift), oklch(0.74f, chroma, a.h), oklch(0.66f, chroma * 0.95f, deepHue))
    return Ramp(raw.map { ContrastGuard.ensure(it, listOf(ink), PRO_CONTRAST, true) }, ink)
}

private fun accentRamp(c: FrameColorScheme): Ramp {
    if (c.isDark && !c.neutral) return jewelRamp(c)
    if (!c.isDark && !c.neutral) return lightRamp(c)
    val sorted = listOf(c.accentA, c.accentB, c.accentC).sortedByDescending { it.luminance() }
    val light = sorted[0]
    val mid = sorted[1]
    val deep = sorted[2]
    val graphiteLight = c.neutral && !c.isDark
    val bright = !graphiteLight && !c.isDark && light.luminance() > 0.28f
    val ink = if (bright) lerp(deep, Color.Black, 0.82f) else Color.White
    val raw = when {
        graphiteLight -> listOf(Color(0xFF3C3C3C), Color(0xFF242424), Color(0xFF101010))
        c.isDark -> listOf(light, lerp(light, mid, 0.5f), lerp(mid, deep, 0.5f)).map { lerp(it, Color.Black, DARK_DEEPEN) }
        else -> listOf(lerp(light, Color.White, if (bright) 0.6f else 0.15f), light, lerp(mid, deep, 0.35f))
    }
    return Ramp(raw.map { ContrastGuard.ensure(it, listOf(ink), PRO_CONTRAST, bright) }, ink)
}

private fun proLook(c: FrameColorScheme): ProLook {
    val ramp = accentRamp(c)
    val ink = ramp.ink
    val textSide = ramp.stops.takeLast(2)
    val soft = ContrastGuard.ensure(lerp(textSide[1], ink, SOFT_INK), textSide, PRO_CONTRAST, ink.luminance() > 0.5f)
    val inkIsLight = ink.luminance() > 0.5f
    val pillBg = if (inkIsLight) Color.White else lerp(ramp.stops[2], Color.Black, 0.82f)
    val pillTone = if (inkIsLight) ramp.stops[2] else ramp.stops[0]
    val pillInk = ContrastGuard.ensure(pillTone, listOf(pillBg), PRO_CONTRAST, !inkIsLight)
    val jewel = c.isDark && !c.neutral
    val sheen = if (jewel) 0.34f else if (c.isDark) 0.30f else 0.60f
    val accent = c.accentA.toOklch()
    val bloom = oklch(0.80f, accent.c.coerceIn(0.11f, 0.19f) * 0.62f, accent.h)
    val deepStop = ramp.stops[2]
    val accentC = c.accentC.toOklch()
    val iconChroma = if (c.neutral) 0f else accent.c.coerceIn(0.12f, 0.20f)
    return ProLook(
        stops = ramp.stops,
        ink = ink,
        inkSoft = soft,
        pillBg = pillBg,
        pillInk = pillInk,
        edgeTop = Color.White.copy(alpha = sheen),
        edgeBottom = Color.White.copy(alpha = if (c.isDark) 0.06f else 0.16f),
        glow = Color.White.copy(alpha = if (c.isDark) 0.26f else 0.42f),
        shadow = ramp.stops[1],
        jewel = jewel,
        bloom = bloom,
        vignette = Color(deepStop.red * 0.55f, deepStop.green * 0.55f, deepStop.blue * 0.55f),
        iconRamp = proIconRamp(c.accentA, c.accentB, c.neutral, onDarkCard = inkIsLight),
        accentGlow = oklch(if (inkIsLight) 0.70f else 0.66f, iconChroma, accentC.h)
    )
}

private fun homeTitle(c: FrameColorScheme): Color = when {
    c.neutral -> c.onSurface
    c.isDark -> ContrastGuard.ensure(lerp(c.primary, Color.White, 0.55f), listOf(c.surface, c.container), ContrastGuard.TEXT, true)
    else -> ContrastGuard.ensure(lerp(c.primary, Color.Black, 0.3f), listOf(Color.White, c.surface, c.background), ContrastGuard.TEXT, false)
}

private fun homeDesc(c: FrameColorScheme): Color = when {
    c.neutral -> c.onSurfaceVariant
    c.isDark -> ContrastGuard.ensure(lerp(c.onSurfaceVariant, c.primary, 0.15f), listOf(c.surface, c.container), ContrastGuard.TEXT, true)
    else -> c.onSurfaceVariant
}

internal fun buildHomeLook(c: FrameColorScheme): HomeLook {
    val title = homeTitle(c)
    return HomeLook(
        dark = c.isDark,
        neutral = c.neutral,
        title = title,
        desc = homeDesc(c),
        grabber = title.copy(alpha = 0.28f),
        pro = proLook(c),
        tones = homeTones(c)
    )
}

private fun toneReach(lightness: Float, hue: Float): Float = oklch(lightness, 0.4f, hue).toOklch().c

private fun homeTones(c: FrameColorScheme): HomeTones {
    if (c.neutral) {
        return HomeTones(
            images = c.accentA,
            video = c.accentB,
            compare = lerp(c.accentB, c.accentC, 0.5f),
            collage = c.accentC,
            edit = lerp(c.accentA, c.accentC, 0.5f),
            pro = lerp(c.accentA, c.accentB, 0.5f)
        )
    }
    val lightness = if (c.isDark) TONE_L_DARK else TONE_L_LIGHT
    val base = listOf(c.accentA, c.accentB, c.accentC).map { it.toOklch() }
    val reach = base.minOf { toneReach(lightness, it.h) }
    val chroma = minOf(reach, base[0].c.coerceIn(minOf(base[0].c, TONE_CHROMA_FLOOR), TONE_CHROMA_CEIL))
    fun tone(x: Color) = oklch(lightness, chroma, base[0].h + hueDelta(base[0].h, x.toOklch().h).coerceIn(-TONE_HUE_SPREAD, TONE_HUE_SPREAD))
    val a = tone(c.accentA)
    val b = tone(c.accentB)
    val d = tone(c.accentC)
    return HomeTones(
        images = a,
        video = b,
        compare = tone(lerp(b, d, 0.5f)),
        collage = d,
        edit = tone(lerp(a, d, 0.5f)),
        pro = tone(lerp(a, b, 0.5f))
    )
}
