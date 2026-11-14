package com.frameout.app.billing

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.frameout.core.pro.ProShowcase
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameHairline

private class QueueVideo(val hueTop: Float, val hueBottom: Float, val title: Float)

private val Videos = listOf(QueueVideo(210f, 260f, 0.62f), QueueVideo(25f, 55f, 0.48f), QueueVideo(150f, 200f, 0.70f))
private const val CYCLE_MS = 7600
private val Done = Color(0xFF4BE3A3)

private fun edgeFade(p: Float): Float = minOf((p / 0.05f).coerceIn(0f, 1f), ((0.97f - p) / 0.05f).coerceIn(0f, 1f))

private fun progressOf(p: Float, index: Int): Float = ((p - (0.10f + 0.26f * index)) / 0.22f).coerceIn(0f, 1f)

@Composable
internal fun BatchShowcase() {
    val colors = FrameTheme.colors
    val cycle = rememberCycle(LocalSystemA11y.current.reduceMotion, CYCLE_MS, 0.9f)
    val finished by remember { derivedStateOf { Videos.indices.count { progressOf(cycle.value, it) >= 1f } } }
    Column(
        Modifier
            .width(248.dp)
            .graphicsLayer { alpha = edgeFade(cycle.value) },
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            FrameText(ProShowcase.batchTitle(), FrameTheme.typography.titleSmall, colors.onSurface, Modifier.weight(1f))
            FrameText(
                "$finished/${Videos.size}",
                FrameTheme.typography.label.copy(fontFamily = FontFamily.Monospace),
                proOnBrush(),
                Modifier.background(proBrush(), FrameShapes.pill).padding(horizontal = 9.dp, vertical = 2.dp)
            )
        }
        Videos.forEachIndexed { index, video -> QueueLine(video, index, cycle) }
    }
}

@Composable
private fun QueueLine(video: QueueVideo, index: Int, cycle: State<Float>) {
    val colors = FrameTheme.colors
    val shape = FrameShapes.sm
    Row(
        Modifier
            .fillMaxWidth()
            .height(42.dp)
            .background(colors.onSurface.copy(alpha = 0.07f), shape)
            .border(1.dp, frameHairline(), shape)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(width = 36.dp, height = 26.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(Brush.verticalGradient(listOf(Color.hsv(video.hueTop, 0.55f, 0.85f), Color.hsv(video.hueBottom, 0.6f, 0.5f))))
        )
        QueueProgress(video, index, cycle, Modifier.weight(1f))
        QueueStatus(index, cycle)
    }
}

@Composable
private fun QueueProgress(video: QueueVideo, index: Int, cycle: State<Float>, modifier: Modifier) {
    val colors = FrameTheme.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(Modifier.fillMaxWidth(video.title).height(6.dp).background(colors.onSurface.copy(alpha = 0.34f), FrameShapes.pill))
        Canvas(Modifier.fillMaxWidth().height(4.dp)) {
            val radius = CornerRadius(size.height / 2f)
            drawRoundRect(colors.onSurface.copy(alpha = 0.12f), size = size, cornerRadius = radius)
            val fill = size.width * progressOf(cycle.value, index)
            if (fill > 0f) {
                val brush = Brush.horizontalGradient(listOf(colors.accentC, colors.accentA, colors.accentB))
                drawRoundRect(brush, size = Size(fill, size.height), cornerRadius = radius)
            }
        }
    }
}

@Composable
private fun QueueStatus(index: Int, cycle: State<Float>) {
    val colors = FrameTheme.colors
    Canvas(Modifier.size(20.dp)) {
        val k = progressOf(cycle.value, index)
        val w = 2.dp.toPx()
        if (k >= 1f) {
            drawCircle(Done, size.minDimension / 2f)
            val tick = Path().apply {
                moveTo(size.width * 0.28f, size.height * 0.52f)
                lineTo(size.width * 0.44f, size.height * 0.68f)
                lineTo(size.width * 0.74f, size.height * 0.34f)
            }
            drawPath(tick, Color.White, style = Stroke(w, cap = StrokeCap.Round, join = StrokeJoin.Round))
        } else {
            val inset = w
            drawCircle(colors.onSurface.copy(alpha = 0.14f), size.minDimension / 2f - inset, style = Stroke(w))
            if (k > 0f) drawArc(colors.accentA, -90f, 360f * k, false, Offset(inset, inset), Size(size.width - inset * 2f, size.height - inset * 2f), style = Stroke(w, cap = StrokeCap.Round))
        }
    }
}
