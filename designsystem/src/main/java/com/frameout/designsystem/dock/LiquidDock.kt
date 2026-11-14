package com.frameout.designsystem.dock

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.theme.FrameColorScheme
import com.frameout.designsystem.theme.ContrastGuard
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.GraphiteFab
import com.frameout.designsystem.theme.GraphiteFabDark

internal object LiquidDock {
    const val FlowMillis = 8000
    const val BreathMillis = 3800

    const val PillTintA = 0.34f
    const val PillTintB = 0.22f
    const val PillTintC = 0.30f

    const val BubbleInkContrast = 5.0f
    const val BubbleGlow = 0.55f
    const val BubbleSquash = 0.18f

    const val DropLeadStiffness = 430f
    const val DropLeadDamping = 0.78f
    const val DropTrailStiffness = 130f
    const val DropTrailDamping = 1f
    const val DropShrinkW = 0.30f
    const val DropShrinkH = 0.34f
    const val DropNeckGap = 0.5f
}

@Composable
internal fun rememberLiquidFlow(millis: Int = LiquidDock.FlowMillis): State<Float> {
    val reduce = LocalSystemA11y.current.reduceMotion
    if (reduce) return remember { mutableFloatStateOf(0.5f) }
    return rememberInfiniteTransition(label = "liquidFlow").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(millis, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "liquidFlowValue"
    )
}

@Composable
internal fun rememberLiquidBreath(): State<Float> = rememberLiquidFlow(LiquidDock.BreathMillis)

internal fun liquidStops(c: FrameColorScheme): List<Color> = when {
    c.neutral -> if (c.isDark) GraphiteFabDark else GraphiteFab
    else -> listOf(c.accentC, c.accentA, c.accentB, c.accentC)
}

internal fun bubbleStops(c: FrameColorScheme): List<Color> =
    liquidStops(c).map { ContrastGuard.ensure(it, listOf(Color.White), LiquidDock.BubbleInkContrast, false) }

private fun flowBrush(stops: List<Color>, w: Float, h: Float, t: Float): Brush =
    Brush.linearGradient(
        colors = stops,
        start = Offset(w * 0.9f * t, 0f),
        end = Offset(w * 0.9f * t + w * 1.1f, h),
        tileMode = TileMode.Mirror
    )

@Composable
internal fun Modifier.liquidFlowBackground(): Modifier {
    val stops = liquidStops(FrameTheme.colors)
    val flow = rememberLiquidFlow()
    return drawBehind { drawRect(flowBrush(stops, size.width, size.height, flow.value)) }
}

@Composable
internal fun Modifier.liquidGlassPill(shape: Shape): Modifier {
    val c = FrameTheme.colors
    val flow = rememberLiquidFlow()
    val solid = LocalSystemA11y.current.reduceTransparency
    val k = if (c.isDark) 1f else 0.45f
    val base = if (c.isDark) lerp(c.surface, Color.Black, 0.5f).copy(alpha = if (solid) 1f else 0.94f) else c.surface.copy(alpha = if (solid) 1f else 0.95f)
    val tint = if (c.neutral || solid) emptyList() else listOf(
        c.accentA.copy(alpha = LiquidDock.PillTintA * k),
        c.accentC.copy(alpha = LiquidDock.PillTintB * k),
        c.accentB.copy(alpha = LiquidDock.PillTintC * k),
        c.accentA.copy(alpha = LiquidDock.PillTintA * k)
    )
    val rim = Brush.verticalGradient(
        listOf(
            (if (c.isDark) Color.White else c.onSurface).copy(alpha = if (c.isDark) 0.22f else 0.14f),
            c.primary.copy(alpha = if (c.isDark) 0.20f else 0.30f)
        )
    )
    return drawBehind {
        val outline = shape.createOutline(size, layoutDirection, this)
        drawOutline(outline, base)
        if (tint.isNotEmpty()) drawOutline(outline, flowBrush(tint, size.width, size.height, flow.value))
        if (!solid) drawOutline(outline, Brush.verticalGradient(listOf(Color.White.copy(alpha = if (c.isDark) 0.07f else 0.25f), Color.Transparent), 0f, size.height * 0.55f))
        drawOutline(outline, rim, style = Stroke(1.dp.toPx()))
    }
}

internal fun DrawScope.drawLiquidBubble(
    colors: FrameColorScheme,
    stops: List<Color>,
    flow: Float,
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    alpha: Float = 1f
) {
    val radius = CornerRadius(minOf(22.dp.toPx(), height / 2f))
    drawBubbleGlow(colors, left, top, width, height, radius.x, alpha)
    val fill = flowBrush(stops, width, height, flow)
    translate(left, top) {
        drawRoundRect(fill, Offset.Zero, Size(width, height), radius, alpha = alpha)
        drawRoundRect(
            Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.42f), Color.Transparent, Color.Black.copy(alpha = 0.10f))),
            Offset.Zero, Size(width, height), radius, alpha = alpha
        )
        drawRoundRect(Color.White.copy(alpha = 0.30f * alpha), Offset.Zero, Size(width, height), radius, style = Stroke(1.dp.toPx()))
    }
}

private fun DrawScope.drawBubbleGlow(
    colors: FrameColorScheme,
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    radius: Float,
    alpha: Float
) {
    if (colors.neutral) return
    val glowPx = 16.dp.toPx()
    for (i in 6 downTo 1) {
        val t = i / 6f
        val grow = glowPx * t
        drawRoundRect(
            colors.primary.copy(alpha = (LiquidDock.BubbleGlow * 0.10f * (1f - t) + 0.02f) * alpha),
            Offset(left - grow, top - grow + 3.dp.toPx() * t), Size(width + grow * 2f, height + grow * 2f), CornerRadius(radius + grow)
        )
    }
}

internal fun DrawScope.drawLiquidDrop(
    colors: FrameColorScheme,
    stops: List<Color>,
    flow: Float,
    centerA: Float,
    centerB: Float,
    centerY: Float,
    width: Float,
    height: Float,
    alpha: Float = 1f
) {
    val lo = minOf(centerA, centerB)
    val hi = maxOf(centerA, centerB)
    val dx = hi - lo
    val radius = minOf(22.dp.toPx(), height / 2f)
    val left = lo - width / 2f
    val top = centerY - height / 2f
    val totalW = dx + width

    if (totalW < width * 1.5f) {
        drawBubbleGlow(colors, left, top, totalW, height, radius, alpha)
    } else {
        drawBubbleGlow(colors, left, top, width, height, radius, alpha)
        drawBubbleGlow(colors, hi - width / 2f, top, width, height, radius, alpha)
    }

    var shape = Path().apply { addRoundRect(RoundRect(0f, 0f, width, height, CornerRadius(radius))) }
    if (dx > 0.5f) {
        val second = Path().apply { addRoundRect(RoundRect(dx, 0f, dx + width, height, CornerRadius(radius))) }
        shape = Path.combine(PathOperation.Union, shape, second)
        val xl = width - radius
        val xr = dx + radius
        val gap = dx - width
        val t = if (gap <= 0f) 1f else (1f - gap / (width * LiquidDock.DropNeckGap)).coerceIn(0f, 1f)
        val k = t * t * (3f - 2f * t)
        if (xr > xl && k > 0.06f) {
            val cy = height / 2f
            val nh = cy * k
            val xm = (xl + xr) / 2f
            val c = xm - xl
            val bridge = Path().apply {
                moveTo(xl, 0f)
                cubicTo(xl + c * 0.6f, 0f, xm - c * 0.4f, cy - nh, xm, cy - nh)
                cubicTo(xm + c * 0.4f, cy - nh, xr - c * 0.6f, 0f, xr, 0f)
                lineTo(xr, height)
                cubicTo(xr - c * 0.6f, height, xm + c * 0.4f, cy + nh, xm, cy + nh)
                cubicTo(xm - c * 0.4f, cy + nh, xl + c * 0.6f, height, xl, height)
                close()
            }
            shape = Path.combine(PathOperation.Union, shape, bridge)
        }
    }

    translate(left, top) {
        drawPath(shape, flowBrush(stops, totalW, height, flow), alpha = alpha)
        drawPath(
            shape,
            Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.42f), Color.Transparent, Color.Black.copy(alpha = 0.10f))),
            alpha = alpha
        )
        drawPath(shape, Color.White.copy(alpha = 0.30f * alpha), style = Stroke(1.dp.toPx()))
    }
}
