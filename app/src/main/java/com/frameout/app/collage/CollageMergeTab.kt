package com.frameout.app.collage

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.frameout.app.billing.TierFold
import com.frameout.app.billing.rememberProGate
import com.frameout.app.viewer.StepButton
import com.frameout.app.viewer.ValueChip
import com.frameout.core.collage.BLEND_STRENGTH_MAX
import com.frameout.core.collage.CollageBlend
import com.frameout.core.pro.ProCatalog
import com.frameout.core.pro.ProFeature
import com.frameout.core.pro.ProTier
import com.frameout.core.util.CollageStrings
import com.frameout.core.util.EditStrings
import com.frameout.designsystem.a11y.frameSelectable
import com.frameout.designsystem.components.FrameSlider
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameHairline
import kotlin.math.roundToInt
import com.frameout.core.util.safeCoerceIn

@Composable
internal fun MergeTab(state: CollageState) {
    val gate = rememberProGate()
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        StrengthRow(state)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            items(CollageBlend.all, key = { it.id }) { b ->
                val tier = ProCatalog.blendTier(b.id)
                val index = CollageBlend.all.indexOf(b)
                BlendTile(b, index, b == state.blend, tier, gate.locked(ProFeature.Collage, tier)) {
                    if (tier == ProTier.Pro) gate.use(ProFeature.Collage, tier) { state.pickBlend(b) } else state.pickBlend(b)
                }
            }
        }
    }
}

@Composable
private fun StrengthRow(state: CollageState) {
    val max = BLEND_STRENGTH_MAX
    val on = state.blend != CollageBlend.None
    val label = CollageStrings.mergeStrength()
    Row(Modifier.fillMaxWidth().alpha(if (on) 1f else 0.4f), verticalAlignment = Alignment.CenterVertically) {
        GroupIcon(FrameIconsExtra.CollageMerge)
        StepButton(FrameIconsExtra.Minus, EditStrings.decrease()) { if (on) state.blendStrength = (state.blendStrength - 1).safeCoerceIn(0, max) }
        FrameSlider(
            state.blendStrength / max.toFloat(),
            { if (on) state.blendStrength = (it * max).roundToInt().safeCoerceIn(0, max) },
            0f, 1f, label, Modifier.weight(1f).height(28.dp)
        )
        StepButton(FrameIconsExtra.Plus, EditStrings.increase()) { if (on) state.blendStrength = (state.blendStrength + 1).safeCoerceIn(0, max) }
        ValueChip("${state.blendStrength}")
    }
}

@Composable
private fun BlendTile(blend: CollageBlend, index: Int, active: Boolean, tier: ProTier, locked: Boolean, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    val border by animateColorAsState(if (active) colors.primary else frameHairline(), label = "blendBorder")
    val fill by animateColorAsState(if (active) colors.primaryContainer else colors.container, label = "blendFill")
    val name = if (blend == CollageBlend.None) CollageStrings.off() else CollageStrings.mergeStyle(index)
    Box(Modifier.width(52.dp).height(52.dp)) {
        Box(
            Modifier
                .matchParentSize()
                .clip(FrameShapes.sm)
                .background(fill)
                .border(if (active) 1.5.dp else 1.dp, border, FrameShapes.sm)
                .frameSelectable(active, Role.RadioButton, onClick)
                .semantics { contentDescription = name },
            Alignment.Center
        ) {
            BlendGlyph(blend, if (active) colors.primary else colors.onSurfaceVariant, Modifier.size(34.dp).padding(1.dp))
        }
        TierFold(tier, locked, FrameShapes.sm, ring = !active)
    }
}
