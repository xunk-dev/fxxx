package com.frameout.app.billing

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val RAYS = 14
private const val CONFETTI = 34
private val Confetti = listOf(ProGold.Light, ProGold.Deep, Color.White, Color(0xFFFFC857))

internal fun DrawScope.drawCelebration(center: Offset, t: Float, pulse: Float, spin: Float, accent: Color) {
    val reach = maxOf(size.width, size.height) * 0.62f
    drawHalo(center, reach, t, pulse, accent)
    drawRays(center, reach, t, spin)
    drawFlash(center, reach, t)
    drawRings(center, reach, t)
    drawConfetti(t, spin)
}

private fun DrawScope.drawHalo(c: Offset, reach: Float, t: Float, pulse: Float, accent: Color) {
    val k = seg(t, 0.05f, 0.5f) * (0.30f + 0.10f * pulse)
    val brush = Brush.radialGradient(listOf(ProGold.Deep.copy(alpha = k), accent.copy(alpha = k * 0.30f), Color.Transparent), c, reach * 0.7f)
    drawCircle(brush, reach * 0.7f, c)
}

private fun DrawScope.drawRays(c: Offset, reach: Float, t: Float, spin: Float) {
    val alpha = seg(t, 0.12f, 0.55f) * 0.42f
    if (alpha <= 0f) return
    val half = (PI / RAYS * 0.34).toFloat()
    val brush = Brush.radialGradient(listOf(ProGold.Light.copy(alpha = alpha), Color.Transparent), c, reach)
    rotate(spin * 360f * 0.25f, c) {
        for (i in 0 until RAYS) {
            val a = (2.0 * PI * i / RAYS).toFloat()
            val path = Path().apply {
                moveTo(c.x, c.y)
                lineTo(c.x + cos(a - half) * reach, c.y + sin(a - half) * reach)
                lineTo(c.x + cos(a + half) * reach, c.y + sin(a + half) * reach)
                close()
            }
            drawPath(path, brush)
        }
    }
}

private fun DrawScope.drawFlash(c: Offset, reach: Float, t: Float) {
    val f = seg(t, 0.03f, 0.09f) * (1f - seg(t, 0.09f, 0.32f))
    if (f <= 0f) return
    val r = reach * 0.8f
    drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.85f * f), ProGold.Light.copy(alpha = 0.35f * f), Color.Transparent), c, r), r, c)
}

private fun DrawScope.drawRings(c: Offset, reach: Float, t: Float) {
    for (k in 0..2) {
        val p = ((t - (0.07f + 0.10f * k)) / 0.55f).coerceIn(0f, 1f)
        if (p <= 0f || p >= 1f) continue
        val width = 3.dp.toPx() * (1f - p) + 0.5f
        drawCircle((if (k == 1) Color.White else ProGold.Light).copy(alpha = (1f - p) * 0.6f), reach * 0.6f * easeOut(p), c, style = Stroke(width))
    }
}

private fun DrawScope.drawConfetti(t: Float, spin: Float) {
    val fade = seg(t, 0.22f, 0.5f)
    if (fade <= 0f) return
    for (i in 0 until CONFETTI) {
        val x = unitHash(i * 2.3f + 1f) * size.width
        val y = ((unitHash(i * 5.1f) + spin * (0.6f + 0.8f * unitHash(i * 7.7f))) % 1f) * (size.height + 40.dp.toPx()) - 20.dp.toPx()
        val sway = sin((spin * 6.2832f * 2f) + i) * 10.dp.toPx()
        val w = (4f + 4f * unitHash(i * 3.3f)).dp.toPx()
        withTransform({ rotate(spin * 720f + i * 40f, Offset(x + sway, y)) }) {
            drawRect(Confetti[i % Confetti.size].copy(alpha = 0.75f * fade), Offset(x + sway - w / 2f, y - w / 4f), Size(w, w / 2f))
        }
    }
}
