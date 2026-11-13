package com.frameout.app.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.frameout.app.FrameOutApp
import com.frameout.core.ads.AdStrings
import com.frameout.designsystem.icons.FrameIcons

@Composable
internal fun AdSettings() {
    val hub = (LocalContext.current.applicationContext as FrameOutApp).ads
    val required by hub.privacyOptions.collectAsState()
    if (!required) return
    SettingsDivider()
    NavRow(FrameIcons.Info, AdStrings.privacyOptions(), onClick = hub::showPrivacyOptions)
}
