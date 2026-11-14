package com.frameout.app.viewer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import com.frameout.app.billing.TierLockCorner
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.frameout.core.edit.BrushScale
import com.frameout.core.edit.ShapeFill
import com.frameout.core.edit.ShapeKind
import com.frameout.core.edit.ShapeMark
import com.frameout.core.edit.ShapePainter
import com.frameout.core.util.EditStrings
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.ShapeStrings
import com.frameout.app.billing.rememberProGate
import com.frameout.core.pro.ProCatalog
import com.frameout.core.pro.ProFeature
import com.frameout.core.pro.ProTier
import androidx.compose.runtime.remember
import com.frameout.designsystem.a11y.frameSelectable
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameControl
import kotlin.math.roundToInt

private const val DISABLED_ALPHA = 0.38f
private const val OPACITY_STEP = 5

@Composable
internal fun shapeSections(draft: EditDraft, dims: ImageDims, tools: ViewerToolEditor): List<PanelSection> {
    val shapes = tools.shapes
    val widthLo = ShapeMark.MIN_WIDTH
    val widthHi = ShapeMark.MAX_WIDTH
    val widthFraction = BrushScale.toFraction(draft.shapeWidth, widthLo, widthHi)
    val opacityLo = ShapeMark.MIN_OPACITY
    val opacity = draft.shapeOpacity
    return listOf(
        PanelSection(FrameIconsExtra.Shapes, ShapeStrings.title(), modified = draft.shapeFill != ShapeFill.None) {
            ShapePalette(draft, tools)
            SwatchRow(draft.color, tools::setColor, compact = true) { tools.setPickerOpen(true) }
        },
        PanelSection(FrameIconsExtra.Fit, "${EditStrings.size()} · ${EditStrings.opacity()}") {
            CompactSliderRow(
                label = EditStrings.size(),
                valueText = "${BrushScale.pixels(draft.shapeWidth, dims.w, dims.h)} px",
                fraction = widthFraction,
                onFraction = { shapes.setWidth(BrushScale.fromFraction(it, widthLo, widthHi)) },
                onStep = { dir -> shapes.setWidth(BrushScale.stepPixels(draft.shapeWidth, dir, dims.w, dims.h, widthLo, widthHi)) },
                dense = true,
                leading = { BrushDot(Color(draft.color), widthFraction) }
            )
            CompactSliderRow(
                label = EditStrings.opacity(),
                valueText = "${(opacity * 100f).roundToInt()}%",
                fraction = (opacity - opacityLo) / (1f - opacityLo),
                onFraction = { shapes.setOpacity(opacityLo + it * (1f - opacityLo)) },
                onStep = { dir -> shapes.setOpacity(((opacity * 100f).roundToInt() + dir * OPACITY_STEP) / 100f) },
                dense = true,
                leading = { RowIcon(FrameIconsExtra.Contrast) }
            )
        }
    )
}

@Composable
private fun ShapePalette(draft: EditDraft, tools: ViewerToolEditor) {
    val shapes = tools.shapes
    val gate = rememberProGate()
    val kinds = remember { ProCatalog.orderedShapes() }
    val selected = draft.shapeKind
    val dashEnabled = selected.supportsDash
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), Arrangement.spacedBy(4.dp), Alignment.CenterVertically) {
        IconToggle(FrameIconsExtra.Dash, ShapeStrings.dashed(), draft.shapeDash && dashEnabled, dashEnabled) { shapes.setDash(!draft.shapeDash) }
        FillToggle(draft, tools)
        Box(Modifier.width(1.dp).height(22.dp).background(FrameTheme.colors.divider))
        kinds.forEach { kind ->
            val tier = ProCatalog.shapeTier(kind)
            ShapeTile(kind, kind == selected, tier, gate.locked(ProFeature.Shapes, tier)) { shapes.setKind(kind) }
        }
    }
}

@Composable
private fun ShapeTile(kind: ShapeKind, selected: Boolean, tier: ProTier, locked: Boolean, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    val tint = if (selected) colors.primary else colors.onSurface
    val label = ShapeStrings.name(kind)
    Box(
        Modifier.size(38.dp).pressScale()
            .frameControl(FrameShapes.sm, selected = selected, glowAlpha = 0f, cornerFraction = 0.3f)
            .clip(FrameShapes.sm)
            .frameSelectable(selected, Role.RadioButton, onClick)
            .semantics { contentDescription = label },
        Alignment.Center
    ) {
        Canvas(Modifier.size(22.dp)) {
            drawIntoCanvas { ShapePainter.paint(it.nativeCanvas, iconMark(kind, tint.toArgb()), size.width, size.height, null, size.width, true) }
        }
        TierLockCorner(tier, locked)
    }
}

private fun iconMark(kind: ShapeKind, color: Int): ShapeMark {
    val width = 0.085f
    return when {
        kind.pointBased -> ShapeMark(kind, color, width, 0.14f, 0.84f, 0.86f, 0.18f)
        kind == ShapeKind.Callout -> ShapeMark(kind, color, width, 0.12f, 0.1f, 0.88f, 0.6f)
        kind == ShapeKind.Highlighter -> ShapeMark(kind, color, width, 0.1f, 0.3f, 0.9f, 0.7f)
        kind == ShapeKind.Magnifier -> ShapeMark(kind, color, width, 0.12f, 0.12f, 0.72f, 0.72f)
        kind.radial -> ShapeMark(kind, color, width, 0.2f, 0.2f, 0.8f, 0.8f)
        else -> ShapeMark(kind, color, width, 0.14f, 0.18f, 0.86f, 0.82f)
    }
}

@Composable
private fun FillToggle(draft: EditDraft, tools: ViewerToolEditor) {
    val fill = draft.shapeFill
    val (icon, label) = when (fill) {
        ShapeFill.None -> FrameIconsExtra.RectSelect to ShapeStrings.fillNone()
        ShapeFill.Soft -> FrameIconsExtra.Contrast to ShapeStrings.fillSoft()
        ShapeFill.Solid -> FrameIconsExtra.Solid to EditStrings.solid()
    }
    val next = when (fill) {
        ShapeFill.None -> ShapeFill.Soft
        ShapeFill.Soft -> ShapeFill.Solid
        ShapeFill.Solid -> ShapeFill.None
    }
    IconToggle(icon, "${EditStrings.background()}: $label", fill != ShapeFill.None, draft.shapeKind.supportsFill) { tools.shapes.setFill(next) }
}
