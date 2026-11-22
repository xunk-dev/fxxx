package com.frameout.app.collage

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.core.collage.CellBlend
import com.frameout.core.collage.Ramps
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.core.util.safeCoerceIn

internal fun polygonShape(look: CellBlend?): Shape? {
    val poly = look?.polygon ?: return null
    val ox = look.bounds.left
    val oy = look.bounds.top
    return GenericShape { _, _ -> addPath(polygonPath(poly, ox, oy)) }
}

internal fun Modifier.rampMask(r: Ramps): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val clear = listOf(Color.Transparent, Color.Black)
        if (r.left > 0f) drawRect(Brush.horizontalGradient(clear, 0f, r.left), blendMode = BlendMode.DstIn)
        if (r.top > 0f) drawRect(Brush.verticalGradient(clear, 0f, r.top), blendMode = BlendMode.DstIn)
        if (r.right > 0f) drawRect(Brush.horizontalGradient(clear, size.width, size.width - r.right), blendMode = BlendMode.DstIn)
        if (r.bottom > 0f) drawRect(Brush.verticalGradient(clear, size.height, size.height - r.bottom), blendMode = BlendMode.DstIn)
    }

@Composable
internal fun EmptySlot() {
    val tint = FrameTheme.colors.onSurfaceVariant
    Canvas(Modifier.fillMaxSize()) {
        val inset = 6.dp.toPx()
        val dashed = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
        drawRoundRect(
            tint.copy(alpha = 0.28f), Offset(inset, inset), Size(size.width - inset * 2, size.height - inset * 2),
            CornerRadius(8.dp.toPx()), style = Stroke(1.2.dp.toPx(), pathEffect = dashed)
        )
        val c = Offset(size.width / 2f, size.height / 2f)
        val arm = minOf(size.width, size.height) * 0.1f
        val w = 1.6.dp.toPx()
        drawLine(tint.copy(alpha = 0.4f), Offset(c.x - arm, c.y), Offset(c.x + arm, c.y), w, StrokeCap.Round)
        drawLine(tint.copy(alpha = 0.4f), Offset(c.x, c.y - arm), Offset(c.x, c.y + arm), w, StrokeCap.Round)
    }
}

@Composable
internal fun NumberBadge(n: Int) {
    Box(Modifier.padding(6.dp).defaultMinSize(minWidth = 20.dp, minHeight = 20.dp).background(Color.Black.copy(alpha = 0.55f), CircleShape).padding(horizontal = 4.dp), contentAlignment = Alignment.Center) {
        FrameText("$n", style = FrameTheme.typography.label.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold), color = Color.White)
    }
}

@Composable
internal fun BoxScope.StampPill(text: String, unit: Float, cellWidth: Float, padX: Float = 0f, padY: Float = 0f) {
    val density = LocalDensity.current
    val fontPx = (cellWidth * 0.07f).safeCoerceIn(22f * unit, 44f * unit)
    Box(
        Modifier
            .align(Alignment.BottomStart)
            .padding(start = with(density) { padX.toDp() }, bottom = with(density) { padY.toDp() })
            .padding(with(density) { (fontPx * 0.6f).toDp() })
            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
            .padding(horizontal = with(density) { (fontPx * 0.55f).toDp() }, vertical = with(density) { (fontPx * 0.32f).toDp() })
    ) {
        val font = FrameTheme.typography.label.copy(fontSize = with(density) { fontPx.toSp() }, fontWeight = FontWeight.SemiBold, lineHeight = with(density) { (fontPx * 1.1f).toSp() })
        FrameText(text, style = font, color = Color.White, maxLines = 1)
    }
}
