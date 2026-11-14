package com.frameout.app.viewer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.FrameSlider
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.frameout.core.edit.BrushScale
import com.frameout.core.edit.TextMark
import com.frameout.core.util.EditStrings
import com.frameout.designsystem.a11y.frameSelectable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.fieldFill
import kotlin.math.abs
import kotlin.math.roundToInt

private val DARK_BACKGROUND = 0xCC000000.toInt()
private val LIGHT_BACKGROUND = 0xCCFFFFFF.toInt()
private const val MIN_OPACITY = 0.05f
private const val ROTATION_SNAP = 3f

internal fun pickerColorFor(draft: EditDraft): Int {
    val mark = draft.selectedText ?: draft.text
    return when (draft.pickerTarget) {
        ColorTarget.Brush -> draft.color
        ColorTarget.TextFill -> mark.color
        ColorTarget.TextOutline -> mark.outlineColor
        ColorTarget.TextBackground -> if (android.graphics.Color.alpha(mark.background) > 0) mark.background else 0xFFFFFFFF.toInt()
    }
}

@Composable
internal fun textSections(draft: EditDraft, dims: ImageDims, tools: ViewerToolEditor): List<PanelSection> {
    val mark = draft.selectedText ?: draft.text
    return listOf(
        PanelSection(FrameIconsExtra.Text, EditStrings.text()) {
            TextInputRow(draft, tools)
            StyleChips(mark, tools)
        },
        PanelSection(FrameIconsExtra.Palette, EditStrings.color(), modified = android.graphics.Color.alpha(mark.background) > 0) {
            TextColorRow(mark, tools)
            TextBackgroundRow(mark, tools)
        },
        PanelSection(FrameIconsExtra.Blur, "${EditStrings.shadow()} · ${EditStrings.outline()}", modified = mark.outline > 0f || mark.opacity < 1f) {
            TextEffectsGrid(mark, dims, tools)
        }
    )
}

@Composable
private fun TextEffectsGrid(mark: TextMark, dims: ImageDims, tools: ViewerToolEditor) {
    val sizeLo = TextMark.MIN_SIZE
    val sizeHi = TextMark.MAX_SIZE
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SliderCell(
            FrameIconsExtra.Blur, EditStrings.shadow(), "${(mark.shadow * 100f).roundToInt()}%", mark.shadow,
            { f -> tools.editText { it.copy(shadow = f) } }, Modifier.weight(1f)
        )
        SliderCell(
            FrameIconsExtra.Contrast, EditStrings.opacity(), "${(mark.opacity * 100f).roundToInt()}%",
            ((mark.opacity - MIN_OPACITY) / (1f - MIN_OPACITY)).coerceIn(0f, 1f),
            { f -> tools.editText { it.copy(opacity = MIN_OPACITY + f * (1f - MIN_OPACITY)) } }, Modifier.weight(1f)
        )
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SliderCell(
            FrameIconsExtra.RectSelect, EditStrings.outline(), "${(mark.outline * 100f).roundToInt()}%", mark.outline,
            { f -> tools.editText { it.copy(outline = f) } }, Modifier.weight(1f),
            leading = { ColorDot(mark.outlineColor, EditStrings.outline()) { tools.openPicker(ColorTarget.TextOutline) } }
        )
        SliderCell(
            FrameIconsExtra.Fit, EditStrings.size(), "${BrushScale.pixels(mark.size, dims.w, dims.h)} px", BrushScale.toFraction(mark.size, sizeLo, sizeHi),
            { f -> tools.editText { it.copy(size = BrushScale.fromFraction(f, sizeLo, sizeHi)) } }, Modifier.weight(1f)
        )
    }
}

@Composable
private fun SliderCell(
    icon: Int,
    label: String,
    valueText: String,
    fraction: Float,
    onFraction: (Float) -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(24.dp), Alignment.Center) { if (leading != null) leading() else RowIcon(icon) }
        FrameSlider(fraction, onFraction, 0f, 1f, label, Modifier.weight(1f).height(30.dp))
        FrameText(
            valueText, style = FrameTheme.typography.monoSmall.copy(fontSize = 10.sp), color = FrameTheme.colors.primary,
            maxLines = 1, softWrap = false, textAlign = TextAlign.End, modifier = Modifier.width(42.dp)
        )
    }
}

@Composable
private fun ColorDot(color: Int, label: String, onClick: () -> Unit) {
    Box(Modifier.size(24.dp).frameClickable(onClick = onClick).semantics { contentDescription = label }, Alignment.Center) {
        Box(Modifier.size(18.dp).clip(CircleShape).background(Color(color)).border(1.5.dp, FrameTheme.colors.primary, CircleShape))
    }
}

@Composable
private fun TextColorRow(mark: TextMark, tools: ViewerToolEditor) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(24.dp), Alignment.Center) { RowIcon(FrameIconsExtra.Palette) }
        Box(Modifier.weight(1f)) { SwatchRow(mark.color, { c -> tools.editText { it.copy(color = c) } }, compact = true) { tools.openPicker(ColorTarget.TextFill) } }
    }
}

@Composable
private fun TextBackgroundRow(mark: TextMark, tools: ViewerToolEditor) {
    val none = android.graphics.Color.alpha(mark.background) == 0
    val dark = mark.background == DARK_BACKGROUND
    val light = mark.background == LIGHT_BACKGROUND
    val custom = !none && !dark && !light
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(24.dp), Alignment.Center) { RowIcon(FrameIconsExtra.Solid) }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            BackgroundChip(EditStrings.backgroundNone(), none, Modifier.weight(1f), null) { tools.editText { it.copy(background = 0) } }
            BackgroundChip(EditStrings.backgroundDark(), dark, Modifier.weight(1f), DARK_BACKGROUND) { tools.editText { it.copy(background = DARK_BACKGROUND) } }
            BackgroundChip(EditStrings.backgroundLight(), light, Modifier.weight(1f), LIGHT_BACKGROUND) { tools.editText { it.copy(background = LIGHT_BACKGROUND) } }
            BackgroundChip(EditStrings.backgroundCustom(), custom, Modifier.weight(1f), if (custom) mark.background else null, rainbow = !custom) {
                tools.openPicker(ColorTarget.TextBackground)
            }
        }
    }
}

@Composable
private fun BackgroundChip(label: String, selected: Boolean, modifier: Modifier, fill: Int?, rainbow: Boolean = false, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    Box(
        modifier.height(30.dp).clip(FrameShapes.sm)
            .background(if (selected) colors.primary.copy(alpha = 0.18f) else colors.fieldFill)
            .border(1.dp, if (selected) colors.primary.copy(alpha = 0.45f) else Color.Transparent, FrameShapes.sm)
            .frameSelectable(selected, Role.RadioButton, onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        when {
            rainbow -> Box(Modifier.size(18.dp).clip(CircleShape).border(2.dp, Brush.sweepGradient(HUE_COLORS), CircleShape))
            fill == null -> Image(painterResource(FrameIconsExtra.Cancel), null, Modifier.size(16.dp), colorFilter = ColorFilter.tint(if (selected) colors.primary else colors.onSurfaceVariant))
            else -> Box(Modifier.size(18.dp).clip(CircleShape).background(Color(fill)).border(1.dp, colors.outline, CircleShape))
        }
    }
}

@Composable
private fun TextInputRow(draft: EditDraft, tools: ViewerToolEditor) {
    val colors = FrameTheme.colors
    val focus = remember { FocusRequester() }
    val selectedText = draft.selectedText
    val content = selectedText?.text.orEmpty()
    var field by remember(draft.selected) { mutableStateOf(TextFieldValue(content, TextRange(content.length))) }
    val hint = EditStrings.textHint()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BasicTextField(
            value = field,
            onValueChange = { field = it; tools.setTextContent(it.text) },
            textStyle = FrameTheme.typography.body.copy(color = colors.onSurface),
            cursorBrush = SolidColor(colors.primary),
            maxLines = 3,
            modifier = Modifier.weight(1f).focusRequester(focus).semantics { contentDescription = hint },
            decorationBox = { inner ->
                Box(
                    Modifier.fillMaxWidth().heightIn(min = 38.dp).clip(FrameShapes.md).background(colors.fieldFill)
                        .border(1.dp, colors.outline, FrameShapes.md).padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (field.text.isEmpty()) FrameText(hint, style = FrameTheme.typography.body, color = colors.onSurfaceVariant)
                    inner()
                }
            }
        )
        PanelIconButton(FrameIconsExtra.Plus, EditStrings.textAdd()) {
            tools.addText()
            focus.requestFocus()
        }
        PanelIconButton(FrameIcons.Trash, EditStrings.textDelete(), enabled = selectedText != null) { tools.deleteSelectedText() }
    }
}

@Composable
private fun StyleChips(mark: TextMark, tools: ViewerToolEditor) {
    val bold = EditStrings.bold()
    val italic = EditStrings.italic()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        StyleChip("B", bold, mark.bold, Modifier.weight(1f), FontFamily.SansSerif, FontWeight.Bold, FontStyle.Normal) {
            tools.editText { it.copy(bold = !it.bold) }
        }
        StyleChip("I", italic, mark.italic, Modifier.weight(1f), FontFamily.Serif, FontWeight.Normal, FontStyle.Italic) {
            tools.editText { it.copy(italic = !it.italic) }
        }
        listOf(
            Triple(TextMark.FONT_SANS, EditStrings.fontSans(), FontFamily.SansSerif),
            Triple(TextMark.FONT_SERIF, EditStrings.fontSerif(), FontFamily.Serif),
            Triple(TextMark.FONT_MONO, EditStrings.fontMono(), FontFamily.Monospace),
            Triple(TextMark.FONT_CURSIVE, EditStrings.fontCursive(), FontFamily.Cursive)
        ).forEach { (font, label, family) ->
            StyleChip("Aa", label, mark.font == font, Modifier.weight(1f), family, FontWeight.Normal, FontStyle.Normal) {
                tools.editText { it.copy(font = font) }
            }
        }
    }
}

@Composable
private fun StyleChip(
    glyph: String,
    label: String,
    selected: Boolean,
    modifier: Modifier,
    family: FontFamily,
    weight: FontWeight,
    fontStyle: FontStyle,
    onClick: () -> Unit
) {
    val colors = FrameTheme.colors
    val tint = if (selected) colors.primary else colors.onSurfaceVariant
    Box(
        modifier.height(30.dp).clip(FrameShapes.sm)
            .background(if (selected) colors.primary.copy(alpha = 0.18f) else colors.fieldFill)
            .border(1.dp, if (selected) colors.primary.copy(alpha = 0.45f) else Color.Transparent, FrameShapes.sm)
            .frameSelectable(selected, Role.Checkbox, onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        FrameText(
            glyph,
            style = FrameTheme.typography.body.copy(fontFamily = family, fontWeight = weight, fontStyle = fontStyle, fontSize = 15.sp, lineHeight = 18.sp),
            color = tint, maxLines = 1
        )
    }
}
