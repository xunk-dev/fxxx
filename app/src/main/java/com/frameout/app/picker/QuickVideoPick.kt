package com.frameout.app.picker

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.frameout.app.FrameOutApp
import com.frameout.core.media.PermissionHelper

@Composable
fun rememberQuickVideoPick(onPicked: (Uri) -> Unit): () -> Unit {
    val context = LocalContext.current
    val prefs = remember { (context.applicationContext as FrameOutApp).container.prefs }
    var custom by rememberSaveable { mutableStateOf(false) }
    val system = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let(onPicked) }
    if (custom) {
        QuickPickerDialog(onPicked = { uri -> custom = false; onPicked(uri) }, onClose = { custom = false })
    }
    return {
        val full = PermissionHelper.hasFullVideoAccess(context)
        val firstAsk = !full && !prefs.libraryAsked()
        if (full || firstAsk) {
            if (firstAsk) prefs.setLibraryAsked()
            custom = true
        } else {
            system.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
        }
    }
}

@Composable
private fun QuickPickerDialog(onPicked: (Uri) -> Unit, onClose: () -> Unit) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(Modifier.fillMaxSize()) {
            PickerScreen(
                onContinue = { uris -> uris.firstOrNull()?.let(onPicked) ?: onClose() },
                onClose = onClose,
                vm = viewModel(key = "picker-quick"),
                mode = PickerMode.QuickVideo
            )
        }
    }
}
