package com.frameout.app.viewer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.frameout.core.util.EditStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.a11y.frameSelectable
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.fieldFill

internal val HUE_COLORS = listOf(
    Color(0xFFFF0000), Color(0xFFFFFF00), Color(0xFF00FF00),
    Color(0xFF00FFFF), Color(0xFF0000FF), Color(0xFFFF00FF), Color(0xFFFF0000)
)

@Composable
internal fun SwatchRow(color: Int, onPick: (Int) -> Unit, compact: Boolean = false, onCustom: () -> Unit) {
    val custom = BRUSH_COLORS.none { it == color }
    val names = BrushColorNames.all()
    val cell = if (compact) 28.dp else 40.dp
    val dot = if (compact) 20.dp else 22.dp
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        BRUSH_COLORS.forEachIndexed { i, preset -> Swatch(preset, names[i], color == preset, Modifier.weight(1f), cell, dot) { onPick(preset) } }
        if (custom) Swatch(color, EditStrings.customColor(), true, Modifier.weight(1f), cell, dot) { onCustom() }
        RainbowButton(Modifier.weight(1f), cell, dot, onCustom)
    }
}

@Composable
private fun Swatch(color: Int, name: String, selected: Boolean, modifier: Modifier, cell: Dp, dot: Dp, onClick: () -> Unit) {
    val ring = if (selected) FrameTheme.colors.primary else FrameTheme.colors.outline
    Box(modifier.height(cell).frameSelectable(selected, Role.RadioButton, onClick).semantics { contentDescription = name }, Alignment.Center) {
        Box(Modifier.size(dot).clip(CircleShape).background(Color(color)).border(if (selected) 2.dp else 1.dp, ring, CircleShape))
    }
}

@Composable
private fun RainbowButton(modifier: Modifier, cell: Dp, dot: Dp, onClick: () -> Unit) {
    val rainbow = Brush.sweepGradient(HUE_COLORS)
    Box(modifier.height(cell).frameClickable(onClick = onClick).semantics { contentDescription = EditStrings.customColor() }, Alignment.Center) {
        Box(Modifier.size(dot).clip(CircleShape).background(FrameTheme.colors.fieldFill).border(2.dp, rainbow, CircleShape), Alignment.Center) {
            Image(painterResource(FrameIconsExtra.Palette), null, Modifier.size(10.dp), colorFilter = ColorFilter.tint(FrameTheme.colors.onSurface))
        }
    }
}

@Composable
internal fun BrushDot(color: Color, fraction: Float) {
    val diameter = (4f + 14f * fraction).dp
    Box(Modifier.size(diameter).clip(CircleShape).background(color).border(1.dp, FrameTheme.colors.outline, CircleShape))
}

internal object BrushColorNames {
    fun all(): List<String> = listOf(
        EditStrings.red(), EditStrings.yellow(), EditStrings.green(), EditStrings.blue(), EditStrings.white(), EditStrings.black()
    )
}
