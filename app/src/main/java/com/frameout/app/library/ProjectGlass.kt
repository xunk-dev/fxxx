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
    "Extracting", "Indexing" -> c.primary
    "Completed" -> lerp(c.accentA, c.accentB, 0.5f)
    "Paused" -> c.warning
    "Failed" -> c.error
    else -> c.accentC
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
        .background(if (solid) Brush.linearGradient(listOf(flat, flat)) else Brush.linearGradient(listOf(g.fillTop, g.fillBottom)))
        .drawBehind {
            if (solid) return@drawBehind
            val rtl = layoutDirection == LayoutDirection.Rtl
            val cx = size.width * (if (rtl) 0.12f else 0.88f)
            val cy = size.height * 0.05f
            val r = size.width * 0.75f
            drawCircle(
                Brush.radialGradient(listOf(tone.copy(alpha = (g.lightAlpha * 0.8f + boost).coerceAtMost(0.6f)), Color.Transparent), Offset(cx, cy), r),
                r, Offset(cx, cy)
            )
            drawLine(g.highlight, Offset(size.height * 0.25f, 0.5f), Offset(size.width - size.height * 0.25f, 0.5f), 1.dp.toPx())
        }
}
