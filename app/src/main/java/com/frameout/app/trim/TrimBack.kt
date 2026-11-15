package com.frameout.app.trim

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState

@Composable
internal fun rememberGuardedBack(onBack: () -> Unit, holding: Boolean = false, hold: () -> Boolean = { false }): () -> Unit {
    val leave by rememberUpdatedState(onBack)
    val held by rememberUpdatedState(hold)
    val guarded = { if (!held()) leave() }
    BackHandler(enabled = holding, onBack = guarded)
    return guarded
}
