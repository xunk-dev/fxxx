package com.frameout.app.library

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.frameout.app.LocalTabNav
import com.frameout.app.home.HomeQuickFab
import com.frameout.app.home.QuickSheet
import com.frameout.app.home.rememberExtractStrip
import com.frameout.engine.timeline.TrimRange

internal class LibraryQuick(val fab: @Composable () -> Unit, val sheet: @Composable () -> Unit)

@Composable
internal fun rememberLibraryQuick(onQuickStart: (Uri, TrimRange, () -> Unit) -> Unit): LibraryQuick {
    val context = LocalContext.current
    var quickUri by rememberSaveable { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            quickUri = uri.toString()
        }
    }
    val strip by rememberExtractStrip()
    val nav = LocalTabNav.current
    return LibraryQuick(
        fab = {
            HomeQuickFab(
                strip,
                onStart = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) },
                onOpen = nav.onOpenProject
            )
        },
        sheet = {
            quickUri?.let { raw ->
                key(raw) {
                    QuickSheet(
                        uri = Uri.parse(raw),
                        onClose = { quickUri = null },
                        snapshot = true,
                        compact = true,
                        onStart = { uri, range, _, _ -> onQuickStart(uri, range) { quickUri = null } }
                    )
                }
            }
        }
    )
}
