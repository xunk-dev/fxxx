package com.frameout.app.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.frameout.core.prefs.AppPrefs
import com.frameout.core.storage.ImageFormat
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.components.FrameSlider
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameTheme
import kotlin.math.roundToInt

private const val MIN_QUALITY = 30
private const val MAX_QUALITY = 100

@Composable
internal fun QualitySettings(prefs: AppPrefs, formatKey: String) {
    val colors = FrameTheme.colors
    val quality by prefs.jpegQuality.collectAsState()
    if (ImageFormat.fromKey(formatKey).lossless) return
    val shown = quality.coerceIn(MIN_QUALITY, MAX_QUALITY)
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { SettingsRowHeader(FrameIconsExtra.Contrast, FrameStrings.qualityTitle()) }
            FrameText("$shown%", style = FrameTheme.typography.mono, color = colors.primary)
        }
        FrameSlider(
            value = shown.toFloat(),
            onChange = { prefs.setJpegQuality(it.roundToInt().coerceIn(MIN_QUALITY, MAX_QUALITY)) },
            min = MIN_QUALITY.toFloat(),
            max = MAX_QUALITY.toFloat(),
            label = FrameStrings.qualityTitle(),
            modifier = Modifier.height(40.dp)
        )
    }
}
