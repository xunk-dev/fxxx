package com.frameout.app.splash

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.frameout.designsystem.theme.FrameAccent
import kotlin.math.max
import kotlin.math.min

internal class LogoPalette(accent: FrameAccent, private val onLight: Boolean = false) {
    private val ramp: Array<FloatArray>
    private val pop: Array<FloatArray>
    private val neutral = accent.neutral
    private val ink = mix(rgb(accent.lightPrimary), BLACK, 0.55f)

    init {
        if (accent.key == "indigo") {
            ramp = O_RAMP
            pop = O_POP
        } else {
            val a = rgb(accent.a)
            val b = rgb(accent.b)
            val c = rgb(accent.c)
            val b2 = mix(b, WHITE, 0.18f)
            ramp = arrayOf(mix(c, WHITE, 0.10f), a, b, b2)
            pop = arrayOf(mix(c, WHITE, 0.30f), mix(b2, WHITE, 0.25f))
        }
    }

    fun color(argb: Long): Color {
        val alpha = ((argb shr 24) and 0xFF) / 255f
        val x = floatArrayOf(((argb shr 16) and 0xFF).toFloat(), ((argb shr 8) and 0xFF).toFloat(), (argb and 0xFF).toFloat())
        val r = remap(x)
        return Color(r[0] / 255f, r[1] / 255f, r[2] / 255f, alpha)
    }

    fun brush(g: LogoGradient): Brush {
        val stops = Array(g.stops.size) { g.stops[it].first to color(g.stops[it].second) }
        return Brush.linearGradient(*stops, start = Offset(g.x1, g.y1), end = Offset(g.x2, g.y2))
    }

    private fun remap(x: FloatArray): FloatArray {
        if (chroma(x) <= NEUTRAL_CHROMA) return if (onLight && lum(x) > LIGHT_LUM) mix(x, ink, 0.88f) else x
        var h = hue(x)
        val oi: FloatArray
        val ni: FloatArray
        if (h >= RAMP_MIN_H && h <= RAMP_MAX_H) {
            val hh = h.coerceIn(O_RAMP_H[0], O_RAMP_H[3])
            var i = 0
            while (i < 2 && hh > O_RAMP_H[i + 1]) i++
            val u = (hh - O_RAMP_H[i]) / (O_RAMP_H[i + 1] - O_RAMP_H[i])
            oi = mix(O_RAMP[i], O_RAMP[i + 1], u)
            ni = mix(ramp[i], ramp[i + 1], u)
        } else {
            if (h > 180f) h -= 360f
            val u = ((O_POP_H0 - h) / (O_POP_H0 - O_POP_H1)).coerceIn(0f, 1f)
            oi = mix(O_POP[0], O_POP[1], u)
            ni = mix(pop[0], pop[1], u)
        }
        val out = FloatArray(3) { (ni[it] + (x[it] - oi[it])).coerceIn(0f, 255f) }
        return finish(if (onLight) darken(out) else out)
    }

    private fun darken(c: FloatArray): FloatArray = mix(c, ink, ((lum(c) - 110f) / 145f).coerceIn(0f, 1f) * 0.5f)

    private fun finish(out: FloatArray): FloatArray {
        if (neutral) {
            val g = 0.2126f * out[0] + 0.7152f * out[1] + 0.0722f * out[2]
            return floatArrayOf(g, g, g)
        }
        return out
    }

    private companion object {
        const val NEUTRAL_CHROMA = 0.08f
        const val RAMP_MIN_H = 168f
        const val RAMP_MAX_H = 300f
        const val LIGHT_LUM = 140f
        val WHITE = floatArrayOf(255f, 255f, 255f)
        val BLACK = floatArrayOf(0f, 0f, 0f)

        val O_RAMP = arrayOf(
            floatArrayOf(1f, 246f, 253f), floatArrayOf(42f, 68f, 251f),
            floatArrayOf(119f, 37f, 251f), floatArrayOf(208f, 42f, 252f)
        )
        val O_POP = arrayOf(floatArrayOf(252f, 168f, 75f), floatArrayOf(251f, 86f, 134f))
        val O_RAMP_H = FloatArray(4) { hue(O_RAMP[it]) }
        val O_POP_H0 = hue(O_POP[0])
        val O_POP_H1 = hue(O_POP[1]) - 360f

        fun rgb(c: Color) = floatArrayOf(c.red * 255f, c.green * 255f, c.blue * 255f)

        fun mix(a: FloatArray, b: FloatArray, t: Float) = FloatArray(3) { a[it] * (1f - t) + b[it] * t }

        fun lum(c: FloatArray) = 0.2126f * c[0] + 0.7152f * c[1] + 0.0722f * c[2]

        fun chroma(c: FloatArray) = (max(c[0], max(c[1], c[2])) - min(c[0], min(c[1], c[2]))) / 255f

        fun hue(c: FloatArray): Float {
            val r = c[0] / 255f
            val g = c[1] / 255f
            val b = c[2] / 255f
            val mx = max(r, max(g, b))
            val d = mx - min(r, min(g, b))
            if (d == 0f) return 0f
            val h = when (mx) {
                r -> ((g - b) / d) % 6f
                g -> (b - r) / d + 2f
                else -> (r - g) / d + 4f
            }
            return (h * 60f + 360f) % 360f
        }
    }
}

internal class LogoBrushes(accent: FrameAccent, onLight: Boolean = false) {
    private val p = LogoPalette(accent, onLight)
    val strip = p.brush(LogoGradients.strip)
    val bandBase = p.brush(LogoGradients.bandBase)
    val bandShade = p.brush(LogoGradients.bandShade)
    val connector = p.brush(LogoGradients.connector)
    val face = p.brush(LogoGradients.face)
    val trailOuter = p.brush(LogoGradients.trailOuter)
    val trailMid = p.brush(LogoGradients.trailMid)
    val mountain = p.brush(LogoGradients.mountain)
    val sun = p.brush(LogoGradients.sun)
    val ring = p.brush(LogoGradients.ring)
}

@Volatile private var cached: Triple<FrameAccent, Boolean, LogoBrushes>? = null

internal fun logoBrushes(accent: FrameAccent, onLight: Boolean = false): LogoBrushes {
    val c = cached
    if (c != null && c.first == accent && c.second == onLight) return c.third
    return LogoBrushes(accent, onLight).also { cached = Triple(accent, onLight, it) }
}
