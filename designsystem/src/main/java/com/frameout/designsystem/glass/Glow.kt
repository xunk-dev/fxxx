package com.frameout.designsystem.glass

import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import com.frameout.designsystem.theme.FrameTheme
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.frameGlow(
    color: Color,
    alpha: () -> Float,
    spread: Dp = 16.dp,
    cornerFraction: () -> Float = { 0.5f }
): Modifier = composed {
    val colors = FrameTheme.colors
    if (colors.neutral) return@composed monoGlow(colors.isDark, alpha, spread, cornerFraction)
    Modifier.drawBehind {
        val a = alpha()
        if (a <= 0.005f) return@drawBehind
        val spreadPx = spread.toPx()
        val base = size.minDimension * cornerFraction().coerceIn(0f, 0.5f)
        val steps = 8
        for (i in steps downTo 1) {
            val t = i / steps.toFloat()
            val grow = spreadPx * t
            val layer = (a * (1f - t) * 0.20f + a * 0.01f).coerceIn(0f, 1f)
            drawRoundRect(
                color = color.copy(alpha = layer),
                topLeft = Offset(-grow, -grow),
                size = Size(size.width + grow * 2f, size.height + grow * 2f),
                cornerRadius = CornerRadius(base + grow)
            )
        }
    }
}

private fun monoGlow(
    dark: Boolean,
    alpha: () -> Float,
    spread: Dp,
    cornerFraction: () -> Float
): Modifier = Modifier.drawBehind {
    val a = alpha()
    if (a <= 0.005f) return@drawBehind
    val spreadPx = spread.toPx()
    val base = size.minDimension * cornerFraction().coerceIn(0f, 0.5f)
    val steps = 10
    val drop = if (dark) 0f else spreadPx * 0.32f
    val tone = if (dark) Color.White else Color.Black
    val gain = if (dark) 0.20f else 0.30f
    val reach = if (dark) 1.15f else 1f
    for (i in steps downTo 1) {
        val t = i / steps.toFloat()
        val grow = spreadPx * reach * t
        val fall = (1f - t) * (1f - t)
        val layer = (a * (fall * gain + 0.012f)).coerceIn(0f, 1f)
        drawRoundRect(
            color = tone.copy(alpha = layer),
            topLeft = Offset(-grow, -grow + drop),
            size = Size(size.width + grow * 2f, size.height + grow * 2f),
            cornerRadius = CornerRadius(base + grow)
        )
    }
}
