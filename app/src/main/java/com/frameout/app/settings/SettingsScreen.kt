package com.frameout.app.settings

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import com.frameout.designsystem.a11y.frameClickable
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.rotate
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.produceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.frameout.app.home.ExtractStatusHost
import com.frameout.app.applyAccent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.app.BuildConfig
import com.frameout.app.FrameOutApp
import com.frameout.app.settleAccentTrial
import com.frameout.app.billing.rememberProGate
import com.frameout.app.PreferredFrameTheme
import com.frameout.app.library.IconButtonBox
import com.frameout.core.prefs.AppPrefs
import com.frameout.core.util.DiagLog
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.ModeStrings
import com.frameout.designsystem.a11y.screenHeading
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameAmbientGlow
import com.frameout.designsystem.theme.frameRailPadding

@Composable
fun SettingsScreen(onBack: () -> Unit, onAbout: () -> Unit = {}) {
    val context = LocalContext.current
    val prefs = (context.applicationContext as FrameOutApp).container.prefs
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose { (context.applicationContext as FrameOutApp).settleAccentTrial() }
    }
    val theme by prefs.themeMode.collectAsState()
    val lang by prefs.language.collectAsState()
    val format by prefs.saveFormat.collectAsState()
    val autoSave by prefs.autoSave.collectAsState()
    val datePolicy by prefs.datePolicy.collectAsState()
    val extract by prefs.extractMode.collectAsState()
    val folder by prefs.saveFolderUri.collectAsState()
    val exportFolder by prefs.exportFolderUri.collectAsState()
    PreferredFrameTheme { SettingsContent(context, prefs, onBack, onAbout, SettingsValues(theme, lang, format, autoSave, datePolicy, extract, folder, exportFolder)) }
}

internal data class SettingsValues(
    val theme: String, val lang: String, val format: String, val autoSave: Boolean, val datePolicy: String, val extract: String, val folder: String, val exportFolder: String
)

@Composable
private fun SettingsContent(context: Context, prefs: AppPrefs, onBack: () -> Unit, onAbout: () -> Unit, v: SettingsValues) {
    val colors = FrameTheme.colors
    var sheet by remember { mutableStateOf<SettingsSheet?>(null) }
    val pickFolder = rememberFolderPicker(prefs)
    val pickExportFolder = rememberFolderPicker(prefs) { prefs.setExportFolderUri(it) }
    val folderText by produceState(FrameStrings.defaultFolder(), v.folder) { value = withContext(Dispatchers.IO) { folderValue(context, v.folder) } }
    val exportFolderText by produceState(FrameStrings.defaultFolder(), v.exportFolder) { value = withContext(Dispatchers.IO) { folderValue(context, v.exportFolder) } }
    Box(Modifier.fillMaxSize().background(colors.background).frameAmbientGlow()) {
        Box(Modifier.fillMaxSize().frameRailPadding()) {
        Column(
            Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 640.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            SettingsHeader(onBack)
            ProSettings()
            AppearanceSettings(prefs, v.theme, v.lang) { sheet = SettingsSheet.Language }
            ExportSettings(prefs, folderText, exportFolderText, v, { sheet = SettingsSheet.Format }, { sheet = SettingsSheet.Folder })
            ExtractionSettings(prefs, v.extract) { sheet = SettingsSheet.Mode }
            MoreSettings(context, prefs, v.datePolicy, onAbout)
            Spacer(Modifier.height(120.dp))
        }
        }
        com.frameout.app.MainTabBar(2)
        ExtractStatusHost()
        SettingsSheets(sheet, prefs, context, v, pickFolder, pickExportFolder, onClose = { sheet = null })
    }
}

@Composable
private fun SettingsHeader(onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconButtonBox(FrameIcons.Back, FrameStrings.back(), onBack)
        FrameText(
            FrameStrings.settings(),
            style = FrameTheme.typography.display.copy(fontSize = 22.sp, lineHeight = 28.sp),
            modifier = Modifier.screenHeading()
        )
    }
}

@Composable
private fun AppearanceSettings(prefs: AppPrefs, theme: String, lang: String, onLanguage: () -> Unit) {
    val app = LocalContext.current.applicationContext as com.frameout.app.FrameOutApp
    val accent by prefs.accent.collectAsState()
    val iconFollows by prefs.iconFollowsTheme.collectAsState()
    SettingsSection(FrameStrings.appearance()) {
        SettingsCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SettingsRowHeader(FrameIconsExtra.Theme, FrameStrings.theme())
                ThemePicker(theme) { prefs.setThemeMode(it) }
                AccentPicker(accent) { app.applyAccent(it) }
            }
            SettingsDivider(16.dp)
            AppIconSettings(prefs, iconFollows, accent, theme)
            SettingsDivider(16.dp)
            NavRow(FrameIconsExtra.Language, FrameStrings.language(), value = languageLabel(lang), onClick = onLanguage)
        }
    }
}

@Composable
private fun ExportSettings(prefs: AppPrefs, folderText: String, exportFolderText: String, v: SettingsValues, onFormat: () -> Unit, onFolder: () -> Unit) {
    val lossless = com.frameout.core.storage.ImageFormat.fromKey(v.format).lossless
    SettingsSection(FrameStrings.export()) {
        SettingsCard {
            NavRow(FrameIconsExtra.ImageFormat, FrameStrings.format(), value = FrameStrings.settingValue(v.format), onClick = onFormat)
            if (!lossless) {
                SettingsDivider()
                QualitySettings(prefs, v.format)
            }
            SettingsDivider()
            val allDefault = folderText == FrameStrings.defaultFolder() && exportFolderText == FrameStrings.defaultFolder()
            NavRow(
                FrameIconsExtra.Folder,
                FrameStrings.saveFolder(),
                hint = "${FrameStrings.saveFolderHint()} · ${FrameStrings.exportFolderHint()}",
                value = if (allDefault) FrameStrings.defaultFolder() else FrameStrings.customFolder(),
                hintLines = 1,
                onClick = onFolder
            )
            SettingsDivider()
            ToggleRow(FrameIcons.Save, FrameStrings.autoSave(), FrameStrings.autoSaveHint(), v.autoSave, hintLines = 1) { prefs.setAutoSave(it) }
        }
    }
}

@Composable
private fun MoreSettings(context: Context, prefs: AppPrefs, datePolicy: String, onAbout: () -> Unit) {
    val keepLocation by prefs.keepLocation.collectAsState()
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.padding(top = 24.dp)) {
        SettingsCard {
            MoreHeaderRow(expanded) { expanded = !expanded }
            if (expanded) {
                SettingsDivider()
                DatePolicyRow(prefs, datePolicy)
                SettingsDivider()
                ToggleRow(FrameIconsExtra.Location, FrameStrings.keepLocation(), FrameStrings.keepLocationHint(), keepLocation, hintLines = 1) { prefs.setKeepLocation(it) }
                SettingsDivider()
                VolumeNavSettings(prefs)
                SettingsDivider()
                NudgeSettings()
                AdSettings()
                SettingsDivider()
                DiagnosticsSettings(context)
            }
            SettingsDivider()
            NavRow(FrameIcons.Info, ModeStrings.about(), value = BuildConfig.VERSION_NAME, monoValue = true, onClick = onAbout)
        }
    }
}

@Composable
private fun MoreHeaderRow(expanded: Boolean, onToggle: () -> Unit) {
    val colors = FrameTheme.colors
    Row(
        Modifier.fillMaxWidth().frameClickable { onToggle() }.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingsIconTile(FrameIconsExtra.More, expanded)
        FrameText(FrameStrings.more(), style = rowTitleStyle(), modifier = Modifier.weight(1f))
        androidx.compose.foundation.Image(
            androidx.compose.ui.res.painterResource(FrameIconsExtra.Chevron),
            null,
            Modifier.size(18.dp).rotate(if (expanded) 90f else 0f),
            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(colors.onSurfaceVariant)
        )
    }
}

@Composable
private fun DatePolicyRow(prefs: AppPrefs, datePolicy: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingsIconTile(FrameIconsExtra.Calendar)
        FrameText(FrameStrings.datePolicy(), style = rowTitleStyle(), modifier = Modifier.weight(1f))
        SettingsSegmented(settingOptions(listOf("frame", "video")), datePolicy, compact = true) { prefs.setDatePolicy(it) }
    }
}

@Composable
private fun ExtractionSettings(prefs: AppPrefs, extract: String, onMode: () -> Unit) {
    val gate = rememberProGate()
    val skipBlurry = prefs.skipBlurry.collectAsState().value && gate.skipUsable()
    val skipDuplicates = prefs.skipDuplicates.collectAsState().value && gate.skipUsable()
    val locked = gate.locked(false)
    SettingsSection(FrameStrings.extraction(), FrameStrings.settingsApplyNotice()) {
        SettingsCard {
            ExtractModeSettings(prefs, extract, onMode)
            SettingsDivider()
            com.frameout.app.billing.ProGroup(locked = locked, title = com.frameout.core.util.TrimStrings.filtersTitle()) {
                ToggleRow(FrameIconsExtra.Blur, FrameStrings.skipBlurryTitle(), ModeStrings.helpBlurry(), skipBlurry, locked) { gateSkip(it) { prefs.setSkipBlurry(it) } }
                SettingsDivider()
                ToggleRow(FrameIconsExtra.Duplicate, FrameStrings.skipDuplicatesTitle(), ModeStrings.helpDuplicates(), skipDuplicates, locked) { gateSkip(it) { prefs.setSkipDuplicates(it) } }
            }
        }
    }
}

@Composable
private fun DiagnosticsSettings(context: Context) {
    NavRow(FrameIcons.Share, FrameStrings.exportLog(), hint = FrameStrings.diagHint(), hintLines = 1) {
        val file = DiagLog.export(context)
        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        context.startActivity(Intent.createChooser(send, FrameStrings.exportLog()))
    }
}
