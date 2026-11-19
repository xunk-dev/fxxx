package com.frameout.app.collage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.frameout.core.collage.CollageTemplates
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
    LaunchedEffect(projectId, frameIds) { state.load(context, app, frameIds) }
    var leavePrompt by remember { mutableStateOf(false) }
    val leave: () -> Unit = { if (!state.holdBack()) { if (state.dirty) leavePrompt = true else onBack() } }
    androidx.activity.compose.BackHandler(enabled = state.saving, onBack = leave)
    androidx.activity.compose.BackHandler(enabled = state.dirty && !state.saving && !leavePrompt, onBack = leave)
    androidx.activity.compose.BackHandler(enabled = state.selectedCell != null && !state.saving) { state.selectedCell = null }
    LaunchedEffect(state) { state.persistOptions(app.container.prefs) }
    LaunchedEffect(state) { state.history.track() }
    val gate = com.frameout.app.billing.rememberProGate()
    com.frameout.app.billing.SecureWhile(gate.enforced && !com.frameout.core.pro.ProCatalog.collageIsFree(state.template.id) && !com.frameout.core.pro.ProAccess.permits(com.frameout.core.pro.ProFeature.Collage))
    val doSave: (Boolean) -> Unit = { thenLeave -> startCollageSave(state, app, scope, context, thenLeave, onBack) }
    val saveGated: (Boolean) -> Unit = { thenLeave ->
        gate.gated(com.frameout.core.pro.ProFeature.Collage, com.frameout.core.pro.ProCatalog.collageIsFree(state.template.id), state.template.id) { doSave(thenLeave) }
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
            CollageSaveStatus(state, Modifier.align(Alignment.TopCenter))
        }
    }
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
            onSaved()
        } else {
            state.messageIsError = true
            state.message = SaveFailure.message(context, FrameStrings.saveFailed())
        }
    }
}

@Composable
private fun CollageBody(state: CollageState, onBack: () -> Unit, onSave: () -> Unit) {
    val listState = rememberLazyListState()
    LaunchedEffect(state.template.id) {
        listState.animateScrollToItem(CollageTemplates.all.indexOfFirst { it.id == state.template.id }.coerceAtLeast(0))
    }
    Column(Modifier.fillMaxSize().navigationBarsPadding()) {
        val canSave = state.ready && state.frames.isNotEmpty() && !state.saving
        FrameTopBar(
            FrameStrings.collage(), onBack, FrameStrings.back(),
            actionIcon = if (canSave) FrameIcons.Save else null,
            actionLabel = CollageStrings.saveCollage(),
            onAction = if (canSave) onSave else null
        )
        TemplatePicker(state.template, state.frames.size, listState, state::pickTemplate)
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
