package com.frameout.app.frames

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.frameout.core.export.ContactSheetLayout
import com.frameout.core.export.ContactSheetPreview
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.theme.FrameTheme
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal val PaperWidth = 200.dp
internal val PaperHeight = 188.dp

private val PaperColor = Color(0xFF121212)
private val CellColor = Color(0xFF1E1E1E)
private val InkColor = Color(0xFFE8E8E8)
private val MutedColor = Color(0xFFA8A8A8)
private val Placeholders = listOf(
    Color(0xFF27345F) to Color(0xFF5B6BB8),
    Color(0xFF1D4A47) to Color(0xFF4AA396),
    Color(0xFF55324C) to Color(0xFFB0679A),
    Color(0xFF46431F) to Color(0xFFA39D52),
    Color(0xFF1F4659) to Color(0xFF58A0C6),
    Color(0xFF573625) to Color(0xFFC27A52)
)

internal class PaperSpec(
    val summary: ContactSheetPreview.Summary,
    val captioned: Boolean,
    val thumbs: List<ImageBitmap?>,
    val canAdd: Boolean
)

@Composable
internal fun ContactPaper(spec: PaperSpec, description: String, onAdd: (() -> Unit)?, modifier: Modifier = Modifier) {
    val accent = FrameTheme.colors.primary
    val base = modifier.size(PaperWidth, PaperHeight).semantics { contentDescription = description }
    val tap = if (onAdd != null) base.frameClickable(onClick = onAdd) else base
    Box(tap) {
        Canvas(Modifier.size(PaperWidth, PaperHeight)) { drawPaper(spec, accent) }
    }
}

private class PaperGeo(val k: Float, val slotH: Float, val addSlot: Int, val fullH: Float, val paperH: Float)

private fun DrawScope.geoOf(spec: PaperSpec): PaperGeo {
    val plan = spec.summary.plan
    val k = size.width / ContactSheetLayout.SHEET_WIDTH
    val slotH = (plan.cellH + plan.captionH).toFloat()
    val addSlot = if (spec.canAdd && spec.summary.hasRoom) spec.summary.firstCount else -1
    val shown = spec.summary.firstCount + if (addSlot >= 0) 1 else 0
    val rows = max(1, ceil(shown / plan.columns.toFloat()).toInt())
    val chrome = 2 * ContactSheetLayout.MARGIN + ContactSheetLayout.HEADER
    val full = (chrome + rows * slotH + (rows - 1) * ContactSheetLayout.GAP) * k
    return PaperGeo(k, slotH, addSlot, full, min(full, size.height))
}

private fun DrawScope.drawPaper(spec: PaperSpec, accent: Color) {
    val geo = geoOf(spec)
    val radius = CornerRadius(3.dp.toPx())
    drawRoundRect(PaperColor, size = Size(size.width, geo.paperH), cornerRadius = radius)
    drawRoundRect(Color.White.copy(alpha = 0.14f), size = Size(size.width, geo.paperH), cornerRadius = radius, style = Stroke(1.dp.toPx()))
    clipRect(0f, 0f, size.width, geo.paperH) {
        drawHeader(geo.k)
        for (i in 0 until spec.summary.firstCount) drawCell(spec, geo, i)
        if (geo.addSlot >= 0) drawAddSlot(spec, geo, accent)
        if (geo.fullH > size.height) drawFade()
    }
}

private fun DrawScope.drawHeader(k: Float) {
    val top = (ContactSheetLayout.MARGIN + 6) * k
    val left = ContactSheetLayout.MARGIN * k
    val h = max(2.dp.toPx(), 24 * k)
    drawRect(InkColor.copy(alpha = 0.8f), Offset(left, top), Size(size.width * 0.3f, h))
    drawRect(MutedColor.copy(alpha = 0.7f), Offset(left + size.width * 0.3f + 3.dp.toPx(), top), Size(size.width * 0.06f, h))
}

private fun cellOrigin(spec: PaperSpec, geo: PaperGeo, slot: Int): Offset {
    val plan = spec.summary.plan
    val left = plan.cellLeft(slot % plan.columns) * geo.k
    val top = (ContactSheetLayout.MARGIN + ContactSheetLayout.HEADER + (slot / plan.columns) * (geo.slotH + ContactSheetLayout.GAP)) * geo.k
    return Offset(left, top)
}

private fun DrawScope.drawCell(spec: PaperSpec, geo: PaperGeo, index: Int) {
    val origin = cellOrigin(spec, geo, index)
    if (origin.y >= geo.paperH) return
    val plan = spec.summary.plan
    val w = plan.cellW * geo.k
    val h = plan.cellH * geo.k
    drawRect(CellColor, origin, Size(w, h))
    val thumb = spec.thumbs.getOrNull(index)
    if (thumb != null) drawThumb(thumb, origin, plan, geo.k) else drawPlaceholder(index, origin, Size(w, h))
    if (spec.captioned) drawCaption(origin.copy(y = origin.y + h), Size(w, plan.captionH * geo.k), plan.textSize * geo.k)
}

private fun DrawScope.drawThumb(thumb: ImageBitmap, origin: Offset, plan: ContactSheetLayout.Plan, k: Float) {
    val box = ContactSheetLayout.fit(thumb.width, thumb.height, plan.cellW, plan.cellH)
    val dst = IntOffset((origin.x + box[0] * k).roundToInt(), (origin.y + box[1] * k).roundToInt())
    val dstSize = IntSize(((box[2] - box[0]) * k).roundToInt().coerceAtLeast(1), ((box[3] - box[1]) * k).roundToInt().coerceAtLeast(1))
    drawImage(thumb, IntOffset.Zero, IntSize(thumb.width, thumb.height), dst, dstSize)
}

private fun DrawScope.drawPlaceholder(index: Int, origin: Offset, cell: Size) {
    val pair = Placeholders[(index * 5 + 2) % Placeholders.size]
    drawRect(Brush.linearGradient(listOf(pair.first, pair.second), origin, Offset(origin.x + cell.width, origin.y + cell.height)), origin, cell)
}

private fun DrawScope.drawCaption(origin: Offset, area: Size, textSize: Float) {
    val bar = max(1.5.dp.toPx(), textSize * 0.5f)
    val top = origin.y + max(0f, (area.height - bar) / 2f)
    val inset = 2.dp.toPx()
    drawRect(InkColor.copy(alpha = 0.85f), Offset(origin.x + inset, top), Size(area.width * 0.16f, bar))
    drawRect(MutedColor.copy(alpha = 0.75f), Offset(origin.x + area.width * 0.7f - inset, top), Size(area.width * 0.3f, bar))
}

private fun DrawScope.drawAddSlot(spec: PaperSpec, geo: PaperGeo, accent: Color) {
    val origin = cellOrigin(spec, geo, geo.addSlot)
    val plan = spec.summary.plan
    val box = Size(plan.cellW * geo.k, plan.cellH * geo.k)
    drawRect(accent.copy(alpha = 0.12f), origin, box)
    val dash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))
    drawRect(accent, origin, box, style = Stroke(1.5.dp.toPx(), pathEffect = dash))
    val c = Offset(origin.x + box.width / 2f, origin.y + box.height / 2f)
    val arm = min(box.width, box.height) * 0.18f
    val stroke = 2.dp.toPx()
    drawLine(accent, Offset(c.x - arm, c.y), Offset(c.x + arm, c.y), stroke, StrokeCap.Round)
    drawLine(accent, Offset(c.x, c.y - arm), Offset(c.x, c.y + arm), stroke, StrokeCap.Round)
}

private fun DrawScope.drawFade() {
    val height = 36.dp.toPx()
    drawRect(Brush.verticalGradient(listOf(PaperColor.copy(alpha = 0f), PaperColor), size.height - height, size.height), Offset(0f, size.height - height), Size(size.width, height))
}
