package com.frameout.app.viewer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.theme.FrameTheme

private val RAIL_HEIGHT = 7.dp
private val RAIL_LINE = 1.dp
private val THUMB_LINE = 2.5.dp
private val MIN_THUMB = 28.dp
private val RAIL_INSET = 12.dp

@Composable
internal fun GlowScrollRail(scroll: ScrollState, viewportPx: Int, modifier: Modifier = Modifier) {
    val reduce = LocalSystemA11y.current.reduceMotion
    val colors = FrameTheme.colors
    val duration = if (reduce) 0 else 220
    val overflow = scroll.maxValue > 0 && viewportPx > 0
    val shown by animateFloatAsState(if (overflow) 1f else 0f, tween(duration), label = "railShown")
    val energy by animateFloatAsState(if (scroll.isScrollInProgress) 1f else 0.6f, tween(duration), label = "railEnergy")
    val track = colors.divider
    val accent = colors.primary
    Canvas(modifier.fillMaxWidth().height(RAIL_HEIGHT).padding(horizontal = RAIL_INSET)) {
        val cy = size.height / 2f
        val line = RAIL_LINE.toPx()
        drawRoundRect(track, Offset(0f, cy - line / 2f), Size(size.width, line), CornerRadius(line / 2f))
        if (shown <= 0.01f || scroll.maxValue <= 0) return@Canvas
        val max = scroll.maxValue.toFloat()
        val ratio = viewportPx / (viewportPx + max)
        val thumb = (size.width * ratio).coerceIn(MIN_THUMB.toPx().coerceAtMost(size.width), size.width)
        val travel = size.width - thumb
        val progress = (scroll.value / max).coerceIn(0f, 1f)
        val x = if (layoutDirection == LayoutDirection.Rtl) travel * (1f - progress) else travel * progress
        val glow = shown * energy
        fun band(height: Float, color: Color) =
            drawRoundRect(color, Offset(x, cy - height / 2f), Size(thumb, height), CornerRadius(height / 2f))
        band(RAIL_HEIGHT.toPx(), accent.copy(alpha = 0.10f * glow))
        band(RAIL_HEIGHT.toPx() * 0.65f, accent.copy(alpha = 0.20f * glow))
        val core = THUMB_LINE.toPx()
        drawRoundRect(
            Brush.horizontalGradient(
                listOf(accent.copy(alpha = 0.55f * shown), accent.copy(alpha = shown), accent.copy(alpha = 0.55f * shown)),
                startX = x, endX = x + thumb
            ),
            Offset(x, cy - core / 2f), Size(thumb, core), CornerRadius(core / 2f)
        )
    }
}
