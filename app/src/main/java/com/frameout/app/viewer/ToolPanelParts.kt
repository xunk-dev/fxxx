package com.frameout.app.viewer

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.app.billing.TierFold
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.a11y.frameSelectable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.fieldFill
import com.frameout.designsystem.theme.frameControl
import com.frameout.designsystem.theme.frameHairline

internal class ChoiceOption(
    val icon: Int,
    val label: String,
    val selected: Boolean,
    val tier: com.frameout.core.pro.ProTier = com.frameout.core.pro.ProTier.Free,
    val onClick: () -> Unit
)

@Composable
internal fun PanelIconButton(icon: Int, label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(Modifier.size(40.dp).pressScale().alpha(if (enabled) 1f else 0.35f).frameClickable(enabled = enabled, onClick = onClick), Alignment.Center) {
        Box(Modifier.size(32.dp).clip(CircleShape).frameControl(CircleShape), Alignment.Center) {
            Image(painterResource(icon), label, Modifier.size(16.dp), colorFilter = ColorFilter.tint(FrameTheme.colors.onSurface))
        }
    }
}

@Composable
internal fun PanelDoneButton(label: String, icon: Int, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    Box(Modifier.size(40.dp).pressScale().frameClickable(onClick = onClick), Alignment.Center) {
        Box(Modifier.size(34.dp).frameControl(CircleShape, selected = true, solid = true, glowAlpha = 0.45f, glowSpread = 4.dp).clip(CircleShape), Alignment.Center) {
            Image(painterResource(icon), label, Modifier.size(18.dp), colorFilter = ColorFilter.tint(Color.White))
        }
    }
}

@Composable
internal fun ToolTab(
    icon: Int,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tier: com.frameout.core.pro.ProTier = com.frameout.core.pro.ProTier.Free
) {
    val colors = FrameTheme.colors
    val tint by animateColorAsState(if (selected) colors.primary else colors.onSurfaceVariant, tween(200), label = "tabTint")
    Box(
        modifier.pressScale().heightIn(min = TAB_HEIGHT)
            .then(if (selected) Modifier.frameControl(FrameShapes.sm, selected = true, glowAlpha = 0f, cornerFraction = 0.3f) else Modifier)
            .clip(FrameShapes.sm)
            .frameSelectable(selected, Role.Tab, onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.padding(horizontal = 2.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(painterResource(icon), null, Modifier.size(18.dp), colorFilter = ColorFilter.tint(tint))
            FrameText(
                label,
                style = FrameTheme.typography.label.copy(fontSize = 10.sp),
                color = tint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
        TierFold(tier, true, FrameShapes.sm, kind = com.frameout.app.billing.TierMarkKind.Chip)
    }
}

@Composable
internal fun ActionTile(icon: Int, label: String, modifier: Modifier = Modifier, selected: Boolean = false, mirror: Boolean = false, iconOnly: Boolean = false, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    val tint by animateColorAsState(if (selected) colors.primary else colors.onSurface, tween(200), label = "tileTint")
    Column(
        modifier.pressScale().heightIn(min = if (iconOnly) 44.dp else 48.dp)
            .frameControl(FrameShapes.md, selected = selected, glowAlpha = 0f, cornerFraction = 0.25f)
            .clip(FrameShapes.md)
            .frameClickable(onClick = onClick).padding(horizontal = 4.dp, vertical = if (iconOnly) 4.dp else 6.dp)
            .semantics { contentDescription = label },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically)
    ) {
        Image(
            painterResource(icon), null,
            Modifier.size(if (iconOnly) 22.dp else 18.dp).graphicsLayer { if (mirror) scaleX = -1f },
            colorFilter = ColorFilter.tint(tint)
        )
        if (!iconOnly) FrameText(label, style = FrameTheme.typography.label.copy(fontSize = 11.sp), color = tint, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

@Composable
internal fun ChoiceChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    val tint by animateColorAsState(if (selected) colors.primary else colors.onSurfaceVariant, tween(200), label = "chipTint")
    Box(
        modifier.pressScale().heightIn(min = 32.dp)
            .frameControl(FrameShapes.sm, selected = selected, glowAlpha = 0f, cornerFraction = 0.3f)
            .clip(FrameShapes.sm)
            .frameSelectable(selected, Role.RadioButton, onClick),
        Alignment.Center
    ) { FrameText(label, style = FrameTheme.typography.label.copy(fontSize = 12.sp), color = tint, maxLines = 1, modifier = Modifier.padding(horizontal = 12.dp)) }
}

internal val TAB_HEIGHT = 46.dp

@Composable
internal fun SegmentedChoice(options: List<ChoiceOption>, modifier: Modifier = Modifier) {
    val colors = FrameTheme.colors
    Row(
        modifier.fillMaxWidth().clip(FrameShapes.md).background(colors.fieldFill).border(1.dp, frameHairline(), FrameShapes.md).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { option ->
            val tint by animateColorAsState(if (option.selected) colors.primary else colors.onSurfaceVariant, tween(200), label = "choiceTint")
            Box(
                Modifier.weight(1f).heightIn(min = 36.dp).pressScale()
                    .frameControl(FrameShapes.sm, selected = option.selected, glowAlpha = if (option.selected) 0.3f else 0f, glowSpread = 3.dp, cornerFraction = 0.3f)
                    .clip(FrameShapes.sm)
                    .frameSelectable(option.selected, Role.RadioButton, option.onClick)
                    .semantics { contentDescription = option.label },
                Alignment.Center
            ) {
                Image(painterResource(option.icon), null, Modifier.size(20.dp).alpha(if (option.selected) 1f else 0.6f), colorFilter = ColorFilter.tint(tint))
                TierFold(option.tier, true, FrameShapes.sm, kind = com.frameout.app.billing.TierMarkKind.Chip)
            }
        }
    }
}

@Composable
internal fun LabeledSegmentedChoice(options: List<ChoiceOption>, modifier: Modifier = Modifier) {
    val colors = FrameTheme.colors
    Row(
        modifier.clip(FrameShapes.md).background(colors.fieldFill).border(1.dp, frameHairline(), FrameShapes.md).padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        options.forEach { option ->
            val tint by animateColorAsState(if (option.selected) colors.primary else colors.onSurfaceVariant, tween(200), label = "labeledChoiceTint")
            Box(
                Modifier.weight(1f).height(34.dp).pressScale()
                    .frameControl(FrameShapes.sm, selected = option.selected, glowAlpha = 0f, cornerFraction = 0.3f)
                    .clip(FrameShapes.sm)
                    .frameSelectable(option.selected, Role.RadioButton, option.onClick)
                    .semantics { contentDescription = option.label },
                Alignment.Center
            ) {
                Row(Modifier.padding(horizontal = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Image(painterResource(option.icon), null, Modifier.size(15.dp).alpha(if (option.selected) 1f else 0.7f), colorFilter = ColorFilter.tint(tint))
                    Spacer(Modifier.width(3.dp))
                    FrameText(
                        option.label,
                        style = FrameTheme.typography.label.copy(fontSize = 10.sp),
                        color = tint,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                TierFold(option.tier, true, FrameShapes.sm, kind = com.frameout.app.billing.TierMarkKind.Chip)
            }
        }
    }
}
