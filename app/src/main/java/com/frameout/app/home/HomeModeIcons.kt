package com.frameout.app.home

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import com.frameout.designsystem.theme.FrameTheme

internal val HomeGold = Color(0xFFE8C26A)
private val BoltTop = Color(0xFFFFF4A8)
private val BoltBottom = Color(0xFFFFB020)
@Composable
internal fun homeAccent(): Color = if (FrameTheme.colors.isDark) FrameTheme.colors.primary else FrameTheme.accent.darkPrimary

@Composable
internal fun homeOnAccent(): Color = if (FrameTheme.colors.isDark) FrameTheme.colors.onPrimary else FrameTheme.accent.darkOnPrimary

private val FilmBase = Color(0xFF15171C)

@Composable
internal fun QuickModeIcon(modifier: Modifier = Modifier) {
    val primary = homeAccent()
    Canvas(modifier) {
        scale(size.width / 100f, size.height / 80f, Offset.Zero) {
            drawGlow()
            drawArt(primary)
            drawBolt()
        }
    }
}

@Composable
internal fun ProModeIcon(modifier: Modifier = Modifier) {
    val primary = homeAccent()
    Canvas(modifier) {
        scale(size.width / 100f, size.height / 80f, Offset.Zero) {
            drawArt(primary)
            drawBadge()
        }
    }
}

private fun DrawScope.drawGlow() {
    val center = Offset(62f, 44f)
    drawCircle(Brush.radialGradient(listOf(HomeGold.copy(alpha = 0.3f), Color.Transparent), center, 48f), 48f, center)
}

private fun DrawScope.drawArt(primary: Color) {
    rotate(-10f, Offset(50f, 54f)) {
        val strip = CornerRadius(5f)
        drawRoundRect(FilmBase, Offset(10f, 38f), Size(80f, 30f), strip)
        drawRoundRect(primary.copy(alpha = 0.55f), Offset(10f, 38f), Size(80f, 30f), strip, Stroke(1.4f))
        for (i in 0 until 8) {
            val x = 15f + i * 9.5f
            drawRoundRect(primary.copy(alpha = 0.5f), Offset(x, 41f), Size(5f, 3.6f), CornerRadius(1f))
            drawRoundRect(primary.copy(alpha = 0.5f), Offset(x, 62.4f), Size(5f, 3.6f), CornerRadius(1f))
        }
    }
    rotate(8f, Offset(46f, 28f)) {
        val card = Path().apply { addRoundRect(RoundRect(22f, 6f, 70f, 50f, CornerRadius(7f))) }
        drawPath(card, Brush.verticalGradient(listOf(lerp(primary, Color.White, 0.35f), lerp(primary, Color.Black, 0.25f)), 6f, 50f))
        clipPath(card) {
            drawCircle(Color.White.copy(alpha = 0.92f), 5f, Offset(34f, 19f))
            drawPath(peak(26f, 46f, 66f, 22f), lerp(primary, Color.Black, 0.3f))
            drawPath(peak(44f, 58f, 74f, 32f), lerp(primary, Color.Black, 0.55f))
        }
        drawPath(card, Color.White.copy(alpha = 0.55f), style = Stroke(1.2f))
    }
}

private fun peak(left: Float, top: Float, right: Float, peakY: Float) = Path().apply {
    moveTo(left, 52f)
    lineTo(top, peakY)
    lineTo(right, 52f)
    close()
}

private fun DrawScope.drawBolt() {
    val bolt = Path().apply {
        moveTo(80f, 34f)
        lineTo(66f, 55f)
        lineTo(75f, 55f)
        lineTo(70f, 74f)
        lineTo(88f, 49f)
        lineTo(78.5f, 49f)
        close()
    }
    rotate(12f, Offset(77f, 54f)) {
        drawPath(bolt, Brush.verticalGradient(listOf(BoltTop, BoltBottom), 34f, 74f))
        drawPath(bolt, Color(0xFF8A5A00).copy(alpha = 0.55f), style = Stroke(1f))
    }
}

private fun DrawScope.drawBadge() {
    val center = Offset(78f, 58f)
    drawCircle(FilmBase, 14f, center)
    drawCircle(HomeGold, 14f, center, style = Stroke(1.8f))
    val knobs = floatArrayOf(74f, 82f, 77f)
    for (i in 0..2) {
        val y = 51.5f + i * 6.5f
        drawLine(HomeGold.copy(alpha = 0.85f), Offset(70f, y), Offset(86f, y), 1.4f, StrokeCap.Round)
        drawCircle(FilmBase, 3.5f, Offset(knobs[i], y))
        drawCircle(HomeGold, 2.3f, Offset(knobs[i], y))
    }
}
