package com.frameout.app.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.frameout.core.prefs.AppPrefs
import com.frameout.designsystem.icons.FrameIconsExtra

@Composable
internal fun DebugShotSettings(prefs: AppPrefs) {
    val enabled by prefs.debugShotButton.collectAsState()
    val allLanguages by prefs.debugShotLanguages.collectAsState()
    ToggleRow(
        FrameIconsExtra.Snapshot,
        "Screenshot button (debug)",
        "Floating button that saves a clean PNG of the current screen, without the button",
        enabled,
        hintLines = 2
    ) { prefs.setDebugShotButton(it) }
    if (enabled) {
        SettingsDivider()
        ToggleRow(
            FrameIconsExtra.Language,
            "Shoot every language",
            "One tap switches through all app languages and saves the same screen for each, then restores yours",
            allLanguages,
            hintLines = 2
        ) { prefs.setDebugShotLanguages(it) }
    }
}
