package com.frameout.app.billing

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.theme.FrameTheme
import kotlin.math.sin

private class CollageCell(val l: Float, val t: Float, val r: Float, val b: Float)

private val Grid = listOf(CollageCell(0f, 0f, 0.5f, 0.5f), CollageCell(0.5f, 0f, 1f, 0.5f), CollageCell(0f, 0.5f, 0.5f, 1f), CollageCell(0.5f, 0.5f, 1f, 1f))
private val Hero = listOf(CollageCell(0f, 0f, 0.62f, 1f), CollageCell(0.62f, 0f, 1f, 0.34f), CollageCell(0.62f, 0.34f, 1f, 0.67f), CollageCell(0.62f, 0.67f, 1f, 1f))
private val Mosaic = listOf(CollageCell(0f, 0f, 0.4f, 0.62f), CollageCell(0.4f, 0f, 1f, 0.38f), CollageCell(0f, 0.62f, 0.4f, 1f), CollageCell(0.4f, 0.38f, 1f, 1f))
private val Layouts = listOf(Grid, Hero, Mosaic)

private val Centers = listOf(Offset(0.30f, 0.55f), Offset(0.72f, 0.45f), Offset(0.50f, 0.72f), Offset(0.62f, 0.30f))
private const val CYCLE_MS = 9000
private const val TWO_PI = 6.2831855f

private fun mix(a: Float, b: Float, k: Float): Float = a + (b - a) * k

@Composable
internal fun CollageShowcase() {
    val colors = FrameTheme.colors
    val scene = rememberShowcaseScene(ShowcaseArt.Collage)
    val reveal by animateFloatAsState(if (scene != null) 1f else 0f, tween(300), label = "collageReveal")
    val cycle = rememberCycle(LocalSystemA11y.current.reduceMotion, CYCLE_MS, 0.5f)
    Canvas(Modifier.size(SCENE_W_DP.dp, SCENE_H_DP.dp).graphicsLayer { alpha = reveal }) {
        val s = scene ?: return@Canvas
        val p = cycle.value
        val segment = (p * Layouts.size).coerceAtMost(Layouts.size - 0.001f)
        val from = segment.toInt()
        val to = (from + 1) % Layouts.size
        val raw = ((segment - from - 0.55f) / 0.35f).coerceIn(0f, 1f)
        val k = raw * raw * (3f - 2f * raw)
        val pulse = sin(p * TWO_PI * 3f) * 0.5f + 0.5f
        val gap = (2f + 3f * pulse).dp.toPx()
        val radius = (5f + 8f * pulse).dp.toPx()
        for (i in 0 until 4) {
            val a = Layouts[from][i]
            val b = Layouts[to][i]
            val rect = Rect(
                mix(a.l, b.l, k) * size.width + gap / 2f,
                mix(a.t, b.t, k) * size.height + gap / 2f,
                mix(a.r, b.r, k) * size.width - gap / 2f,
                mix(a.b, b.b, k) * size.height - gap / 2f
            )
            drawCell(s, i, rect, radius)
        }
        val active = if (k < 0.5f) from else to
        val y = size.height - 9.dp.toPx()
        for (d in Layouts.indices) {
            val x = size.width / 2f + (d - 1) * 12.dp.toPx()
            drawCircle(Color.Black.copy(alpha = 0.40f), 4.dp.toPx(), Offset(x, y))
            drawCircle(if (d == active) colors.accentC else Color.White.copy(alpha = 0.45f), 2.8.dp.toPx(), Offset(x, y))
        }
    }
}

private fun DrawScope.drawCell(s: ShowcaseScene, index: Int, r: Rect, radius: Float) {
    if (r.width < 2f || r.height < 2f) return
    val sw = s.sharp.width
    val sh = s.sharp.height
    val aspect = r.width / r.height
    var cropH = sh * (0.58f + 0.08f * index)
    var cropW = cropH * aspect
    if (cropW > sw) {
        cropW = sw.toFloat()
        cropH = cropW / aspect
    }
    val center = Centers[index]
    val left = (center.x * sw - cropW / 2f).coerceIn(0f, sw - cropW)
    val top = (center.y * sh - cropH / 2f).coerceIn(0f, sh - cropH)
    val shape = Path().apply { addRoundRect(RoundRect(r, CornerRadius(radius))) }
    clipPath(shape) {
        drawImage(
            s.sharp,
            srcOffset = IntOffset(left.toInt(), top.toInt()),
            srcSize = IntSize(cropW.toInt().coerceAtLeast(1), cropH.toInt().coerceAtLeast(1)),
            dstOffset = IntOffset(r.left.toInt(), r.top.toInt()),
            dstSize = IntSize(r.width.toInt(), r.height.toInt())
        )
    }
    drawRoundRect(Color.White.copy(alpha = 0.32f), r.topLeft, r.size, CornerRadius(radius), style = Stroke(1.dp.toPx()))
}
