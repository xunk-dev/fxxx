package com.frameout.app.splash

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.frameout.designsystem.theme.FrameAccent
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.sin

internal const val LOGO_FRACTION = 0.30f
private const val STAGE_UNIT = 121.6f
private const val IMPACT_MS = 880f
private const val ECHO_COUNT = 4
private const val ECHO_LAG_MS = 34f
private val LINE_Y = floatArrayOf(-88f, -58f, -24f, 32f, 62f, 96f, 134f, -102f)

internal fun splashUnit(minDimension: Float): Float = minDimension * LOGO_FRACTION / STAGE_UNIT

internal fun splashShake(t: Float): Offset {
    val dt = t - IMPACT_MS
    if (dt <= 0f) return Offset.Zero
    val amp = 9f * exp(-dt / 120f)
    return Offset(amp * sin(dt / 17f), amp * 0.55f * cos(dt / 13f))
}

internal fun DrawScope.drawSplashLogo(t: Float, c: Offset, width: Float, accent: FrameAccent, onLight: Boolean, paths: SplashPaths) {
    val tint = if (onLight) accent.a else Color.White
    drawSplashGlow(t, c, width, accent, onLight)
    drawSpeedLines(t, c, width / STAGE_UNIT, tint)
    val s = width / ICON_W
    val b = logoBrushes(accent, onLight)
    withTransform({
        translate(c.x - ICON_CX * s, c.y - ICON_CY * s)
        scale(s, s, Offset.Zero)
    }) {
        drawWhooshLayers(t, b, paths)
        drawSlamFrame(t, b, paths, tint)
        drawImpact(t, tint, onLight)
    }
}

private fun DrawScope.drawSplashGlow(t: Float, c: Offset, width: Float, accent: FrameAccent, onLight: Boolean) {
    val appear = outCubic(seg(t, 0f, 320f))
    val strength = if (onLight) 0.7f else 1f
    val glow = (0.16f + 0.22f * outCubic(seg(t, 300f, 1000f))) * appear * strength
    val radius = width * 1.1f
    drawCircle(Brush.radialGradient(listOf(accent.a.copy(alpha = glow), Color.Transparent), center = c, radius = radius), radius, c)
}

private fun DrawScope.drawSpeedLines(t: Float, c: Offset, u: Float, tint: Color) {
    for (j in LINE_Y.indices) {
        val from = 230f + j * 38f
        val pr = seg(t, from, from + 420f)
        if (pr <= 0f || pr >= 1f) continue
        val len = (90f + (j * 37) % 90) * u
        val x = c.x + (250f - 640f * pr) * u
        val h = maxOf(1f, 1.6f * u)
        val brush = Brush.horizontalGradient(listOf(Color.Transparent, tint), startX = x, endX = x + len)
        drawRoundRect(brush, Offset(x, c.y + LINE_Y[j] * u), Size(len, h), CornerRadius(h / 2f), alpha = 0.55f * sin(PI.toFloat() * pr))
    }
}

private fun DrawScope.slide(t: Float, from: Float, to: Float, dx: Float, dy: Float, lag: Float, block: DrawScope.(Float) -> Unit) {
    val e = 1f - outExpo(seg(t, from, to))
    val dist = hypot(dx * e, dy * e)
    if (lag > 0f && dist > 3f && t > from) {
        for (k in ECHO_COUNT downTo 1) {
            val ke = 1f - outExpo(seg(t - k * lag, from, to))
            val alpha = 0.34f * (1f - k / (ECHO_COUNT + 1f)) * minOf(1f, dist / 120f)
            withTransform({ translate(dx * ke, dy * ke) }) { block(alpha) }
        }
    }
    withTransform({ translate(dx * e, dy * e) }) { block(seg(t, from, from + 90f)) }
}

private fun DrawScope.drawWhooshLayers(t: Float, b: LogoBrushes, paths: SplashPaths) {
    slide(t, 150f, 700f, -850f, 0f, ECHO_LAG_MS) { a -> drawPath(paths.strip, b.strip, alpha = a) }
    slide(t, 220f, 740f, 0f, -520f, 0f) { a ->
        drawPath(paths.bandBase, b.bandBase, alpha = a)
        drawPath(paths.bandShade, b.bandShade, alpha = a)
    }
    slide(t, 260f, 760f, -520f, 0f, ECHO_LAG_MS) { a -> drawPath(paths.connector, b.connector, alpha = a) }
    slide(t, 240f, 760f, 0f, -460f, 0f) { a -> drawPath(paths.face, b.face, alpha = a) }
    slide(t, 300f, 800f, 950f, 0f, ECHO_LAG_MS) { a -> drawPath(paths.trailOuter, b.trailOuter, alpha = a) }
    slide(t, 360f, 860f, 950f, 0f, ECHO_LAG_MS) { a -> drawPath(paths.trailMid, b.trailMid, alpha = a) }
}

private fun DrawScope.drawSlamFrame(t: Float, b: LogoBrushes, paths: SplashPaths, tint: Color) {
    val fp = seg(t, 560f, 900f)
    val a = seg(fp, 0f, 0.3f)
    if (a <= 0f) return
    val k = 1f + 0.7f * (1f - outExpo(fp))
    withTransform({ scale(k, k, FRAME_PIVOT) }) {
        drawPath(paths.mountain, b.mountain, alpha = a)
        drawSplashSun(t, b, paths, a, tint)
        drawPath(paths.ring, b.ring, alpha = a)
        drawGlintAt(inOut(seg(t, 1250f, 1700f)), paths)
    }
}

private fun DrawScope.drawSplashSun(t: Float, b: LogoBrushes, paths: SplashPaths, alpha: Float, tint: Color) {
    val sunP = seg(t, 900f, 1250f)
    if (sunP > 0f) {
        val k = outBack(sunP)
        withTransform({ scale(k, k, SUN_CENTER) }) { drawPath(paths.sun, b.sun, alpha = alpha) }
    }
    val pulse = outCubic(seg(t, 930f, 1400f))
    if (pulse > 0f && pulse < 1f) {
        drawCircle(tint.copy(alpha = 0.7f * (1f - pulse)), SUN_R + 100f * pulse, SUN_CENTER, style = Stroke(5f * (1f - pulse) + 0.5f))
    }
}

private fun DrawScope.drawImpact(t: Float, tint: Color, onLight: Boolean) {
    if (t < IMPACT_MS) return
    val weight = if (onLight) 0.55f else 1f
    val flash = 0.8f * weight * (1f - outCubic(seg(t, IMPACT_MS, 1180f)))
    val center = Offset(FRAME_PIVOT.x - 20f, FRAME_PIVOT.y + 10f)
    if (flash > 0f) {
        drawCircle(Brush.radialGradient(listOf(tint.copy(alpha = flash), Color.Transparent), center = center, radius = 520f), 520f, center)
    }
    val sp = seg(t, IMPACT_MS, 1380f)
    if (sp < 1f) {
        drawCircle(tint.copy(alpha = 0.65f * weight * (1f - sp)), 60f + 760f * outCubic(sp), FRAME_PIVOT, style = Stroke(16f * (1f - sp) + 1f))
    }
}
