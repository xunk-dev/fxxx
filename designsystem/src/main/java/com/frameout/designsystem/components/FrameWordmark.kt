package com.frameout.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.frameout.designsystem.theme.FrameTheme

private const val DarkLift = 0.18f
private const val LightDeepen = 0.28f

@Composable
fun FrameWordmark(modifier: Modifier = Modifier, height: Dp = 20.dp, description: String? = null) {
    val colors = wordmarkColors()
    val shapes = remember { FrameWordmarkData.strokes.map { PathParser().parsePathString(it.first).toPath() to it.second } }
    val labelled = if (description == null) modifier else modifier.semantics { contentDescription = description }
    Canvas(labelled.height(height).aspectRatio(FrameWordmarkData.WIDTH / FrameWordmarkData.HEIGHT)) {
        val k = size.height / FrameWordmarkData.HEIGHT
        val brush = Brush.horizontalGradient(colors, 0f, FrameWordmarkData.WIDTH)
        withTransform({ scale(k, k, Offset.Zero) }) { drawWordmark(shapes, brush) }
    }
}

@Composable
private fun wordmarkColors(): List<Color> {
    val c = FrameTheme.colors
    return remember(c.accentA, c.accentB, c.accentC, c.onSurface, c.isDark, c.neutral) {
        val accents = listOf(c.accentA, c.accentB, c.accentC)
        when {
            c.neutral -> listOf(c.onSurface, c.onSurface)
            c.isDark -> accents.map { lerp(it, Color.White, DarkLift) }
            else -> accents.map { lerp(it, Color.Black, LightDeepen) }
        }
    }
}

private fun DrawScope.drawWordmark(shapes: List<Pair<Path, Float>>, brush: Brush) {
    shapes.forEach { (path, width) ->
        drawPath(path, brush, style = Stroke(width, miter = 10f, cap = StrokeCap.Butt, join = StrokeJoin.Miter))
    }
    drawCircle(brush, FrameWordmarkData.DOT_R, Offset(FrameWordmarkData.DOT_X, FrameWordmarkData.DOT_Y))
}
