package com.frameout.app.settings

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

private enum class Glyph { Person, Car, Paw }

private const val CELLS = 5

private val Kept: Map<Int, Glyph> = mapOf(0 to Glyph.Person, 2 to Glyph.Car, 4 to Glyph.Paw)

internal fun DrawScope.drawContent(pick: Color, dim: Color, ink: Color) {
    val pad = 10.dp.toPx()
    val gap = 5.dp.toPx()
    val w = minOf((size.width - pad * 2 - gap * (CELLS - 1)) / CELLS, 40.dp.toPx())
    val keptH = minOf(size.height - 8.dp.toPx(), 46.dp.toPx())
    val idleH = keptH * 0.62f
    val startX = (size.width - (w * CELLS + gap * (CELLS - 1))) / 2f
    for (i in 0 until CELLS) {
        val x = startX + i * (w + gap)
        val glyph = Kept[i]
        if (glyph != null) keptCard(glyph, x, (size.height - keptH) / 2f, w, keptH, pick, ink)
        else idleCard(x, (size.height - idleH) / 2f, w, idleH, dim, ink)
    }
}

private fun DrawScope.idleCard(x: Float, y: Float, w: Float, h: Float, dim: Color, ink: Color) {
    drawRoundRect(dim.copy(alpha = 0.16f), Offset(x, y), Size(w, h), CornerRadius(5.dp.toPx()))
    val line = y + h * 0.5f
    drawLine(ink.copy(alpha = 0.35f), Offset(x + w * 0.28f, line), Offset(x + w * 0.72f, line), 1.5.dp.toPx(), StrokeCap.Round)
}

private fun DrawScope.keptCard(glyph: Glyph, x: Float, y: Float, w: Float, h: Float, pick: Color, ink: Color) {
    val corner = CornerRadius(5.dp.toPx())
    drawRoundRect(pick.copy(alpha = 0.18f), Offset(x, y), Size(w, h), corner)
    drawRoundRect(pick, Offset(x, y), Size(w, h), corner, style = Stroke(1.5.dp.toPx()))
    val cx = x + w / 2f
    when (glyph) {
        Glyph.Person -> drawPerson(cx, x, y, w, h, pick)
        Glyph.Car -> drawCar(cx, y, w, h, pick, ink)
        Glyph.Paw -> drawPaw(cx, y, w, h, pick)
    }
}

private fun DrawScope.drawPerson(cx: Float, x: Float, y: Float, w: Float, h: Float, pick: Color) {
    drawCircle(pick, w * 0.17f, Offset(cx, y + h * 0.34f))
    drawRoundRect(pick, Offset(cx - w * 0.28f, y + h * 0.52f), Size(w * 0.56f, h * 0.28f), CornerRadius(w * 0.22f))
}

private fun DrawScope.drawCar(cx: Float, y: Float, w: Float, h: Float, pick: Color, ink: Color) {
    val bodyW = w * 0.72f
    val bx = cx - bodyW / 2f
    val top = y + h * 0.36f
    val deck = y + h * 0.52f
    val cabin = Path().apply {
        moveTo(bx + bodyW * 0.18f, deck)
        lineTo(bx + bodyW * 0.30f, top)
        lineTo(bx + bodyW * 0.72f, top)
        lineTo(bx + bodyW * 0.86f, deck)
        close()
    }
    drawPath(cabin, pick)
    drawRoundRect(pick, Offset(bx, deck - 1.dp.toPx()), Size(bodyW, h * 0.18f), CornerRadius(3.dp.toPx()))
    val wheel = maxOf(w * 0.095f, 2.2.dp.toPx())
    val wy = deck + h * 0.19f
    drawCircle(ink.copy(alpha = 0.9f), wheel, Offset(bx + bodyW * 0.26f, wy))
    drawCircle(ink.copy(alpha = 0.9f), wheel, Offset(bx + bodyW * 0.74f, wy))
}

private fun DrawScope.drawPaw(cx: Float, y: Float, w: Float, h: Float, pick: Color) {
    drawOval(pick, Offset(cx - w * 0.22f, y + h * 0.52f), Size(w * 0.44f, h * 0.24f))
    val toe = w * 0.08f
    drawCircle(pick, toe, Offset(cx - w * 0.24f, y + h * 0.46f))
    drawCircle(pick, toe, Offset(cx - w * 0.09f, y + h * 0.36f))
    drawCircle(pick, toe, Offset(cx + w * 0.09f, y + h * 0.36f))
    drawCircle(pick, toe, Offset(cx + w * 0.24f, y + h * 0.46f))
}
