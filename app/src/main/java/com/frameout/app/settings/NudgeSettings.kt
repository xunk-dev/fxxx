package com.frameout.app.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import com.frameout.core.media.PermissionHelper
import com.frameout.core.nudge.NudgePrefs
import com.frameout.core.nudge.NudgeStrings
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.engine.nudge.NudgeScheduler

@Composable
internal fun NudgeSettings() {
    val context = LocalContext.current
    val prefs = remember { NudgePrefs.get(context) }
    var newVideo by remember { mutableStateOf(prefs.newVideoEnabled) }
    var reminders by remember { mutableStateOf(prefs.remindersEnabled) }
    val systemOff = !NotificationManagerCompat.from(context).areNotificationsEnabled()
    val partial = PermissionHelper.isPartialMediaAccess(context)

    val newVideoHint = when {
        systemOff -> NudgeStrings.settingSystemOff()
        partial -> NudgeStrings.settingNewVideoPartial()
        else -> NudgeStrings.settingNewVideoHint()
    }
    ToggleRow(FrameIconsExtra.BellRing, NudgeStrings.settingNewVideoTitle(), newVideoHint, newVideo, hintLines = 2) {
        newVideo = it
        prefs.newVideoEnabled = it
        NudgeScheduler.sync(context)
    }
    SettingsDivider()
    ToggleRow(
        FrameIconsExtra.Bell,
        NudgeStrings.settingRemindersTitle(),
        if (systemOff) NudgeStrings.settingSystemOff() else NudgeStrings.settingRemindersHint(),
        reminders,
        hintLines = 2
    ) {
        reminders = it
        prefs.remindersEnabled = it
        NudgeScheduler.sync(context)
    }
}
