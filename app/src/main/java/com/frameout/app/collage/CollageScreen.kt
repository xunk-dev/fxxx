package com.frameout.app.collage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.frameout.app.FrameOutApp
import com.frameout.app.PreferredFrameTheme
import com.frameout.core.storage.SaveFailure
import com.frameout.core.util.CollageStrings
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.components.FrameTopBar
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameAmbientGlow
import kotlinx.coroutines.launch

@Composable
fun CollageScreen(projectId: Long, frameIds: List<Long>, onBack: () -> Unit, onSaved: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as FrameOutApp
    val state = remember { CollageState(frameIds.size, app.container.prefs) }
    val scope = rememberCoroutineScope()
    CollageToastEffect(state)
    var leavePrompt by remember { mutableStateOf(false) }
    val leave: () -> Unit = { if (!state.holdBack()) { if (state.dirty) leavePrompt = true else onBack() } }
    CollageBackHandlers(state, leavePrompt, leave)
    CollageLifecycleEffects(state, app, context, projectId, frameIds)
    val gate = com.frameout.app.billing.rememberProGate()
    val doSave: (Boolean) -> Unit = { thenLeave -> startCollageSave(state, app, scope, context, thenLeave, onBack) }
    var lockPrompt by remember { mutableStateOf<com.frameout.core.pro.ProEditGuard.LockReport?>(null) }
    val lockGate = remember { com.frameout.app.billing.LockGate { lockPrompt = it } }
    com.frameout.app.billing.SecureWhile(gate.locked(com.frameout.core.pro.ProFeature.Collage, state.tier))
    val saveGated: (Boolean) -> Unit = { thenLeave ->
        lockGate.gate(state.lockReport()) { note ->
            if (note != null) state.stripLocked() else if (state.tier != com.frameout.core.pro.ProTier.Free) state.rewardShown = true
            doSave(thenLeave)
        }
    }
    val onSave: () -> Unit = { saveGated(false) }
    PreferredFrameTheme {
        Box(Modifier.fillMaxSize().background(FrameTheme.colors.background).frameAmbientGlow()) {
            CollageBody(state, leave, onSave)
            ToastHost(state.message, state.messageIsError, Modifier.align(Alignment.TopCenter))
            FramePickerSheet(state, projectId, app)
            if (leavePrompt) {
                com.frameout.app.UnsavedLeaveDialog(
                    onSaveLeave = { leavePrompt = false; saveGated(true) },
                    onLeave = { leavePrompt = false; onBack() },
                    onStay = { leavePrompt = false }
                )
            }
            lockPrompt?.let { com.frameout.app.billing.LockPromptDialog(it, lockGate) }
            CollageSaveStatus(state, Modifier.align(Alignment.TopCenter))
        }
    }
}

@Composable
private fun CollageBackHandlers(state: CollageState, leavePrompt: Boolean, leave: () -> Unit) {
    androidx.activity.compose.BackHandler(enabled = state.saving, onBack = leave)
    androidx.activity.compose.BackHandler(enabled = state.dirty && !state.saving && !leavePrompt, onBack = leave)
    androidx.activity.compose.BackHandler(enabled = state.selectedCell != null && !state.saving) { state.selectedCell = null }
}

@Composable
private fun CollageLifecycleEffects(state: CollageState, app: FrameOutApp, context: android.content.Context, projectId: Long, frameIds: List<Long>) {
    LaunchedEffect(projectId, frameIds) { state.load(context, app, frameIds) }
    LaunchedEffect(state.ready, state.template.cells.size, state.paths, state.previewSide) { state.refinePreviews() }
    LaunchedEffect(state) { state.persistOptions(app.container.prefs) }
    LaunchedEffect(state) { state.history.track() }
}

internal fun saveCollage(state: CollageState, app: FrameOutApp, scope: kotlinx.coroutines.CoroutineScope, context: android.content.Context, onSaved: () -> Unit = {}) {
    state.saving = true
    state.message = null
    state.selectedCell = null
    state.saveJob = scope.launch {
        val saved = try {
            state.save(app)
        } finally {
            state.saveEnded()
        }
        if (saved != null) {
            state.markClean()
            state.result = saved
            if (state.rewardShown) state.rewardShown = false else com.frameout.app.ads.AdsGateway.exportDone()
            onSaved()
        } else {
            state.messageIsError = true
            state.message = SaveFailure.message(context, FrameStrings.saveFailed())
        }
    }
}

@Composable
private fun CollageBody(state: CollageState, onBack: () -> Unit, onSave: () -> Unit) {
    Column(Modifier.fillMaxSize().navigationBarsPadding()) {
        val canSave = state.ready && state.frames.isNotEmpty() && !state.saving
        FrameTopBar(
            FrameStrings.collage(), onBack, FrameStrings.back(),
            actionIcon = if (canSave) FrameIcons.Save else null,
            actionLabel = CollageStrings.saveCollage(),
            onAction = if (canSave) onSave else null
        )
        PreviewArea(state, Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp))
        PreviewHint(state)
        CollagePanel(state, Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
    }
}

@Composable
private fun PreviewArea(state: CollageState, modifier: Modifier) {
    Box(modifier) {
        if (state.ready && state.frames.isEmpty()) {
            FrameText(CollageStrings.noFrames(), color = FrameTheme.colors.onSurfaceVariant, modifier = Modifier.align(Alignment.Center))
        } else {
            CollagePreview(state.canvasTemplate, state.slots(), state.style, state.selectedCell, state::tapCell, state::setCrop, state::swapCells, Modifier.fillMaxSize())
        }
    }
}
