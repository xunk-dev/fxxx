package com.frameout.app.library

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.foundation.background
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.frameout.app.home.rememberGlassStyle
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.theme.FrameColorScheme
import com.frameout.designsystem.theme.FrameTheme

internal fun cardTone(status: String, c: FrameColorScheme): Color = when (status) {
    "Paused" -> lerp(c.primaryContainer, c.warning, 0.18f)
    "Failed" -> lerp(c.primaryContainer, c.error, 0.22f)
    else -> c.primaryContainer
}

@Composable
internal fun Modifier.projectGlass(shape: Shape, tone: Color, boost: Float = 0f): Modifier {
    val g = rememberGlassStyle()
    val colors = FrameTheme.colors
    val solid = LocalSystemA11y.current.reduceTransparency
    val flat = if (colors.isDark) colors.containerHigh else colors.surface
    return this
        .then(
            if (g.dark) Modifier
            else Modifier.shadow(10.dp, shape, ambientColor = tone.copy(alpha = 0.22f), spotColor = tone.copy(alpha = 0.22f))
        )
        .clip(shape)
        .background(
            if (solid) Brush.linearGradient(listOf(flat, flat))
            else if (g.dark) Brush.linearGradient(listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.035f)))
            else Brush.linearGradient(listOf(g.fillTop, g.fillBottom))
        )
        .drawBehind {
            if (solid) return@drawBehind
            val rtl = layoutDirection == LayoutDirection.Rtl
            val cx = size.width * (if (rtl) 0.12f else 0.88f)
            val cy = size.height * 0.05f
            val r = size.width * 0.75f
            drawCircle(
                Brush.radialGradient(listOf(tone.copy(alpha = ((if (g.dark) 0.55f else g.lightAlpha * 0.8f) + boost).coerceAtMost(0.7f)), Color.Transparent), Offset(cx, cy), r),
                r, Offset(cx, cy)
            )
            drawLine(g.highlight, Offset(size.height * 0.25f, 0.5f), Offset(size.width - size.height * 0.25f, 0.5f), 1.dp.toPx())
        }
}
