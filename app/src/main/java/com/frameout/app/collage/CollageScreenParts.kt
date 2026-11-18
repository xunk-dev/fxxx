package com.frameout.app.collage

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.frameout.app.FrameOutApp
import com.frameout.app.trim.CaptureStatus
import com.frameout.app.trim.CaptureStatusState
import com.frameout.core.pro.ProAccess
import com.frameout.core.pro.ProCatalog
import com.frameout.core.pro.ProFeature
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.components.ToastKind
import com.frameout.designsystem.components.toastDurationMs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay

@Composable
internal fun CollageToastEffect(state: CollageState) {
    LaunchedEffect(state.message) {
        if (state.message != null) {
            delay(toastDurationMs(state.message.orEmpty(), if (state.messageIsError) ToastKind.Error else ToastKind.Success))
            state.message = null
        }
    }
}

@Composable
internal fun CollageSaveStatus(state: CollageState, modifier: Modifier) {
    val status = when {
        state.saving -> CaptureStatusState(FrameStrings.saving(), busy = true)
        else -> state.result?.let { CaptureStatusState(FrameStrings.savedOk(), it) }
    }
    CaptureStatus(status, modifier) { state.result = null }
}

internal fun startCollageSave(state: CollageState, app: FrameOutApp, scope: CoroutineScope, context: Context, thenLeave: Boolean, onBack: () -> Unit) {
    val free = ProCatalog.collageIsFree(state.template.id)
    val allowed = free || !app.container.entitlements.enforced || ProAccess.permits(ProFeature.Collage)
    if (allowed) saveCollage(state, app, scope, context) { if (thenLeave) onBack() }
    else app.container.entitlements.showPaywall(ProFeature.Collage)
}
