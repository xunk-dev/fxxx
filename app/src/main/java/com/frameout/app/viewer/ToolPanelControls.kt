package com.frameout.app.viewer

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.core.util.EditStrings
import com.frameout.designsystem.a11y.frameHoldRepeat
import com.frameout.designsystem.a11y.frameSelectable
import com.frameout.designsystem.components.FrameSlider
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameControl

@Composable
internal fun IconToggle(icon: Int, label: String, on: Boolean, enabled: Boolean = true, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    val tint by animateColorAsState(if (on) colors.primary else colors.onSurfaceVariant, tween(200), label = "toggleTint")
    Box(
        modifier.size(38.dp).pressScale().alpha(if (enabled) 1f else 0.38f)
            .frameControl(FrameShapes.sm, selected = on, glowAlpha = if (on) 0.4f else 0f, glowSpread = 3.dp, cornerFraction = 0.3f)
            .clip(FrameShapes.sm)
            .then(if (enabled) Modifier.frameSelectable(on, Role.Switch, onClick) else Modifier)
            .semantics { contentDescription = label },
        Alignment.Center
    ) {
        Image(painterResource(icon), null, Modifier.size(22.dp).alpha(if (on) 1f else 0.65f), colorFilter = ColorFilter.tint(tint))
    }
}

@Composable
internal fun PrecisionRow(
    label: String,
    valueText: String,
    fraction: Float,
    onFraction: (Float) -> Unit,
    onStep: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leading: @Composable () -> Unit
) {
    Box(modifier.fillMaxWidth().alpha(if (enabled) 1f else 0.38f)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            FrameText(label, style = FrameTheme.typography.label.copy(fontSize = 12.sp), color = FrameTheme.colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(64.dp))
            StepButton(FrameIconsExtra.Minus, EditStrings.decrease()) { onStep(-1) }
            FrameSlider(fraction, onFraction, 0f, 1f, label, Modifier.weight(1f).height(28.dp))
            StepButton(FrameIconsExtra.Plus, EditStrings.increase()) { onStep(1) }
            Spacer(Modifier.width(4.dp))
            ValueChip(valueText)
        }
        if (!enabled) Box(Modifier.matchParentSize().pointerInput(Unit) { detectTapGestures { } })
    }
}

@Composable
internal fun StepButton(icon: Int, label: String, onStep: () -> Unit) {
    Box(Modifier.size(40.dp).frameHoldRepeat(onStep).semantics { contentDescription = label }, Alignment.Center) {
        Image(painterResource(icon), null, Modifier.size(18.dp), colorFilter = ColorFilter.tint(FrameTheme.colors.primary))
    }
}

@Composable
internal fun ValueChip(text: String) {
    Box(Modifier.width(46.dp).clip(FrameShapes.xs).background(FrameTheme.colors.primary.copy(alpha = 0.12f)).border(1.dp, FrameTheme.colors.primary.copy(alpha = 0.28f), FrameShapes.xs).padding(vertical = 2.dp), Alignment.Center) {
        FrameText(text, style = FrameTheme.typography.monoSmall.copy(fontSize = 11.sp), color = FrameTheme.colors.primary, maxLines = 1, softWrap = false)
    }
}

@Composable
internal fun CompactSliderRow(
    label: String,
    valueText: String,
    fraction: Float,
    onFraction: (Float) -> Unit,
    onStep: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    dense: Boolean = false,
    showLabel: Boolean = false,
    leading: (@Composable () -> Unit)? = null
) {
    Box(modifier.fillMaxWidth().alpha(if (enabled) 1f else 0.38f)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (leading != null) Box(Modifier.size(24.dp), Alignment.Center) { leading() }
            if (showLabel) FrameText(
                label,
                style = FrameTheme.typography.label.copy(fontSize = 11.sp),
                color = FrameTheme.colors.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.padding(start = 6.dp).width(44.dp)
            )
            FrameSlider(fraction, onFraction, 0f, 1f, label, Modifier.weight(1f).height(if (dense) 30.dp else 36.dp))
            Spacer(Modifier.width(10.dp))
            ValueChip(valueText)
        }
        if (!enabled) Box(Modifier.matchParentSize().pointerInput(Unit) { detectTapGestures { } })
    }
}

@Composable
internal fun ParamChip(icon: Int, label: String, selected: Boolean, modified: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    val tint by animateColorAsState(if (selected) colors.primary else colors.onSurfaceVariant, tween(200), label = "paramTint")
    Row(
        modifier.pressScale().heightIn(min = 36.dp)
            .frameControl(FrameShapes.sm, selected = selected, glowAlpha = 0f, cornerFraction = 0.3f)
            .clip(FrameShapes.sm)
            .frameSelectable(selected, Role.RadioButton, onClick).padding(horizontal = 6.dp),
        Arrangement.Center,
        Alignment.CenterVertically
    ) {
        Image(painterResource(icon), null, Modifier.size(18.dp), colorFilter = ColorFilter.tint(tint))
        Spacer(Modifier.width(5.dp))
        FrameText(label, style = FrameTheme.typography.label, color = tint, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        if (modified) {
            Spacer(Modifier.width(4.dp))
            Box(Modifier.size(6.dp).clip(CircleShape).background(colors.primary))
        }
    }
}
