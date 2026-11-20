package com.frameout.app.frames

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.frameout.app.settings.SettingsCard
import com.frameout.app.settings.rowTitleStyle
import com.frameout.core.export.ContactSheetLayout
import com.frameout.core.export.ContactSheetPreview
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.a11y.frameToggle
import com.frameout.designsystem.components.FrameSlider
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import kotlin.math.roundToInt

@Composable
internal fun ColumnsCard(columns: Int, onChange: (Int) -> Unit) {
    val colors = FrameTheme.colors
    val label = FrameStrings.contactColumns()
    SettingsCard {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                FrameText(label, style = rowTitleStyle(), modifier = Modifier.weight(1f))
                FrameText(columns.toString(), style = FrameTheme.typography.display, color = colors.primary)
            }
            FrameSlider(columns.toFloat(), { onChange(it.roundToInt().coerceIn(ContactSheetLayout.MIN_COLUMNS, ContactSheetLayout.MAX_COLUMNS)) }, ContactSheetLayout.MIN_COLUMNS.toFloat(), ContactSheetLayout.MAX_COLUMNS.toFloat(), label)
            ColumnTicks(columns)
        }
    }
}

@Composable
private fun ColumnTicks(selected: Int) {
    val colors = FrameTheme.colors
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        for (n in ContactSheetLayout.MIN_COLUMNS..ContactSheetLayout.MAX_COLUMNS) {
            FrameText(
                n.toString(),
                style = FrameTheme.typography.bodySmall.copy(fontWeight = if (n == selected) FontWeight.SemiBold else FontWeight.Normal),
                color = if (n == selected) colors.primary else colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(28.dp)
            )
        }
    }
}

@Composable
internal fun StatsCard(summary: ContactSheetPreview.Summary) {
    val colors = FrameTheme.colors
    SettingsCard {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                StatCell(FrameStrings.contactSizeLabel(), "${summary.width} × ${summary.height}", Modifier.weight(1.35f))
                StatDivider()
                StatCell(FrameStrings.contactMpLabel(), "%.1f MP".format(java.util.Locale.US, summary.megapixels), Modifier.weight(1f))
                StatDivider()
                StatCell(FrameStrings.contactCellLabel(), "${summary.plan.cellW}×${summary.plan.cellH}", Modifier.weight(1f))
            }
            Box(Modifier.padding(vertical = 10.dp).fillMaxWidth().height(1.dp).background(colors.onSurface.copy(alpha = 0.08f)))
            StatNote(summary)
        }
    }
}

@Composable
private fun StatCell(label: String, value: String, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        FrameText(label, style = FrameTheme.typography.bodySmall, color = FrameTheme.colors.onSurfaceVariant)
        FrameText(value, style = FrameTheme.typography.mono.copy(fontWeight = FontWeight.SemiBold), color = FrameTheme.colors.onSurface)
    }
}

@Composable
private fun StatDivider() {
    Box(Modifier.padding(horizontal = 10.dp).width(1.dp).height(32.dp).background(FrameTheme.colors.onSurface.copy(alpha = 0.08f)))
}

@Composable
private fun StatNote(summary: ContactSheetPreview.Summary) {
    val colors = FrameTheme.colors
    val warn = summary.tooMany || summary.tiny
    val text = when {
        summary.tooMany -> FrameStrings.contactTooMany()
        summary.tiny -> FrameStrings.contactNoteTiny()
        else -> FrameStrings.contactNoteOk()
    }
    val tint = if (warn) colors.warning else colors.success
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Image(painterResource(if (warn) FrameIconsExtra.Warning else FrameIcons.Check), null, Modifier.size(16.dp), colorFilter = ColorFilter.tint(tint))
        FrameText(text, style = FrameTheme.typography.bodySmall, color = tint, modifier = Modifier.weight(1f))
    }
}

internal class ChipSpec(val icon: Int, val title: String, val on: Boolean, val onChange: (Boolean) -> Unit)

@Composable
internal fun ChipRow(chips: List<ChipSpec>) {
    Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        chips.forEach { ToggleChip(it, Modifier.weight(1f)) }
    }
}

@Composable
private fun ToggleChip(chip: ChipSpec, modifier: Modifier) {
    val colors = FrameTheme.colors
    val ink = if (chip.on) colors.onPrimary else colors.onSurface
    Row(
        modifier
            .height(44.dp)
            .clip(FrameShapes.sm)
            .background(if (chip.on) colors.primary else colors.containerHigh)
            .frameToggle(chip.on, chip.onChange),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(painterResource(chip.icon), null, Modifier.size(18.dp), colorFilter = ColorFilter.tint(if (chip.on) colors.onPrimary else colors.primary))
        FrameText(chip.title, style = FrameTheme.typography.body, color = ink, modifier = Modifier.padding(start = 6.dp))
    }
}
