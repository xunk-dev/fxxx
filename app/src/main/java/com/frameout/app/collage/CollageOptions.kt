package com.frameout.app.collage

import androidx.compose.runtime.snapshotFlow
import com.frameout.core.prefs.AppPrefs

internal data class CollageOptions(val gap: Int, val bgColor: Int, val rounded: Boolean, val stamps: Boolean)

suspend fun CollageState.persistOptions(prefs: AppPrefs) {
    snapshotFlow { CollageOptions(gap, bgColor, rounded, stamps) }.collect {
        prefs.setCollageGap(it.gap)
        prefs.setCollageBgColor(it.bgColor)
        prefs.setCollageRounded(it.rounded)
        prefs.setCollageStamps(it.stamps)
    }
}
