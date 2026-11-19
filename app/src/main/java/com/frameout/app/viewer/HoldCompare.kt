package com.frameout.app.viewer

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import com.frameout.designsystem.haptics.FrameHaptic
import com.frameout.designsystem.haptics.rememberFrameHaptics
import kotlinx.coroutines.withTimeoutOrNull

@Composable
internal fun Modifier.holdToCompare(enabled: Boolean, onHold: (Boolean) -> Unit): Modifier {
    if (!enabled) return this
    val latest by rememberUpdatedState(onHold)
    val haptics = rememberFrameHaptics()
    val timeout = LocalViewConfiguration.current.longPressTimeoutMillis
    return pointerInput(timeout) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            val released = withTimeoutOrNull(timeout) { waitForUpOrCancellation(); true }
            if (released == null) {
                haptics.perform(FrameHaptic.LongPress)
                latest(true)
                try {
                    waitForUpOrCancellation()
                } finally {
                    latest(false)
                }
            }
        }
    }
}
