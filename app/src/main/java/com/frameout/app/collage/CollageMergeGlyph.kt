package com.frameout.app.collage

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.frameout.core.collage.BlendFamily
import com.frameout.core.collage.BlendLayout
import com.frameout.core.collage.CellBlend
import com.frameout.core.collage.CollageBlend
import com.frameout.core.collage.CollageTemplates

@Composable
internal fun BlendGlyph(blend: CollageBlend, tint: Color, modifier: Modifier) {
    val template = CollageTemplates.all.first { it.id == if (blend.family == BlendFamily.Cell) "2x2" else "1+2" }
    Canvas(modifier) {
        val side = size.minDimension
        val gap = if (blend.family == BlendFamily.None) 2.dp.toPx() else 1.6.dp.toPx()
        BlendLayout.compute(template, blend, blend.defaultStrength, side, side, gap).forEach { drawLook(it, tint, blend) }
    }
}

private fun DrawScope.drawLook(look: CellBlend, tint: Color, blend: CollageBlend) {
    val b = look.bounds
    val rect = Rect(b.left, b.top, b.right, b.bottom)
    val poly = look.polygon
    if (look.ramps.any) {
        drawFade(look, rect, tint)
    } else if (poly != null) {
        drawPath(polygonPath(poly, 0f, 0f), tint.copy(alpha = 0.9f))
    } else {
        val r = if (blend == CollageBlend.None) 3.dp.toPx() else 0f
        drawRoundRect(tint.copy(alpha = 0.9f), rect.topLeft, Size(rect.width, rect.height), androidx.compose.ui.geometry.CornerRadius(r))
    }
}

private fun DrawScope.drawFade(look: CellBlend, rect: Rect, tint: Color) {
    drawContext.canvas.saveLayer(rect, Paint())
    drawRect(tint.copy(alpha = 0.9f), rect.topLeft, rect.size)
    val clear = listOf(Color.Transparent, Color.Black)
    val r = look.ramps
    if (r.left > 0f) mask(Brush.horizontalGradient(clear, rect.left, rect.left + r.left), rect)
    if (r.top > 0f) mask(Brush.verticalGradient(clear, rect.top, rect.top + r.top), rect)
    if (r.right > 0f) mask(Brush.horizontalGradient(clear, rect.right, rect.right - r.right), rect)
    if (r.bottom > 0f) mask(Brush.verticalGradient(clear, rect.bottom, rect.bottom - r.bottom), rect)
    drawContext.canvas.restore()
}

private fun DrawScope.mask(brush: Brush, rect: Rect) {
    drawRect(brush, rect.topLeft, rect.size, blendMode = BlendMode.DstIn)
}

internal fun polygonPath(points: FloatArray, ox: Float, oy: Float): androidx.compose.ui.graphics.Path =
    androidx.compose.ui.graphics.Path().apply {
        moveTo(points[0] - ox, points[1] - oy)
        var i = 2
        while (i < points.size) {
            lineTo(points[i] - ox, points[i + 1] - oy)
            i += 2
        }
        close()
    }
