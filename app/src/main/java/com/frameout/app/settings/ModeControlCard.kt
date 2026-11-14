package com.frameout.app.settings

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.frameout.core.prefs.AppPrefs
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.ModeStrings
import com.frameout.designsystem.a11y.frameSelectable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.fieldFill
import com.frameout.designsystem.theme.frameHairline
import com.frameout.engine.extract.ExtractMode
import com.frameout.engine.extract.ExtractModeCodec

private const val MAX_FRAMES = 100000L
private const val MAX_SECONDS = 86400L
private const val MAX_MILLIS = 86400000L
private val FramePresets = listOf(2L, 5L, 10L, 30L)
private val CountPresets = listOf(10L, 30L, 60L, 100L)
private val SecondPresets = listOf(1L, 2L, 5L, 10L)
private val MillisPresets = listOf(100L, 250L, 500L, 1000L)

@Composable
internal fun ModeControlCard(mode: ExtractMode, prefs: AppPrefs, modifier: Modifier = Modifier) {
    val colors = FrameTheme.colors
    val kind = kindOf(mode)
    val save = { next: ExtractMode -> prefs.setExtractMode(ExtractModeCodec.encode(next)) }
    Column(
        modifier
            .fillMaxWidth()
            .animateContentSize()
            .clip(FrameShapes.lg)
            .background(colors.fieldFill)
            .border(1.dp, colors.primary.copy(alpha = 0.4f), FrameShapes.lg)
            .padding(top = 14.dp),
        Arrangement.spacedBy(4.dp)
    ) {
        Column(Modifier.padding(horizontal = 16.dp), Arrangement.spacedBy(4.dp)) {
            FrameText(FrameStrings.settingValue(kind), style = FrameTheme.typography.titleSmall, color = colors.onSurface)
            FrameText(ModeStrings.modeHelp(kind), style = FrameTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
        key(kind) { ModeValueControls(mode, save) }
        Box(Modifier.padding(bottom = 6.dp))
    }
}

@Composable
private fun ModeValueControls(mode: ExtractMode, save: (ExtractMode) -> Unit) {
    when (mode) {
        is ExtractMode.EveryNFrames -> ValueBlock(ModeStrings.framesLabel(), mode.n.toLong(), MAX_FRAMES, 1L, FramePresets) { save(ExtractMode.EveryNFrames(it.toInt())) }
        is ExtractMode.FixedCount -> ValueBlock(ModeStrings.countLabel(), mode.count.toLong(), MAX_FRAMES, 1L, CountPresets) { save(ExtractMode.FixedCount(it.toInt())) }
        is ExtractMode.EveryNMillis -> TimeBlock(mode.millis) { save(ExtractMode.EveryNMillis(it)) }
        else -> Unit
    }
}

@Composable
private fun TimeBlock(millis: Long, onCommit: (Long) -> Unit) {
    var unit by rememberSaveable { mutableStateOf(if (millis % 1000L == 0L) "s" else "ms") }
    val seconds = unit == "s"
    Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        SettingsSegmented(listOf(SegmentOption("s", ModeStrings.seconds()), SegmentOption("ms", ModeStrings.millis())), unit, compact = true) { unit = it }
    }
    key(unit) {
        val shown = if (seconds) (millis / 1000L).coerceAtLeast(1L) else millis
        ValueBlock(
            ModeStrings.timeLabel(), shown, if (seconds) MAX_SECONDS else MAX_MILLIS, if (seconds) 1L else 100L,
            if (seconds) SecondPresets else MillisPresets
        ) { onCommit(if (seconds) it * 1000L else it) }
    }
}

@Composable
private fun ValueBlock(label: String, value: Long, max: Long, step: Long, presets: List<Long>, onCommit: (Long) -> Unit) {
    var reset by remember { mutableIntStateOf(0) }
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), Arrangement.spacedBy(8.dp)) {
            presets.forEach { preset ->
                PresetChip(preset.toString(), preset == value, Modifier.weight(1f)) {
                    onCommit(preset)
                    reset++
                }
            }
        }
        key(reset) { SettingsNumberField(label, value, max, step, onCommit) }
    }
}

@Composable
private fun PresetChip(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    Box(
        modifier
            .defaultMinSize(minHeight = 44.dp)
            .clip(FrameShapes.md)
            .background(if (selected) colors.primary else colors.containerHigh)
            .border(1.dp, if (selected) colors.primary else frameHairline(), FrameShapes.md)
            .frameSelectable(selected, Role.RadioButton, onClick),
        Alignment.Center
    ) {
        FrameText(text, style = FrameTheme.typography.label, color = if (selected) colors.onPrimary else colors.onSurface)
    }
}
