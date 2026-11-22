package com.frameout.app.trim

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.frameout.engine.player.FramePtsIndex
import com.frameout.engine.timeline.TrimRange
import com.frameout.core.util.safeCoerceIn

internal fun pickHandle(x: Float, geometry: TrackGeometry, range: TrimRange, hit: Float, handleWidth: Float): Handle {
    val startX = geometry.xOf(range.startMs)
    val endX = geometry.xOf(range.endMs)
    val startShown = startX >= geometry.inset - 0.5f && startX <= geometry.trackEnd + handleWidth
    val endShown = endX >= geometry.inset - handleWidth && endX <= geometry.trackEnd + 0.5f
    val dStart = if (startShown) kotlin.math.abs(x - (startX - handleWidth / 2)) else Float.MAX_VALUE
    val dEnd = if (endShown) kotlin.math.abs(x - (endX + handleWidth / 2)) else Float.MAX_VALUE
    return when {
        dStart <= hit && dStart <= dEnd -> Handle.Start
        dEnd <= hit -> Handle.End
        else -> Handle.None
    }
}

internal fun applyDrag(target: Handle, ms: Long, range: TrimRange, onSeek: (Long) -> Unit, onRangeChange: (TrimRange) -> Unit) {
    when (target) {
        Handle.Start -> {
            val start = ms.coerceAtMost(range.endMs - MIN_GAP_MS).coerceAtLeast(0L)
            onRangeChange(range.copy(startMs = start, useFull = false))
            onSeek(start)
        }
        Handle.End -> {
            val end = ms.coerceAtLeast(range.startMs + MIN_GAP_MS)
            onRangeChange(range.copy(endMs = end, useFull = false))
            onSeek(end)
        }
        Handle.None -> onSeek(ms)
    }
}

internal fun DrawScope.drawTimeline(
    dur: Long,
    positionMs: Long,
    range: TrimRange,
    selecting: Boolean,
    active: Handle,
    paint: TimelinePaint,
    zoom: TimelineZoom,
    frames: LongArray? = null
) {
    val geometry = TrackGeometry(size.width, HandleWidth.toPx(), dur, zoom)
    val h = size.height
    val hw = HandleWidth.toPx()
    if (selecting) {
        val startX = geometry.xOf(range.startMs)
        val endX = geometry.xOf(range.endMs).coerceAtLeast(startX)
        val from = startX.safeCoerceIn(geometry.inset, geometry.trackEnd)
        val to = endX.safeCoerceIn(geometry.inset, geometry.trackEnd)
        val dim = Color.Black.copy(alpha = 0.58f)
        drawRect(dim, Offset(geometry.inset, 0f), Size((from - geometry.inset).coerceAtLeast(0f), h))
        drawRect(dim, Offset(to, 0f), Size((geometry.trackEnd - to).coerceAtLeast(0f), h))
        val edge = 3.dp.toPx()
        drawRect(paint.primary, Offset(from, 0f), Size(to - from, edge))
        drawRect(paint.primary, Offset(from, h - edge), Size(to - from, edge))
        if (startX >= geometry.inset - 0.5f && startX <= geometry.trackEnd + hw) drawHandle(startX - hw, h, active == Handle.Start, paint)
        if (endX >= geometry.inset - hw && endX <= geometry.trackEnd + 0.5f) drawHandle(endX, h, active == Handle.End, paint)
    }
    if (frames != null) drawFrameTicks(geometry, frames, h)
    drawPlayhead(geometry.xOf(positionMs), h)
}

private fun DrawScope.drawFrameTicks(g: TrackGeometry, frames: LongArray, h: Float) {
    val fromUs = (g.viewStart * 1000f).toLong()
    val toUs = ((g.viewStart + g.viewSpan) * 1000f).toLong()
    var i = FramePtsIndex.lowerBound(frames, fromUs)
    val visible = FramePtsIndex.upperBound(frames, toUs) - i
    if (visible <= 0) return
    val perFrame = g.trackWidth / visible
    if (perFrame < MIN_TICK_PX.dp.toPx()) return
    val len = (if (perFrame >= TALL_TICK_PX.dp.toPx()) 12.dp else 7.dp).toPx()
    val color = Color.White.copy(alpha = 0.7f)
    val shadow = Color.Black.copy(alpha = 0.45f)
    val stroke = 1.dp.toPx()
    while (i < frames.size && frames[i] <= toUs) {
        val x = g.inset + (frames[i] / 1000f - g.viewStart) / g.viewSpan * g.trackWidth
        drawLine(shadow, Offset(x + stroke, h - len), Offset(x + stroke, h), strokeWidth = stroke)
        drawLine(color, Offset(x, h - len), Offset(x, h), strokeWidth = stroke)
        i++
    }
}

private const val MIN_TICK_PX = 5
private const val TALL_TICK_PX = 12

private fun DrawScope.drawHandle(x: Float, h: Float, active: Boolean, paint: TimelinePaint) {
    val hw = HandleWidth.toPx()
    val radius = CornerRadius(5.dp.toPx())
    drawRoundRect(paint.primary, Offset(x, 0f), Size(hw, h), radius)
    if (active) drawRoundRect(Color.White, Offset(x, 0f), Size(hw, h), radius, style = Stroke(2.dp.toPx()))
    val grip = Size(2.dp.toPx(), h * 0.36f)
    drawRoundRect(paint.onPrimary, Offset(x + hw / 2 - grip.width / 2, h * 0.32f), grip, CornerRadius(1.dp.toPx()))
}

private fun DrawScope.drawPlayhead(x: Float, h: Float) {
    drawLine(Color.Black.copy(alpha = 0.4f), Offset(x, 0f), Offset(x, h), strokeWidth = 5.dp.toPx())
    drawLine(Color.White, Offset(x, 0f), Offset(x, h), strokeWidth = 2.dp.toPx())
    drawCircle(Color.White, 5.dp.toPx(), Offset(x, 5.dp.toPx()))
}
