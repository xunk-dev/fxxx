package com.frameout.app.home

import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.frameout.designsystem.theme.FrameTheme

fun Modifier.homeAmbientGlow(): Modifier = composed {
    val colors = FrameTheme.colors
    if (colors.neutral) return@composed Modifier
    val strength = if (colors.isDark) 1f else 0.55f
    Modifier.drawWithCache {
        val radius = size.width * 0.85f
        fun blob(alpha: Float, center: Offset) =
            Brush.radialGradient(listOf(colors.accentA.copy(alpha = alpha * strength), Color.Transparent), center, radius) to center
        val blobs = listOf(
            blob(0.18f, Offset(size.width, 0f)),
            blob(0.07f, Offset(0f, size.height * 0.55f)),
            blob(0.11f, Offset(size.width, size.height * 0.95f))
        )
        onDrawBehind { blobs.forEach { (brush, center) -> drawCircle(brush, radius, center) } }
    }
}
