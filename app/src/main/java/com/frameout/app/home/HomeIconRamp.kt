package com.frameout.app.home

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix

@Immutable
internal class IconRamp(
    val shadow: Color,
    val deep: Color,
    val body: Color,
    val light: Color
)

@Immutable
internal class RampSpec(val lightness: FloatArray, val chroma: FloatArray)

internal object RampSpecs {
    val ToolDark = RampSpec(floatArrayOf(0.38f, 0.58f, 0.79f, 0.975f), floatArrayOf(0.62f, 1.15f, 0.85f, 0.10f))
    val ToolLight = RampSpec(floatArrayOf(0.40f, 0.56f, 0.73f, 0.985f), floatArrayOf(0.78f, 1.12f, 0.90f, 0.10f))
    val ProOnDark = RampSpec(floatArrayOf(0.50f, 0.68f, 0.84f, 0.98f), floatArrayOf(0.65f, 1.10f, 0.80f, 0.10f))
    val ProOnLight = RampSpec(floatArrayOf(0.30f, 0.44f, 0.58f, 0.80f), floatArrayOf(0.75f, 1.10f, 1.00f, 0.50f))
}

private const val TOOL_CHROMA_MIN = 0.10f
private const val TOOL_CHROMA_MAX = 0.19f
private const val PRO_CHROMA_MIN = 0.12f
private const val PRO_CHROMA_MAX = 0.20f
private const val SHADOW_HUE_PULL = 0.4f
private const val SHADOW_HUE_MAX = 10f

private fun shadowHue(hue: Float, accentB: Color): Float =
    hue + (hueDelta(hue, accentB.toOklch().h) * SHADOW_HUE_PULL).coerceIn(-SHADOW_HUE_MAX, SHADOW_HUE_MAX)

internal fun buildIconRamp(hue: Float, chroma: Float, shadowHue: Float, spec: RampSpec): IconRamp = IconRamp(
    shadow = oklch(spec.lightness[0], chroma * spec.chroma[0], shadowHue),
    deep = oklch(spec.lightness[1], chroma * spec.chroma[1], hue),
    body = oklch(spec.lightness[2], chroma * spec.chroma[2], hue),
    light = oklch(spec.lightness[3], chroma * spec.chroma[3], hue)
)

internal fun toolIconRamp(tone: Color, accentB: Color, dark: Boolean, neutral: Boolean): IconRamp {
    val o = tone.toOklch()
    val chroma = if (neutral) 0f else o.c.coerceIn(TOOL_CHROMA_MIN, TOOL_CHROMA_MAX)
    return buildIconRamp(o.h, chroma, shadowHue(o.h, accentB), if (dark) RampSpecs.ToolDark else RampSpecs.ToolLight)
}

internal fun proIconRamp(accentA: Color, accentB: Color, neutral: Boolean, onDarkCard: Boolean): IconRamp {
    val o = accentA.toOklch()
    val chroma = if (neutral) 0f else o.c.coerceIn(PRO_CHROMA_MIN, PRO_CHROMA_MAX)
    return buildIconRamp(o.h, chroma, shadowHue(o.h, accentB), if (onDarkCard) RampSpecs.ProOnDark else RampSpecs.ProOnLight)
}

private fun Color.channel(i: Int): Float = when (i) {
    0 -> red
    1 -> green
    else -> blue
}

internal fun gradientMapFilter(r: IconRamp): ColorFilter {
    val m = FloatArray(20)
    for (c in 0..2) {
        val s0 = r.shadow.channel(c)
        val s1 = r.deep.channel(c)
        val s2 = r.body.channel(c)
        val s3 = r.light.channel(c)
        m[c * 5 + 0] = s1 - s0
        m[c * 5 + 1] = s2 - s1
        m[c * 5 + 2] = s3 - s2
        m[c * 5 + 4] = s0 * 255f
    }
    m[18] = 1f
    return ColorFilter.colorMatrix(ColorMatrix(m))
}
