package com.frameout.app.library

import android.app.Application
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import com.frameout.app.trim.SavedCapture
import com.frameout.core.db.SavedImageEntity
import com.frameout.core.db.getByIdsChunked
import com.frameout.core.export.ContactSheetLayout
import com.frameout.core.storage.FileSaver
import com.frameout.core.storage.FrameExporter
import com.frameout.core.util.AppContainer
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.SavedStrings
import com.frameout.core.util.logFailure
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.icons.SaveKind
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.frameout.core.export.ZipNameSet
import com.frameout.core.export.ZipNames
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

internal enum class SavedExportKind { Zip, Folder, Pdf, Contact }

internal class SavedLibraryActions(
    private val scope: CoroutineScope,
    private val app: Application,
    private val container: AppContainer,
    private val exporter: FrameExporter,
    private val state: MutableStateFlow<LibraryState>
) {
    var job: Job? = null
        private set

    private val resolver get() = app.contentResolver
    private val saver by lazy { FileSaver(app) }
    private val docs by lazy { SavedDocExport(app, container, saver) }

    fun saveToGallery() {
        val ids = state.value.selectedSaved.toList()
        if (ids.isEmpty() || state.value.exportProgress != null) return
        job = scope.launch {
            state.update { it.copy(exportProgress = ExportProgress(0, ids.size), message = null, messageSaved = null) }
            val tally = GalleryTally()
            try {
                val items = container.savedImages.getByIdsChunked(ids)
                items.forEachIndexed { i, item ->
                    tally.add(saveOne(item))
                    state.update { it.copy(exportProgress = ExportProgress(i + 1, ids.size)) }
                }
                tally.failed += ids.size - items.size
            } catch (e: CancellationException) {
                state.update { it.withMessage(FrameStrings.exportCancelled(), false, FrameIconsExtra.Cancel) }
                throw e
            } finally {
                state.update { it.copy(exportProgress = null) }
            }
            val text = tally.message()
            state.update { it.copy(selectedSaved = emptySet(), message = text, errorMessage = text.takeIf { tally.failed > 0 }, messageSaved = if (tally.saved > 0) SaveKind.Image else null) }
        }
    }

    fun export(kind: SavedExportKind, tree: Uri?, contact: ContactSheetLayout.Options? = null, only: List<Long>? = null) {
        val ids = only ?: state.value.selectedSaved.toList()
        runExport(ids.size, kind, tree, contact, only != null) { container.savedImages.getByIdsChunked(ids) }
    }

    fun exportUris(uris: List<Uri>, kind: SavedExportKind, tree: Uri?, contact: ContactSheetLayout.Options? = null) {
        runExport(uris.size, kind, tree, contact, true) { withContext(Dispatchers.IO) { uris.mapIndexed { i, uri -> pickedItem(i, uri) } } }
    }

    private fun pickedItem(index: Int, uri: Uri): SavedImageEntity {
        val name = runCatching {
            resolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull()
        val fallback = "image_${(index + 1).toString().padStart(4, '0')}"
        return SavedImageEntity(id = index + 1L, uri = uri.toString(), displayName = name?.takeIf { it.isNotBlank() } ?: fallback, createdAt = index.toLong())
    }

    private fun runExport(
        total: Int,
        kind: SavedExportKind,
        tree: Uri?,
        contact: ContactSheetLayout.Options?,
        keepSelection: Boolean,
        load: suspend () -> List<SavedImageEntity>
    ) {
        if (total == 0 || state.value.exportProgress != null) return
        if (!com.frameout.app.billing.ExportGate.permit()) {
            state.update { it.copy(message = FrameStrings.saveFailed(), messageSaved = null) }
            return
        }
        job = scope.launch {
            state.update { it.copy(exportProgress = ExportProgress(0, total), exportSaved = null, exportNote = null, message = null, messageSaved = null) }
            var outcome = ExportOutcomeTally(0, total, null)
            try {
                val items = load().filter { readable(Uri.parse(it.uri)) }
                val progress = { done: Int -> state.update { it.copy(exportProgress = ExportProgress(done, total)) } }
                outcome = withContext(Dispatchers.IO) {
                    when (kind) {
                        SavedExportKind.Zip -> toZip(items, total, progress)
                        SavedExportKind.Folder -> toFolder(items, total, tree, progress)
                        SavedExportKind.Pdf -> docs.pdf(items, total, progress)
                        SavedExportKind.Contact -> docs.contact(items, total, contact ?: ContactSheetLayout.Options(), progress)
                    }
                }
            } catch (e: CancellationException) {
                state.update { it.withMessage(FrameStrings.exportCancelled(), false, FrameIconsExtra.Cancel) }
                throw e
            } finally {
                state.update { it.copy(exportProgress = null) }
            }
            finishExport(outcome, keepSelection = keepSelection)
        }
    }

    private fun finishExport(outcome: ExportOutcomeTally, keepSelection: Boolean = false) {
        val text = when {
            outcome.reason != null -> outcome.reason
            outcome.done == 0 -> FrameStrings.saveFailed()
            outcome.done == outcome.total -> SavedStrings.exportDone(outcome.done)
            else -> SavedStrings.exportPartial(outcome.done, outcome.total)
        }
        val failed = outcome.done < outcome.total
        val kept = outcome.zip.takeIf { outcome.done > 0 }
        state.update {
            it.copy(
                selectedSaved = if (outcome.done > 0 && !keepSelection) emptySet() else it.selectedSaved,
                exportSaved = kept,
                exportNote = if (kept != null && failed) text else null,
                message = if (kept == null) text else null,
                errorMessage = if (kept == null && failed) text else null,
                messageSaved = if (outcome.done > 0 && kept == null) SaveKind.File else null
            )
        }
    }

    private suspend fun saveOne(item: SavedImageEntity): GalleryStep {
        val uri = Uri.parse(item.uri)
        return when {
            readable(uri) && uri.authority == MediaStore.AUTHORITY -> GalleryStep.Already
            readable(uri) -> if (copyToGallery(item, uri)) GalleryStep.Saved else GalleryStep.Failed
            else -> if (resaveFromSource(item)) GalleryStep.Saved else GalleryStep.Failed
        }
    }

    private suspend fun resaveFromSource(item: SavedImageEntity): Boolean {
        val frame = item.sourceFrameId?.let { container.frames.getById(it) } ?: return false
        return runCatching { exporter.resaveFromFrame(item.id, frame) }.onFailure { logFailure("SavedActions", it) }.getOrDefault(false)
    }

    private suspend fun copyToGallery(item: SavedImageEntity, source: Uri): Boolean {
        val mime = withContext(Dispatchers.IO) { runCatching { resolver.getType(source) }.getOrNull() } ?: "image/jpeg"
        val root = when {
            mime.startsWith("image/") -> "Pictures"
            mime.startsWith("video/") -> "Movies"
            else -> "Documents"
        }
        val target = saver.save(item.displayName, mime, "$root/${com.frameout.core.storage.AppFolders.NAME}", null) { out -> copyTo(source, out) }
        return target != null
    }

    private fun copyTo(source: Uri, out: OutputStream) {
        resolver.openInputStream(source)?.use { it.copyTo(out) } ?: error("Source unavailable")
    }

    private fun readable(uri: Uri): Boolean =
        runCatching { resolver.openFileDescriptor(uri, "r")?.use { true } ?: false }.getOrDefault(false)

    private suspend fun toZip(items: List<SavedImageEntity>, total: Int, progress: (Int) -> Unit): ExportOutcomeTally {
        if (items.isEmpty()) return ExportOutcomeTally(0, total, null)
        val name = "${com.frameout.core.storage.AppFolders.NAME}_${System.currentTimeMillis()}.zip"
        var written = 0
        val uri = saver.save(name, "application/zip", com.frameout.core.storage.AppFolders.DOCUMENTS, container.prefs.documentsTreeUri()) { out ->
            written = writeZip(items, out, progress)
        }
        return ExportOutcomeTally(if (uri == null) 0 else written, total, uri?.let { SavedCapture(it, "application/zip") })
    }

    private fun writeZip(items: List<SavedImageEntity>, out: OutputStream, progress: (Int) -> Unit): Int {
        val names = ZipNameSet()
        var count = 0
        ZipOutputStream(out.buffered()).use { zip ->
            zip.setLevel(1)
            items.distinctBy { it.id }.forEachIndexed { position, item ->
                val input = runCatching { resolver.openInputStream(Uri.parse(item.uri)) ?: error("Source unavailable") }
                    .onFailure { logFailure("SavedActions", it) }
                    .getOrNull()
                if (input != null) {
                    val fallback = "image_${(position + 1).toString().padStart(4, '0')}"
                    val name = names.unique(ZipNames.safeFile(item.displayName, fallback))
                    input.use {
                        zip.putNextEntry(ZipEntry(ZipNames.requireSafePath(name)))
                        it.copyTo(zip)
                        zip.closeEntry()
                    }
                    count++
                }
                progress(count)
            }
        }
        return count
    }

    private fun toFolder(items: List<SavedImageEntity>, total: Int, tree: Uri?, progress: (Int) -> Unit): ExportOutcomeTally {
        if (tree == null) return ExportOutcomeTally(0, total, null)
        val parent = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        var count = 0
        items.forEach { item ->
            val source = Uri.parse(item.uri)
            val mime = runCatching { resolver.getType(source) }.getOrNull() ?: "image/jpeg"
            val target = runCatching { DocumentsContract.createDocument(resolver, parent, mime, item.displayName) ?: error("Create failed") }
                .onFailure { logFailure("SavedActions", it) }
                .getOrNull()
            val ok = target != null && runCatching {
                resolver.openOutputStream(target)?.use { copyTo(source, it) } ?: error("Output unavailable")
            }.onFailure {
                logFailure("SavedActions", it)
                runCatching { DocumentsContract.deleteDocument(resolver, target) }
            }.isSuccess
            if (ok) count++
            progress(count)
        }
        return ExportOutcomeTally(count, total, null)
    }
}

internal class ExportOutcomeTally(val done: Int, val total: Int, val zip: SavedCapture?, val reason: String? = null)

internal enum class GalleryStep { Saved, Already, Failed }

internal class GalleryTally {
    var saved = 0
    var already = 0
    var failed = 0

    fun add(step: GalleryStep) {
        when (step) {
            GalleryStep.Saved -> saved++
            GalleryStep.Already -> already++
            GalleryStep.Failed -> failed++
        }
    }

    fun message(): String = when {
        saved == 0 && failed == 0 -> SavedStrings.galleryAllThere()
        else -> listOfNotNull(
            SavedStrings.galleryDone(saved).takeIf { saved > 0 },
            SavedStrings.galleryThere(already).takeIf { already > 0 },
            SavedStrings.galleryFailed(failed).takeIf { failed > 0 }
        ).joinToString(FrameStrings.sep())
    }
}
