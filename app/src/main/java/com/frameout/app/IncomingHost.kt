package com.frameout.app

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import com.frameout.designsystem.components.FrameSpinner

internal fun IncomingMedia.Stabilized.noticeRes(): Int? = when {
    failed > 0 -> R.string.incoming_copy_failed
    rejected > 0 -> R.string.incoming_unsupported
    stillsFailed > 0 && stills.isEmpty() -> R.string.incoming_photo_failed
    stillsSkipped > 0 -> R.string.incoming_photos_skipped
    else -> null
}

internal fun incomingPending(): Boolean = IncomingPrep.busy.value || IncomingPrep.result.value != null

@Composable
internal fun IncomingShareHost(
    activity: ComponentActivity,
    incoming: List<Uri>,
    ready: Boolean,
    onIncomingHandled: () -> Unit,
    openPicker: (List<Uri>) -> Unit,
    openStills: (List<com.frameout.core.db.FrameEntity>) -> Unit,
    notice: (String) -> Unit
) {
    val result by IncomingPrep.result.collectAsState()
    val busy by IncomingPrep.busy.collectAsState()
    LaunchedEffect(incoming) {
        if (incoming.isEmpty()) return@LaunchedEffect
        IncomingPrep.start(activity, incoming)
        onIncomingHandled()
    }
    BackHandler(enabled = busy) { IncomingPrep.cancel() }
    LaunchedEffect(result, ready) {
        if (result == null || !ready) return@LaunchedEffect
        val done = IncomingPrep.take() ?: return@LaunchedEffect
        done.noticeRes()?.let { notice(activity.getString(it)) }
        if (done.ready.isNotEmpty()) openPicker(done.ready)
        else if (done.stills.isNotEmpty()) openStills(done.stills)
    }
}

@Composable
internal fun IncomingBusyOverlay() {
    val busy by IncomingPrep.busy.collectAsState()
    if (!busy) return
    Box(
        Modifier
            .fillMaxSize()
            .zIndex(10f)
            .background(Color.Black.copy(alpha = 0.4f))
            .pointerInput(Unit) { detectTapGestures { } },
        Alignment.Center
    ) {
        PreferredFrameTheme { FrameSpinner() }
    }
}
