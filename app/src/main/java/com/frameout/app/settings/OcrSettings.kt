package com.frameout.app.settings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.frameout.app.AraModelPack
import com.frameout.app.PackAction
import com.frameout.app.PackState
import com.frameout.app.PackStates
import com.frameout.core.quality.TextModelHealth
import com.frameout.core.quality.TextModelState
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.OcrStrings
import com.frameout.core.util.QualityStrings
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameTheme
import kotlinx.coroutines.launch

@Composable
internal fun OcrSettings() {
    val pack by AraModelPack.state.collectAsState()
    val model by TextModelHealth.state.collectAsState()
    val showArabic = PackStates.visible(pack)
    if (!showArabic && !model.problem) return
    SettingsSection(QualityStrings.liveTextLabel()) {
        SettingsCard {
            if (showArabic) ArabicRow(pack)
            if (showArabic && model.problem) SettingsDivider()
            if (model.problem) TextModelRow(model)
        }
    }
}

@Composable
private fun ArabicRow(pack: PackState) {
    val context = LocalContext.current
    val action = PackStates.action(pack)
    StatusRow(
        icon = FrameIconsExtra.Language,
        title = OcrStrings.araTitle(),
        hint = arabicHint(pack),
        value = (pack as? PackState.Downloading)?.let { "${it.percent}%" },
        actionLabel = when (action) {
            PackAction.Retry -> FrameStrings.retry()
            PackAction.Confirm -> FrameStrings.notifAllow()
            PackAction.None -> null
        }
    ) {
        when (action) {
            PackAction.Retry -> AraModelPack.retry(context)
            PackAction.Confirm -> context.hostActivity()?.let { AraModelPack.confirm(it) }
            PackAction.None -> Unit
        }
    }
}

@Composable
private fun TextModelRow(model: TextModelState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    StatusRow(
        icon = FrameIconsExtra.Text,
        title = OcrStrings.textTitle(),
        hint = OcrStrings.textMessage(model).orEmpty(),
        value = null,
        actionLabel = FrameStrings.retry()
    ) { scope.launch { TextModelHealth.check(context) } }
}

private fun arabicHint(pack: PackState): String = when (pack) {
    is PackState.Downloading -> OcrStrings.araDownloading(pack.percent)
    PackState.WaitingWifi -> OcrStrings.araWaitingWifi()
    PackState.NeedsConfirmation -> OcrStrings.araConfirm()
    PackState.Unavailable -> OcrStrings.araUnavailable()
    is PackState.Failed -> OcrStrings.araFailed()
    PackState.Idle, PackState.Ready -> ""
}

@Composable
private fun StatusRow(icon: Int, title: String, hint: String, value: String?, actionLabel: String?, onAction: () -> Unit) {
    val colors = FrameTheme.colors
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingsIconTile(icon)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                FrameText(title, style = rowTitleStyle())
                FrameText(hint, style = FrameTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            value?.let { FrameText(it, style = FrameTheme.typography.monoSmall, color = colors.primary, textAlign = TextAlign.End, modifier = Modifier.widthIn(max = 150.dp)) }
        }
        if (actionLabel != null) SettingsActionButton(actionLabel, onClick = onAction)
    }
}

private tailrec fun Context.hostActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.hostActivity()
    else -> null
}
