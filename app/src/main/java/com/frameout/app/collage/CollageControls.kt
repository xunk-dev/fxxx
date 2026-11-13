package com.frameout.app.collage

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.frameout.app.viewer.ChoiceChip
import com.frameout.app.viewer.StepButton
import com.frameout.app.viewer.SwatchRow
import com.frameout.app.viewer.ValueChip
import com.frameout.core.collage.COLLAGE_GAP_MAX
import com.frameout.core.collage.CollageAspect
import com.frameout.core.util.CollageStrings
import com.frameout.core.util.EditStrings
import com.frameout.designsystem.a11y.frameSelectable
import com.frameout.designsystem.components.FrameSlider
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameControl
import kotlin.math.roundToInt

@Composable
internal fun FramesTab(state: CollageState) {
    val max = COLLAGE_GAP_MAX
    val spacing = CollageStrings.spacing()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        GroupIcon(FrameIcons.GridSize)
        StepButton(FrameIconsExtra.Minus, EditStrings.decrease()) { state.gap = (state.gap - 1).coerceIn(0, max) }
        FrameSlider(state.gap / max.toFloat(), { state.gap = (it * max).roundToInt().coerceIn(0, max) }, 0f, 1f, spacing, Modifier.weight(1f).height(28.dp))
        StepButton(FrameIconsExtra.Plus, EditStrings.increase()) { state.gap = (state.gap + 1).coerceIn(0, max) }
        ValueChip("${state.gap}")
        Spacer(Modifier.width(6.dp))
        IconToggle(FrameIconsExtra.RectSelect, CollageStrings.corners(), state.rounded) { state.rounded = !state.rounded }
    }
}

@Composable
internal fun StyleTab(state: CollageState) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) { SwatchRow(state.bgColor, { state.bgColor = it }) { state.pickerOpen = true } }
        Spacer(Modifier.width(6.dp))
        IconToggle(FrameIconsExtra.Timer, CollageStrings.time(), state.stamps) { state.stamps = !state.stamps }
    }
}

@Composable
internal fun CanvasTab(state: CollageState) {
    val aspects = CollageAspect.entries
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        GroupIcon(FrameIcons.Crop)
        ScrollChoiceRow(
            listOf(CollageStrings.aspectAuto()) + aspects.map { CollageStrings.aspectName(it.key) },
            if (state.aspectAuto) 0 else aspects.indexOf(state.aspect) + 1,
            Modifier.weight(1f)
        ) { index ->
            state.aspectAuto = index == 0
            if (index > 0) state.aspect = aspects[index - 1]
        }
    }
}

@Composable
private fun GroupIcon(icon: Int) {
    Image(painterResource(icon), null, Modifier.size(20.dp), colorFilter = ColorFilter.tint(FrameTheme.colors.onSurfaceVariant))
}

@Composable
internal fun IconToggle(icon: Int, label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    val tint by animateColorAsState(if (selected) colors.primary else colors.onSurfaceVariant, tween(200), label = "toggleTint")
    val state = if (selected) CollageStrings.on() else CollageStrings.off()
    Box(
        Modifier
            .size(40.dp)
            .frameControl(FrameShapes.sm, selected = selected, glowAlpha = 0f, cornerFraction = 0.3f)
            .clip(FrameShapes.sm)
            .frameSelectable(selected, Role.Switch, onClick)
            .semantics {
                contentDescription = label
                stateDescription = state
            },
        Alignment.Center
    ) {
        Image(painterResource(icon), null, Modifier.size(20.dp), colorFilter = ColorFilter.tint(tint))
    }
}

@Composable
internal fun ChoiceRow(options: List<String>, selectedIndex: Int, modifier: Modifier = Modifier.fillMaxWidth(), onSelect: (Int) -> Unit) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEachIndexed { i, text -> ChoiceChip(text, i == selectedIndex, Modifier.weight(1f)) { onSelect(i) } }
    }
}

@Composable
internal fun ScrollChoiceRow(options: List<String>, selectedIndex: Int, modifier: Modifier = Modifier, onSelect: (Int) -> Unit) {
    Row(modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEachIndexed { i, text -> ChoiceChip(text, i == selectedIndex, Modifier.widthIn(min = 48.dp)) { onSelect(i) } }
    }
}
