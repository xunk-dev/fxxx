package com.frameout.designsystem.dock

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

class DockCoverState {
    var count by mutableIntStateOf(0)
        internal set
}

val LocalDockCover = staticCompositionLocalOf<DockCoverState?> { null }

@Composable
fun DockCover(active: Boolean) {
    val state = LocalDockCover.current ?: return
    if (!active) return
    DisposableEffect(state) {
        state.count++
        onDispose { state.count-- }
    }
}
