package com.frameout.app.billing

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.core.pro.ProShowcase
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme

private const val CYCLE_MS = 7600
private val Good = Color(0xFF4BE3A3)

@Composable
internal fun InsightsShowcase() {
    val colors = FrameTheme.colors
    val scene = rememberShowcaseScene(ShowcaseArt.Insights)
    val reveal by animateFloatAsState(if (scene != null) 1f else 0f, tween(300), label = "insightsReveal")
    val cycle = rememberCycle(LocalSystemA11y.current.reduceMotion, CYCLE_MS, 0.8f)
    Box(Modifier.size(SCENE_W_DP.dp, SCENE_H_DP.dp).clip(RoundedCornerShape(16.dp)).graphicsLayer { alpha = reveal }) {
        Canvas(Modifier.fillMaxSize()) {
            val s = scene ?: return@Canvas
            val p = cycle.value
            drawImage(s.sharp)
            drawScan(p, colors.accentC)
            drawFace(phase(p, 0.22f, 0.34f), s.spots)
            drawReadBox(phase(p, 0.40f, 0.54f), colors.accentA, s.spots)
            drawSelectionHandles(phase(p, 0.52f, 0.62f), colors.accentA, s.spots)
        }
        InsightChip(
            ProShowcase.faceGood(), FrameIcons.FaceGood, Good,
            Modifier.align(Alignment.BottomStart).padding(8.dp).graphicsLayer { alpha = phase(cycle.value, 0.30f, 0.42f) }
        )
        InsightChip(
            ProShowcase.liveText(), FrameIconsExtra.Text, colors.accentA,
            Modifier.align(Alignment.BottomEnd).padding(8.dp).graphicsLayer { alpha = phase(cycle.value, 0.50f, 0.62f) }
        )
    }
}

private fun DrawScope.drawScan(p: Float, accent: Color) {
    val k = ((p - 0.04f) / 0.40f).coerceIn(0f, 1f)
    if (k <= 0f || k >= 1f) return
    val y = size.height * k
    val band = size.height * 0.18f
    drawRect(Brush.verticalGradient(listOf(Color.Transparent, accent.copy(alpha = 0.45f)), startY = y - band, endY = y), Offset(0f, y - band), Size(size.width, band))
    drawLine(accent, Offset(0f, y), Offset(size.width, y), 2.dp.toPx())
}

private fun DrawScope.drawFace(k: Float, spots: SceneSpots) {
    if (k <= 0f) return
    val c = Offset(size.width * spots.face.x, size.height * spots.face.y)
    val r = size.height * spots.faceR * 1.55f * (0.7f + 0.3f * k)
    drawCircle(Good.copy(alpha = 0.18f * k), r, c)
    drawCircle(Good.copy(alpha = k), r, c, style = Stroke(2.dp.toPx()))
    drawArc(Color.White.copy(alpha = 0.9f * k), -90f, 360f * k, false, Offset(c.x - r - 4.dp.toPx(), c.y - r - 4.dp.toPx()), Size((r + 4.dp.toPx()) * 2f, (r + 4.dp.toPx()) * 2f), style = Stroke(1.5.dp.toPx()))
}

private fun DrawScope.drawReadBox(k: Float, accent: Color, spots: SceneSpots) {
    if (k <= 0f) return
    val z = spots.plate
    val pad = 4.dp.toPx()
    val tl = Offset(size.width * z.left - pad, size.height * z.top - pad)
    val box = Size(size.width * z.width * k + pad * 2f, size.height * z.height + pad * 2f)
    drawRoundRect(accent.copy(alpha = 0.28f * k), tl, box, CornerRadius(6.dp.toPx()))
    drawRoundRect(accent.copy(alpha = k), tl, box, CornerRadius(6.dp.toPx()), style = Stroke(1.6.dp.toPx()))
}

private fun DrawScope.drawSelectionHandles(k: Float, accent: Color, spots: SceneSpots) {
    if (k <= 0f) return
    val z = spots.plate
    val pad = 4.dp.toPx()
    val top = size.height * z.top - pad
    val bottom = size.height * z.bottom + pad
    val left = size.width * z.left - pad
    val right = size.width * z.right + pad
    listOf(Offset(left, top) to -1f, Offset(right, bottom) to 1f).forEach { (at, dir) ->
        val knob = Offset(at.x, at.y + dir * 8.dp.toPx())
        drawLine(accent.copy(alpha = k), at, knob, 2.dp.toPx())
        drawCircle(accent.copy(alpha = k), 4.dp.toPx(), knob)
        drawCircle(Color.White.copy(alpha = k), 1.6.dp.toPx(), knob)
    }
}

@Composable
private fun InsightChip(text: String, icon: Int, tint: Color, modifier: Modifier) {
    val shape = FrameShapes.pill
    Row(
        modifier
            .background(Color.Black.copy(alpha = 0.58f), shape)
            .border(1.dp, tint.copy(alpha = 0.85f), shape)
            .padding(start = 7.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(painterResource(icon), null, Modifier.size(13.dp), colorFilter = ColorFilter.tint(tint))
        FrameText(text, FrameTheme.typography.label.copy(fontSize = 10.sp), Color.White, Modifier.padding(start = 5.dp), maxLines = 1, softWrap = false)
    }
}
