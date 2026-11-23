package com.frameout.app.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.frameout.core.prefs.AppPrefs
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.TrimStrings
import com.frameout.designsystem.icons.FrameIconsExtra

@Composable
internal fun TrimGuideSettings(prefs: AppPrefs) {
    var replayed by remember { mutableStateOf(!prefs.trimGuideSeen()) }
    NavRow(
        FrameIconsExtra.Restore,
        TrimStrings.guideReplayTitle(),
        TrimStrings.guideReplayHint(),
        value = if (replayed) FrameStrings.done() else null,
        hintLines = 1
    ) {
        prefs.resetTrimGuide()
        replayed = true
    }
}
