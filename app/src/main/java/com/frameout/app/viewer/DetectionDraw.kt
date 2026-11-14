package com.frameout.app.viewer

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val IDLE_ALPHA = 0.55f
private const val SELECTED_FILL = 0.14f

internal fun DrawScope.drawDetection(rect: DetectionRect, color: Color, selected: Boolean) {
    val radius = CornerRadius(6.dp.toPx())
    val origin = Offset(rect.left, rect.top)
    val size = Size(rect.width, rect.height)
    if (selected) drawRoundRect(color.copy(alpha = SELECTED_FILL), origin, size, radius)
    val stroke = (if (selected) 2.5.dp else 1.5.dp).toPx()
    val tint = if (selected) color else color.copy(alpha = IDLE_ALPHA)
    drawRoundRect(tint, origin, size, radius, Stroke(stroke))
}

internal fun DrawScope.drawDetectionLabel(
    rect: DetectionRect,
    color: Color,
    ink: Color,
    label: String,
    measurer: TextMeasurer,
    areaWidth: Float,
    selected: Boolean
) {
    val padX = 6.dp.toPx()
    val padY = 3.dp.toPx()
    val maxWidth = rect.width.toInt().coerceAtLeast(90.dp.roundToPx()).coerceAtMost((areaWidth - padX * 2).toInt().coerceAtLeast(1))
    val text = measurer.measure(
        label,
        TextStyle(color = ink, fontSize = 11.sp),
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        constraints = Constraints(maxWidth = maxWidth)
    )
    val chipH = text.size.height + padY * 2
    val chipTop = if (rect.top - chipH >= 0f) rect.top - chipH else rect.top
    val chipSize = Size(text.size.width + padX * 2, chipH)
    val chipLeft = rect.left.coerceIn(0f, (areaWidth - chipSize.width).coerceAtLeast(0f))
    drawRoundRect(color.copy(alpha = if (selected) 1f else 0.9f), Offset(chipLeft, chipTop), chipSize, CornerRadius(5.dp.toPx()))
    drawText(text, topLeft = Offset(chipLeft + padX, chipTop + padY))
}
