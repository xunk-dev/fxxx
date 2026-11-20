package com.frameout.app.library

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.frameout.app.LocalTabNav
import com.frameout.app.picker.rememberQuickVideoPick
import com.frameout.app.home.HomeQuickFab
import com.frameout.app.home.QuickSheet
import com.frameout.app.home.rememberExtractStrip
import com.frameout.engine.timeline.TrimRange

internal class LibraryQuick(val fab: @Composable () -> Unit, val sheet: @Composable () -> Unit)

@Composable
internal fun rememberLibraryQuick(onQuickStart: (Uri, TrimRange, () -> Unit) -> Unit): LibraryQuick {
    val context = LocalContext.current
    var quickUri by rememberSaveable { mutableStateOf<String?>(null) }
    val pickVideo = rememberQuickVideoPick { uri ->
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        quickUri = uri.toString()
    }
    val stripState = rememberExtractStrip()
    val nav = LocalTabNav.current
    val start by rememberUpdatedState(pickVideo)
    val fab: @Composable () -> Unit = remember(stripState, nav) {
        @Composable { HomeQuickFab(stripState.value, onStart = start, onOpen = nav.onOpenProject) }
    }
    return LibraryQuick(
        fab = fab,
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
