package com.frameout.app.viewer

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.frameout.core.edit.AdjustMatrix
import com.frameout.core.edit.BrushScale
import com.frameout.core.edit.CoverStyle
import com.frameout.core.edit.CropPreset
import com.frameout.core.edit.StrokeMark
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.components.FrameText
import com.frameout.core.util.EditStrings
import com.frameout.core.util.ShapeStrings
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameTheme
import kotlin.math.abs
import kotlin.math.roundToInt

internal data class ImageDims(val w: Int, val h: Int)

@Composable
internal fun RowIcon(icon: Int) {
    Image(painterResource(icon), null, Modifier.size(20.dp), colorFilter = ColorFilter.tint(FrameTheme.colors.onSurfaceVariant))
}

private fun signed(n: Int): String = if (n > 0) "+$n" else "$n"

private class AdjustParam(
    val icon: Int, val label: () -> String, val lo: Float, val hi: Float, val neutral: Float,
    val get: (EditDraft) -> Float, val set: (ViewerToolEditor, Float) -> Unit
)

private val ADJUST_PARAMS = listOf(
    AdjustParam(FrameIconsExtra.Sun, EditStrings::brightness, AdjustMatrix.BRIGHTNESS_MIN, AdjustMatrix.BRIGHTNESS_MAX, 0f, { it.brightness }, { t, v -> t.setBrightness(v) }),
    AdjustParam(FrameIconsExtra.Contrast, EditStrings::contrast, AdjustMatrix.CONTRAST_MIN, AdjustMatrix.CONTRAST_MAX, 1f, { it.contrast }, { t, v -> t.setContrast(v) }),
    AdjustParam(FrameIconsExtra.Blur, EditStrings::saturation, AdjustMatrix.SATURATION_MIN, AdjustMatrix.SATURATION_MAX, 1f, { it.saturation }, { t, v -> t.setSaturation(v) })
)

@Composable
internal fun adjustSections(draft: EditDraft, tools: ViewerToolEditor): List<PanelSection> = listOf(
    PanelSection(FrameIconsExtra.Sun, EditStrings.adjust(), modified = draft.adjusted) {
        ADJUST_PARAMS.forEach { param ->
            val value = param.get(draft)
            CompactSliderRow(
                label = param.label(),
                valueText = signed(((value - param.neutral) * 100f).roundToInt()),
                fraction = (value - param.lo) / (param.hi - param.lo),
                onFraction = { param.set(tools, param.lo + it * (param.hi - param.lo)) },
                onStep = { dir -> param.set(tools, (((value * 100f).roundToInt() + dir) / 100f).coerceIn(param.lo, param.hi)) },
                dense = true,
                leading = { RowIcon(param.icon) }
            )
        }
    }
)

@Composable
internal fun drawSections(draft: EditDraft, dims: ImageDims, tools: ViewerToolEditor): List<PanelSection> {
    if (draft.drawMode == DrawMode.Shapes) return shapeSections(draft, dims, tools)
    val lo = BrushScale.DRAW_MIN
    val hi = BrushScale.DRAW_MAX
    val fraction = BrushScale.toFraction(draft.size, lo, hi)
    val opacityLo = StrokeMark.MIN_OPACITY
    return listOf(
        PanelSection(FrameIconsExtra.Brush, EditStrings.draw()) {
            CompactSliderRow(
                label = EditStrings.size(),
                valueText = "${BrushScale.pixels(draft.size, dims.w, dims.h)} px",
                fraction = fraction,
                onFraction = { tools.setSize(BrushScale.fromFraction(it, lo, hi)) },
                onStep = { dir -> tools.setSize(BrushScale.stepPixels(draft.size, dir, dims.w, dims.h, lo, hi)) },
                dense = true,
                leading = { BrushDot(Color(draft.color), fraction) }
            )
            CompactSliderRow(
                label = EditStrings.opacity(),
                valueText = "${(draft.brushOpacity * 100f).roundToInt()}%",
                fraction = ((draft.brushOpacity - opacityLo) / (1f - opacityLo)).coerceIn(0f, 1f),
                onFraction = { tools.setBrushOpacity(opacityLo + it * (1f - opacityLo)) },
                onStep = { dir -> tools.setBrushOpacity(((draft.brushOpacity * 100f).roundToInt() + dir * 5) / 100f) },
                dense = true,
                leading = { RowIcon(FrameIconsExtra.Contrast) }
            )
            SwatchRow(draft.color, tools::setColor, compact = true) { tools.setPickerOpen(true) }
        }
    )
}

@Composable
internal fun coverSections(draft: EditDraft, dims: ImageDims, tools: ViewerToolEditor): List<PanelSection> {
    val gate = com.frameout.app.billing.rememberProGate()
    val sizeLo = BrushScale.COVER_MIN
    val sizeHi = BrushScale.COVER_MAX
    val sizeFraction = BrushScale.toFraction(draft.coverSize, sizeLo, sizeHi)
    return listOf(
        PanelSection(FrameIconsExtra.Blur, "${EditStrings.tabStyle()} · ${ShapeStrings.title()}") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                CoverShapeChoice(draft, tools, Modifier.weight(2f))
                CoverStyleChoice(draft, tools, gate, Modifier.weight(3f))
            }
            CoverStrengthRow(draft, tools)
            CoverSizeRow(draft, dims, tools, sizeFraction, sizeLo, sizeHi)
        }
    )
}

@Composable
private fun CoverShapeChoice(draft: EditDraft, tools: ViewerToolEditor, modifier: Modifier = Modifier) {
    LabeledSegmentedChoice(
        listOf(
            ChoiceOption(FrameIconsExtra.Brush, EditStrings.pen(), draft.shape == CoverShape.Pen) { tools.setShape(CoverShape.Pen) },
            ChoiceOption(FrameIconsExtra.RectSelect, EditStrings.rectangle(), draft.shape == CoverShape.Rect) { tools.setShape(CoverShape.Rect) }
        ),
        modifier
    )
}

@Composable
private fun CoverStyleChoice(draft: EditDraft, tools: ViewerToolEditor, gate: com.frameout.app.billing.ProGate, modifier: Modifier = Modifier) {
    LabeledSegmentedChoice(
        CoverStyle.entries.sortedBy { com.frameout.core.pro.ProCatalog.coverTier(it).ordinal }.map { style ->
            val tier = com.frameout.core.pro.ProCatalog.coverTier(style)
            val icon = when (style) {
                CoverStyle.Solid -> FrameIconsExtra.Solid
                CoverStyle.Blur -> FrameIconsExtra.Blur
                CoverStyle.Pixelate -> FrameIconsExtra.Pixelate
            }
            val label = when (style) {
                CoverStyle.Solid -> EditStrings.solid()
                CoverStyle.Blur -> EditStrings.blur()
                CoverStyle.Pixelate -> EditStrings.pixelate()
            }
            val shown = if (gate.locked(com.frameout.core.pro.ProFeature.EditTools, tier)) tier else com.frameout.core.pro.ProTier.Free
            ChoiceOption(icon, label, draft.style == style, shown) { tools.setStyle(style) }
        },
        modifier
    )
}

@Composable
private fun CoverStrengthRow(draft: EditDraft, tools: ViewerToolEditor) {
    CompactSliderRow(
        label = EditStrings.strength(),
        valueText = "${(draft.strength * 100f).roundToInt()}%",
        fraction = draft.strength,
        onFraction = tools::setStrength,
        onStep = { dir -> tools.setStrength((((draft.strength * 100f).roundToInt() + dir) / 100f).coerceIn(0f, 1f)) },
        enabled = draft.style != CoverStyle.Solid,
        dense = true,
        showLabel = true,
        leading = { RowIcon(FrameIconsExtra.Contrast) }
    )
}

@Composable
private fun CoverSizeRow(draft: EditDraft, dims: ImageDims, tools: ViewerToolEditor, sizeFraction: Float, sizeLo: Float, sizeHi: Float) {
    CompactSliderRow(
        label = EditStrings.size(),
        valueText = "${BrushScale.pixels(draft.coverSize, dims.w, dims.h)} px",
        fraction = sizeFraction,
        onFraction = { tools.setCoverSize(BrushScale.fromFraction(it, sizeLo, sizeHi)) },
        onStep = { dir -> tools.setCoverSize(BrushScale.stepPixels(draft.coverSize, dir, dims.w, dims.h, sizeLo, sizeHi)) },
        enabled = draft.shape == CoverShape.Pen,
        dense = true,
        showLabel = true,
        leading = { BrushDot(FrameTheme.colors.onSurface, sizeFraction) }
    )
}

internal fun cropSections(state: ViewerState, tools: ViewerToolEditor): List<PanelSection> =
    listOf(PanelSection(com.frameout.designsystem.icons.FrameIcons.Crop, EditStrings.cropRotate()) { CropRotateControls(state, tools) })

private const val RATIO_TOLERANCE = 0.02f

@Composable
internal fun CropRotateControls(state: ViewerState, tools: ViewerToolEditor) {
    TransformRow(state.draft, tools)
    RatioRow(state, tools)
}

@Composable
private fun TransformRow(draft: EditDraft, tools: ViewerToolEditor) {
    val swapped = GeometryMath.swaps(draft.rotation)
    val mirroredAcross = if (swapped) draft.flipV else draft.flipH
    val mirroredDown = if (swapped) draft.flipH else draft.flipV
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ActionTile(FrameIconsExtra.Rotate, EditStrings.rotateLeft(), Modifier.weight(1f), mirror = true, iconOnly = true) { tools.rotate(-90) }
        ActionTile(FrameIconsExtra.Rotate, EditStrings.rotateRight(), Modifier.weight(1f), iconOnly = true) { tools.rotate(90) }
        ActionTile(FrameIconsExtra.FlipH, FrameStrings.flipH(), Modifier.weight(1f), selected = mirroredAcross, iconOnly = true) { tools.flipHorizontal() }
        ActionTile(FrameIconsExtra.FlipV, FrameStrings.flipV(), Modifier.weight(1f), selected = mirroredDown, iconOnly = true) { tools.flipVertical() }
    }
}

@Composable
private fun RatioRow(state: ViewerState, tools: ViewerToolEditor) {
    val shown = state.bitmap
    val rect = state.displayCrop()
    val widthPx = ((rect.r - rect.l) * (shown?.width ?: 0)).roundToInt()
    val heightPx = ((rect.b - rect.t) * (shown?.height ?: 0)).roundToInt()
    val ratio = if (heightPx > 0) widthPx.toFloat() / heightPx else 0f
    val matched = CropPreset.entries.firstOrNull { it != CropPreset.Free && abs(ratio - it.w.toFloat() / it.h) <= it.w.toFloat() / it.h * RATIO_TOLERANCE }
    val active = if (!state.hasCrop) CropPreset.Free else matched
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CropPreset.entries.forEach { preset ->
            ChoiceChip(
                label = if (preset == CropPreset.Free) FrameStrings.cropFree() else preset.label,
                selected = preset == active,
                modifier = Modifier.weight(1f).heightIn(min = 40.dp)
            ) { tools.setCropRatio(preset) }
        }
    }
}
