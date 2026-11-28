package com.frameout.app.compare

import com.frameout.core.db.getByIdsChunked
import com.frameout.core.storage.SaveFailure
import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.frameout.app.FrameOutApp
import com.frameout.app.billing.lockReport
import com.frameout.app.viewer.FrameBitmapLoader
import com.frameout.core.db.FrameEntity
import com.frameout.core.edit.FrameEdit
import com.frameout.core.edit.toEdit
import com.frameout.core.storage.FrameExporter
import com.frameout.core.util.EditedThumb
import com.frameout.core.util.FrameStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CompareState(val frames: List<FrameEntity> = emptyList(), val bitmaps: List<Bitmap?> = emptyList(), val loading: Boolean = true, val message: String? = null, val busyMessage: String? = null, val messageSaved: com.frameout.designsystem.icons.SaveKind? = null, val isError: Boolean = false, val diff: Bitmap? = null, val diffPercent: Float = 0f, val diffBusy: Boolean = false, val fullReady: Boolean = false, val lockPrompt: com.frameout.core.pro.ProEditGuard.LockReport? = null) {
    val messageBusy: Boolean get() = message != null && message == busyMessage
}

class CompareViewModel(app: Application) : AndroidViewModel(app) {
    private val c = (app as FrameOutApp).container
    private val exporter = FrameExporter(app, c.storage, c.savedImages, c.frameEdits, c.projects, c.videoIndex)
    private val loader = FrameBitmapLoader(app)
    private val _state = MutableStateFlow(CompareState())
    val state: StateFlow<CompareState> = _state.asStateFlow()
    private var openedIds: List<Long>? = null
    private var openJob: kotlinx.coroutines.Job? = null

    init {
        viewModelScope.launch {
            _state.map { Triple(it.message, it.messageBusy, it.isError) }.distinctUntilChanged().collectLatest { (text, busy, error) ->
                if (text != null && !busy) {
                    kotlinx.coroutines.delay(com.frameout.designsystem.components.toastDurationMs(text, if (error) com.frameout.designsystem.components.ToastKind.Error else com.frameout.designsystem.components.ToastKind.Success))
                    clearMessage()
                }
            }
        }
    }

    fun open(frameIds: List<Long>) {
        if (openedIds == frameIds) return
        openedIds = frameIds
        openJob?.cancel()
        _state.update { CompareState() }
        openJob = viewModelScope.launch {
            val frames = c.frames.getByIdsChunked(frameIds).sortedBy { frameIds.indexOf(it.id) }
            val edits = withContext(Dispatchers.IO) { frames.map { c.editHistory.current(it.id)?.toEdit() ?: FrameEdit(it.id) } }
            val previews = frames.mapIndexed { i, f -> EditedThumb.peek(f.thumbnailPath ?: f.fullPath, edits[i]) }
            _state.update { it.copy(frames = frames, bitmaps = previews, loading = previews.any { p -> p == null }) }
            val quick = frames.mapIndexed { i, f ->
                async { previews[i] ?: runCatching { EditedThumb.load(f.thumbnailPath ?: f.fullPath, edits[i]) }.getOrNull() }
            }.awaitAll()
            _state.update { cur -> if (cur.fullReady) cur else cur.copy(bitmaps = cur.bitmaps.mapIndexed { i, b -> b ?: quick.getOrNull(i) }, loading = false) }
            val fulls = loadFulls(frames, edits)
            _state.update { it.copy(loading = false, fullReady = true, bitmaps = it.bitmaps.mapIndexed { i, b -> fulls.getOrNull(i) ?: b }) }
        }
    }

    fun reset() {
        openedIds = null
        openJob?.cancel()
        _state.update { CompareState() }
    }

    private suspend fun loadFulls(frames: List<FrameEntity>, edits: List<FrameEdit>): List<Bitmap?> = coroutineScope {
        frames.mapIndexed { i, f ->
            async {
                val full = withContext(Dispatchers.IO) {
                    try {
                        loadOne(f, edits[i])
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        com.frameout.core.util.logFailure("CompareLoad", e)
                        null
                    } catch (e: OutOfMemoryError) {
                        null
                    }
                }
                _state.update { cur ->
                    val list = cur.bitmaps.toMutableList()
                    if (i < list.size && full != null) list[i] = full
                    cur.copy(bitmaps = list)
                }
                full
            }
        }.awaitAll()
    }

    internal val lockGate = com.frameout.app.billing.LockGate { report -> _state.update { it.copy(lockPrompt = report) } }
    private var saving = false

    private fun runSave(block: suspend () -> Triple<String, Boolean, Boolean>) {
        if (saving) return
        saving = true
        val busyText = FrameStrings.saving()
        _state.update { it.copy(message = busyText, busyMessage = busyText, messageSaved = null, isError = false) }
        viewModelScope.launch {
            var result = Triple(SaveFailure.message(getApplication<Application>(), FrameStrings.saveFailed()), false, false)
            try {
                result = block()
            } catch (e: kotlinx.coroutines.CancellationException) {
                _state.update { if (it.messageBusy) it.copy(message = null, busyMessage = null) else it }
                throw e
            } catch (e: Exception) {
                com.frameout.core.util.logFailure("CompareSave", e)
            } finally {
                saving = false
            }
            val (text, ok, error) = result
            _state.update { it.copy(message = text, busyMessage = null, messageSaved = if (ok) com.frameout.designsystem.icons.SaveKind.Image else null, isError = error) }
        }
    }

    fun saveAll() {
        val frames = _state.value.frames
        if (frames.isEmpty()) return
        viewModelScope.launch {
            lockGate.gate(c.lockReport(frames.map { it.id })) { note ->
                runSave {
                    val saved = exporter.saveToGallery(frames)
                    val lost = exporter.lastMetadataFailures
                    val text = if (saved > 0 && lost > 0) FrameStrings.savedNoMetadata(lost) else if (saved == frames.size) (note ?: FrameStrings.savedOk()) else if (saved > 0) FrameStrings.savedN(saved) else SaveFailure.message(getApplication<Application>(), FrameStrings.saveFailed())
                    Triple(text, saved > 0, saved <= 0)
                }
            }
        }
    }

    fun save(position: Int) {
        val frame = _state.value.frames.getOrNull(position) ?: return
        viewModelScope.launch {
            lockGate.gate(c.lockReport(listOf(frame.id))) { note ->
                runSave {
                    val ok = exporter.saveToGallery(listOf(frame)) > 0
                    Triple(if (ok) (note ?: FrameStrings.savedOk()) else SaveFailure.message(getApplication<Application>(), FrameStrings.saveFailed()), ok, !ok)
                }
            }
        }
    }

    fun ensureDiff() {
        val s = _state.value
        if (s.diff != null || s.diffBusy) return
        if (!s.fullReady) return
        val a = s.bitmaps.getOrNull(0) ?: return
        val b = s.bitmaps.getOrNull(1) ?: return
        _state.update { it.copy(diffBusy = true) }
        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) { runCatching { DiffMaker.make(a, b) }.getOrNull() }
            _state.update { it.copy(diff = result?.bitmap, diffPercent = result?.percent ?: 0f, diffBusy = false) }
        }
    }

    fun clearMessage() = _state.update { if (it.messageBusy) it else it.copy(message = null, busyMessage = null, messageSaved = null, isError = false) }

    private suspend fun loadOne(frame: FrameEntity, edit: FrameEdit): Bitmap? {
        val source = c.projects.getById(frame.projectId)?.sourceUri?.let(Uri::parse)
        return loader.load(frame, source, edit)
    }
}
