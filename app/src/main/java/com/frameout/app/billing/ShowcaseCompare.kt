package com.frameout.app.billing

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.theme.FrameTheme
import kotlin.math.sin

private const val CYCLE_MS = 7000
private const val TWO_PI = 6.2831855f
private val Gray = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
private val HandleInk = Color(0xFF1B1B22)
private val Hot = Color(0xFFFF4D6D)

@Composable
internal fun CompareShowcase() {
    val colors = FrameTheme.colors
    val scene = rememberShowcaseScene(ShowcaseArt.Compare)
    val reveal by animateFloatAsState(if (scene != null) 1f else 0f, tween(300), label = "compareReveal")
    val cycle = rememberCycle(LocalSystemA11y.current.reduceMotion, CYCLE_MS, 0.12f)
    Box(Modifier.size(SCENE_W_DP.dp, SCENE_H_DP.dp).clip(RoundedCornerShape(16.dp)).graphicsLayer { alpha = reveal }) {
        Canvas(Modifier.fillMaxSize()) {
            val s = scene ?: return@Canvas
            val p = cycle.value
            val x = size.width * (0.5f + 0.34f * sin(p * TWO_PI))
            drawImage(s.sharp)
            clipRect(left = x) {
                val other = s.other
                if (other != null) {
                    drawFrameB(s, other, p)
                } else {
                    drawImage(s.soft, colorFilter = Gray)
                    drawRect(Color.Black.copy(alpha = 0.52f))
                    drawHeat(p, colors.accentC, colors.accentA)
                }
            }
            drawHandle(x)
        }
        Tag("A", Modifier.align(Alignment.TopStart))
        Tag("B", Modifier.align(Alignment.TopEnd))
    }
}

private fun DrawScope.drawFrameB(s: ShowcaseScene, other: ImageBitmap, p: Float) {
    val wave = sin(p * TWO_PI * 2f) * 0.5f + 0.5f
    val k = wave * wave * (3f - 2f * wave)
    drawImage(other)
    if (k <= 0.01f) return
    drawImage(other, colorFilter = Gray, alpha = k)
    drawRect(Color.Black.copy(alpha = 0.5f * k))
    val heat = s.heat ?: return
    drawImage(heat, alpha = k)
    drawImage(heat, alpha = 0.6f * k)
}

private fun DrawScope.drawHeat(p: Float, inner: Color, outer: Color) {
    val spots = listOf(Offset(0.27f, 0.50f) to 0.30f, Offset(0.72f, 0.75f) to 0.26f, Offset(0.60f, 0.28f) to 0.20f)
    spots.forEachIndexed { i, (at, r) ->
        val beat = sin(p * TWO_PI * 2f + i * 1.7f) * 0.5f + 0.5f
        val radius = size.height * r * (0.85f + 0.3f * beat)
        val c = Offset(at.x * size.width, at.y * size.height)
        val stops = listOf(Hot.copy(alpha = 0.95f), inner.copy(alpha = 0.55f), outer.copy(alpha = 0.18f), Color.Transparent)
        drawCircle(Brush.radialGradient(stops, c, radius), radius, c)
    }
}

private fun DrawScope.drawHandle(x: Float) {
    val cy = size.height / 2f
    drawLine(Color.White, Offset(x, 0f), Offset(x, size.height), 2.dp.toPx())
    drawCircle(Color.Black.copy(alpha = 0.25f), 14.dp.toPx(), Offset(x, cy + 1.dp.toPx()))
    drawCircle(Color.White, 12.dp.toPx(), Offset(x, cy))
    val w = 1.8.dp.toPx()
    val a = 3.dp.toPx()
    val b = 6.5.dp.toPx()
    val h = 4.dp.toPx()
    drawLine(HandleInk, Offset(x - a, cy - h), Offset(x - b, cy), w, StrokeCap.Round)
    drawLine(HandleInk, Offset(x - b, cy), Offset(x - a, cy + h), w, StrokeCap.Round)
    drawLine(HandleInk, Offset(x + a, cy - h), Offset(x + b, cy), w, StrokeCap.Round)
    drawLine(HandleInk, Offset(x + b, cy), Offset(x + a, cy + h), w, StrokeCap.Round)
}

@Composable
private fun Tag(text: String, modifier: Modifier) {
    FrameText(
        text,
        FrameTheme.typography.label.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
        Color.White,
        modifier
            .padding(8.dp)
            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}
