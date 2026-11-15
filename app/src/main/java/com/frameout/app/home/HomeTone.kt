package com.frameout.app.home

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

internal class Oklch(val l: Float, val c: Float, val h: Float)

private fun toLinear(v: Float): Float =
    if (v <= 0.04045f) v / 12.92f else ((v + 0.055f) / 1.055f).pow(2.4f)

private fun fromLinear(v: Float): Float {
    val x = v.coerceIn(0f, 1f)
    return if (x <= 0.0031308f) x * 12.92f else 1.055f * x.pow(1f / 2.4f) - 0.055f
}

private fun cbrt(v: Float): Float = Math.cbrt(v.toDouble()).toFloat()

internal fun Color.toOklch(): Oklch {
    val r = toLinear(red)
    val g = toLinear(green)
    val b = toLinear(blue)
    val l = cbrt(0.4122214708f * r + 0.5363325363f * g + 0.0514459929f * b)
    val m = cbrt(0.2119034982f * r + 0.6806995451f * g + 0.1073969566f * b)
    val s = cbrt(0.0883024619f * r + 0.2817188376f * g + 0.6299787005f * b)
    val lightness = 0.2104542553f * l + 0.7936177850f * m - 0.0040720468f * s
    val a = 1.9779984951f * l - 2.4285922050f * m + 0.4505937099f * s
    val bb = 0.0259040371f * l + 0.7827717662f * m - 0.8086757660f * s
    val hue = (Math.toDegrees(atan2(bb.toDouble(), a.toDouble())).toFloat() + 360f) % 360f
    return Oklch(lightness, hypot(a, bb), hue)
}

internal fun oklch(lightness: Float, chroma: Float, hueDeg: Float): Color {
    val hr = Math.toRadians(hueDeg.toDouble())
    val ca = cos(hr).toFloat()
    val sa = sin(hr).toFloat()
    var c = chroma
    var r = 0f
    var g = 0f
    var b = 0f
    for (i in 0 until 40) {
        val a = c * ca
        val bb = c * sa
        val l = (lightness + 0.3963377774f * a + 0.2158037573f * bb).let { it * it * it }
        val m = (lightness - 0.1055613458f * a - 0.0638541728f * bb).let { it * it * it }
        val s = (lightness - 0.0894841775f * a - 1.2914855480f * bb).let { it * it * it }
        r = 4.0767416621f * l - 3.3077115913f * m + 0.2309699292f * s
        g = -1.2684380046f * l + 2.6097574011f * m - 0.3413193965f * s
        b = -0.0041960863f * l - 0.7034186147f * m + 1.7076147010f * s
        val inGamut = r >= -0.002f && g >= -0.002f && b >= -0.002f && r <= 1.002f && g <= 1.002f && b <= 1.002f
        if (inGamut) break
        c *= 0.94f
    }
    return Color(fromLinear(r), fromLinear(g), fromLinear(b))
}

internal fun hueDelta(from: Float, to: Float): Float = ((to - from + 540f) % 360f) - 180f

internal fun tileWash(tone: Color, look: HomeLook): Color = when {
    !look.dark -> Color.White
    look.neutral -> lerp(tone, Color.Black, 0.86f)
    else -> {
        val t = tone.toOklch()
        oklch(0.17f, min(t.c, 0.17f) * 0.55f, t.h)
    }
}

internal fun analog(color: Color, degrees: Float): Color {
    val o = color.toOklch()
    return oklch(o.l, o.c, o.h + degrees)
}
