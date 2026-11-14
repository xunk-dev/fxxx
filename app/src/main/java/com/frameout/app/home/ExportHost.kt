package com.frameout.app.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.frameout.app.FrameOutApp
import com.frameout.app.billing.ExportGate
import com.frameout.app.billing.rememberProGate
import com.frameout.app.frames.ContactSheetSheet
import com.frameout.app.library.LibraryViewModel
import com.frameout.app.library.SavedExportKind
import com.frameout.app.trim.CaptureStatus
import com.frameout.app.trim.CaptureStatusState
import com.frameout.app.trim.OutputSavers
import com.frameout.app.trim.SaveResult
import com.frameout.app.trim.TrimOutput
import com.frameout.core.export.ContactSheetLayout
import com.frameout.core.pro.ProFeature
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.HomeStrings
import com.frameout.core.util.TrimStrings
import com.frameout.core.util.logFailure
import com.frameout.designsystem.icons.SaveKind
import com.frameout.engine.timeline.TrimRange
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private data class SavedExportSignal(
    val done: Int?,
    val total: Int?,
    val saved: com.frameout.app.trim.SavedCapture?,
    val note: String?,
    val message: String?,
    val error: Boolean
)

@Composable
internal fun BoxScope.ExportHost(visible: Boolean, initialTab: Int, onClose: () -> Unit) {
    val context = LocalContext.current
    val app = remember { context.applicationContext as FrameOutApp }
    val scope = rememberCoroutineScope()
    val gate = rememberProGate()
    val vm: LibraryViewModel = viewModel()
    val savers = remember { OutputSavers(context) }

    var styleKey by rememberSaveable { mutableStateOf<String?>(null) }
    var videoUri by rememberSaveable { mutableStateOf<String?>(null) }
    var percent by remember { mutableStateOf<Int?>(null) }
    var status by remember { mutableStateOf<CaptureStatusState?>(null) }
    var watching by remember { mutableStateOf(false) }
    var contactUris by remember { mutableStateOf<List<Uri>?>(null) }
    var folderUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val style = ExportStyle.fromKey(styleKey)

    fun backToStyles() {
        styleKey = null
        videoUri = null
    }

    fun finishAll() {
        backToStyles()
        onClose()
    }

    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) {
            backToStyles()
        } else {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            videoUri = uri.toString()
        }
    }

    fun runSaved(uris: List<Uri>, kind: SavedExportKind, tree: Uri?, contact: ContactSheetLayout.Options?) {
        if (vm.exportRunning()) {
            status = CaptureStatusState(FrameStrings.exportBusy(), error = true)
            return
        }
        watching = true
        vm.exportUris(uris, kind, tree, contact)
        finishAll()
    }

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { tree ->
        if (tree != null && folderUris.isNotEmpty()) runSaved(folderUris, SavedExportKind.Folder, tree, null) else backToStyles()
    }

    val imagesPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        if (uris.isEmpty()) {
            backToStyles()
        } else {
            when (style) {
                ExportStyle.Zip -> ExportGate.gated { runSaved(uris, SavedExportKind.Zip, null, null) }
                ExportStyle.Pdf -> ExportGate.gated { runSaved(uris, SavedExportKind.Pdf, null, null) }
                ExportStyle.Folder -> ExportGate.gated {
                    folderUris = uris
                    folderPicker.launch(null)
                }
                else -> backToStyles()
            }
        }
    }

    val contactPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        if (uris.isNotEmpty()) contactUris = (contactUris.orEmpty() + uris).distinct()
    }

    LaunchedEffect(vm) {
        vm.state
            .map { SavedExportSignal(it.exportProgress?.done, it.exportProgress?.total, it.exportSaved, it.exportNote, it.message, it.errorMessage != null) }
            .distinctUntilChanged()
            .collect { s ->
                if (!watching) return@collect
                when {
                    s.total != null -> status = CaptureStatusState(FrameStrings.saving() + " " + s.done + "/" + s.total, busy = true)
                    s.saved != null -> {
                        status = CaptureStatusState(s.note ?: FrameStrings.savedOk(), saved = s.saved)
                        watching = false
                        vm.clearExport()
                        vm.clearMessage()
                    }
                    s.message != null -> {
                        status = CaptureStatusState(s.message, error = s.error, kind = if (s.error) null else SaveKind.File)
                        watching = false
                        vm.clearExport()
                        vm.clearMessage()
                    }
                }
            }
    }

    fun startClip(output: TrimOutput, uri: Uri, range: TrimRange, positionMs: Long, durationMs: Long) {
        if (percent != null) return
        gate.use(ProFeature.AdvancedExport, output.tier, autoRun = true) {
            percent = 0
            status = CaptureStatusState(HomeStrings.exportSaving(0), busy = true)
            scope.launch {
                val result = try {
                    savers.save(output, uri, range, positionMs, durationMs) { p ->
                        percent = p
                        status = CaptureStatusState(HomeStrings.exportSaving(p), busy = true)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logFailure("HomeExport", e)
                    SaveResult.fail(FrameStrings.saveFailed())
                } finally {
                    percent = null
                }
                status = CaptureStatusState(result.message, error = result.error, kind = output.saveKind)
                if (!result.error) finishAll()
            }
        }
    }

    ExportStyleSheet(
        visible = visible && styleKey == null,
        initialTab = initialTab,
        onClose = onClose,
        onPick = { picked ->
            if (picked == ExportStyle.Contact) {
                ExportGate.gated {
                    styleKey = picked.key
                    contactUris = emptyList()
                }
            } else {
                styleKey = picked.key
                if (picked.isVideo) {
                    videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                } else {
                    imagesPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            }
        }
    )

    val output = style?.trimOutput
    if (output != null && visible) {
        videoUri?.let { raw ->
            key(raw) {
                QuickSheet(
                    uri = Uri.parse(raw),
                    onClose = { if (percent == null) backToStyles() else finishAll() },
                    onStart = { uri, range, position, duration -> startClip(output, uri, range, position, duration) },
                    title = HomeStrings.exportClipTitle(style?.title().orEmpty()),
                    confirmLabel = HomeStrings.exportAction(),
                    hint = HomeStrings.exportClipHint() + if (output.limited) " " + TrimStrings.limitHint(output.seconds) else "",
                    maxMs = output.maxMs,
                    progress = percent,
                    latchStart = false,
                    loop = output.loopsSilently
                )
            }
        }
    }

    val prefs = remember { app.container.prefs }
    val pendingContact = contactUris
    val contactPreview = rememberContactPreview(pendingContact) {
        contactPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    ContactSheetSheet(pendingContact != null, pendingContact?.size ?: 0, prefs, { contactUris = null; backToStyles() }, forSaved = true, preview = contactPreview) { options ->
        contactUris = null
        pendingContact?.let { runSaved(it, SavedExportKind.Contact, null, options) }
    }

    CaptureStatus(status, Modifier.align(Alignment.TopCenter)) { status = null }
}
