package com.frameout.app.viewer

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.frameout.app.FrameOutApp
import com.frameout.core.db.FrameEditEntity
import com.frameout.core.db.FrameEntity
import com.frameout.core.edit.FrameEdit
import com.frameout.core.storage.FrameExporter
import com.frameout.core.util.EditedThumb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext

class ViewerViewModel(app: Application) : AndroidViewModel(app) {
    internal val c = (app as FrameOutApp).container
    private val exporter = FrameExporter(app, c.storage, c.savedImages, c.frameEdits, c.projects, c.videoIndex)
    internal val loader = FrameBitmapLoader(app)
    internal val editHistory = c.editHistory
    internal val _state = MutableStateFlow(ViewerState())
    private val player = ViewerPlayer(viewModelScope, _state, c.frames, { loadFrame(it, seamless = true) }, { refreshNeighbors() })
    private val export = ViewerExport(viewModelScope, exporter, _state, app) { frame, savedId ->
        if (replaceSavedId == null && frame.projectId == _state.value.snapshotProject) {
            replaceSavedId = savedId
            replaceFrameId = frame.id
        }
    }
    internal val tools = ViewerToolEditor(viewModelScope, _state, loader, editHistory, { sourceUri }, { loadFrame(_state.value.current) }, ::stashFinal)
    val state: StateFlow<ViewerState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.map { it.message to it.messageBusy }.distinctUntilChanged().collectLatest { (text, busy) ->
                if (text != null && !busy) {
                    kotlinx.coroutines.delay(com.frameout.designsystem.components.toastDurationMs(text, com.frameout.designsystem.components.ToastKind.Success))
                    clearMessage()
                }
            }
        }
        viewModelScope.launch {
            _state.map { it.deletedStack to it.savedUndoCount }.distinctUntilChanged().collectLatest {
                if (_state.value.hasUndo) {
                    kotlinx.coroutines.delay(6000)
                    dismissUndo()
                }
            }
        }
    }
    internal var sourceUri: Uri? = null
    internal var loadGeneration = 0L
    private var retired: Bitmap? = null
    internal val cache = WindowCache<Bitmap>(
        budgetBytes = (Runtime.getRuntime().maxMemory() / 5L).coerceIn(64L * 1024L * 1024L, 160L * 1024L * 1024L),
        maxEntryBytes = MAX_PREFETCH_BYTES.toLong(),
        sizeOf = { it.allocationByteCount.toLong() },
        drop = { it.takeUnless { b -> b.isRecycled }?.recycle() }
    )
    private class Handoff(val frameId: Long, val edit: FrameEdit, val bitmap: Bitmap)
    private var handoff: Handoff? = null
    internal val inFlight = HashMap<Long, Job>()
    internal val decodeGate = Semaphore(2)
    internal var windowJob: Job? = null
    internal var navJob: Job? = null
    internal var pendingSteps = 0
    internal var direction = 1
    internal val screenPx = app.resources.displayMetrics.let { minOf(it.widthPixels, it.heightPixels) }
    internal var replaceSavedId: Long? = null
    internal var replaceFrameId: Long? = null
    internal var savedIds: List<Long> = emptyList()
    internal var savedIndex = 0
    internal val savedDelete = ViewerSavedDelete(c.storage, c.savedImages, com.frameout.core.storage.GalleryBackup(app))

    fun open(projectId: Long, frameId: Long, savedId: Long? = null, openEdit: Boolean = false) {
        replaceSavedId = savedId
        replaceFrameId = savedId?.let { frameId }
        savedIds = if (savedId == null) emptyList() else SavedPlaylist.ids.takeIf { savedId in it } ?: listOf(savedId)
        savedIndex = savedId?.let { savedIds.indexOf(it) } ?: 0
        _state.update { it.copy(savedMode = savedId != null) }
        viewModelScope.launch {
            val home = c.projects.getBySourceUri(com.frameout.core.db.SnapshotHome.URI)?.id ?: -1L
            _state.update { it.copy(snapshotProject = home) }
            sourceUri = c.projects.getById(projectId)?.sourceUri?.let(Uri::parse)
            loadFrame(c.frames.getActiveById(frameId), onReady = if (openEdit) ({ openToolsFor(frameId) }) else null)
        }
    }
    private fun openToolsFor(frameId: Long) {
        val now = _state.value
        if (now.current?.id == frameId && now.bitmap != null && now.tool == EditTool.None) openTools()
    }
    fun openTools(tool: EditTool = EditTool.Adjust) { stopPlay(); resetLiveText(); tools.open(tool) }
    fun blurDetection(detection: Detection) { detect.cancel(); stopPlay(); resetLiveText(); tools.open(EditTool.Cover, listOf(DetectionBlur.cover(detection))) }
    fun togglePlay() { resetLiveText(); detect.cancel(); player.toggle() }
    fun stopPlay() = player.stop()
    private val live = ViewerLiveText(viewModelScope, _state, app) { stopPlay() }
    internal val detect = ViewerDetect(viewModelScope, _state, c.frameTags) { stopPlay() }
    private fun resetLiveText() = live.reset()
    fun toggleLiveText() = live.toggle()
    fun liveSelect(anchor: Int, focus: Int) = live.select(anchor, focus)
    fun liveTapWord(index: Int) = live.tapWord(index)
    fun liveClearSelection() = live.clearSelection()
    fun liveSelectAll() = live.selectAll()
    fun liveCopy() = live.copy()
    fun undoEdit() = stepHistory { editHistory.undo(it) }
    fun redoEdit() = stepHistory { editHistory.redo(it) }
    private fun stepHistory(op: suspend (Long) -> FrameEditEntity?) {
        val frame = _state.value.current ?: return
        viewModelScope.launch { op(frame.id)?.let { applyHistory(it) }; loadFrame(_state.value.current) }
    }
    private fun editAllowed(): Boolean = com.frameout.core.pro.ProEditGuard.enforce(_state.value.edit)
    fun save() {
        val frame = _state.value.current ?: return
        if (!editAllowed()) return
        val savedId = replaceSavedId
        if (savedId != null && frame.id == replaceFrameId) export.replaceSaved(savedId, frame) else export.save(frame)
    }
    fun shareIntent(onReady: (Intent) -> Unit) { if (editAllowed()) _state.value.current?.let { export.share(it, onReady) } }
    fun search(context: android.content.Context) { if (editAllowed()) _state.value.current?.let { export.search(context, it) } }
    private fun applyHistory(entity: FrameEditEntity) {
        _state.update { it.withEdit(fromEntity(entity)) }
    }
    private fun stashFinal(frameId: Long, edit: FrameEdit, bitmap: Bitmap) {
        handoff?.bitmap?.takeUnless { it.isRecycled }?.recycle()
        handoff = Handoff(frameId, edit, bitmap)
    }

    private fun takeHandoff(frameId: Long, edit: FrameEdit): Bitmap? {
        val h = handoff ?: return null
        handoff = null
        if (h.frameId == frameId && h.edit == edit && !h.bitmap.isRecycled) return h.bitmap
        h.bitmap.takeUnless { it.isRecycled }?.recycle()
        return null
    }

    internal suspend fun loadFrame(frame: FrameEntity?, seamless: Boolean = false, onReady: (() -> Unit)? = null) {
        if (seamless && frame != null) {
            loadSeamless(frame)
            return
        }
        val generation = ++loadGeneration
        windowJob?.cancel()
        detect.cancel()
        resetLiveText()
        val before = _state.value
        val old = before.bitmap
        if (frame == null) { _state.update { it.cleared() }; old?.takeUnless { it.isRecycled }?.recycle(); return }
        val edit = editHistory.current(frame.id)?.let(::fromEntity) ?: FrameEdit(frame.id)
        val ready = cache.take(frame.id, edit) ?: takeHandoff(frame.id, edit)
        val waiting = ready == null && inFlight.containsKey(frame.id)
        val sharpPlaceholder = if (ready == null) {
            EditedThumb.peekSharp(frame.fullPath, edit, screenPx) ?: neighborThumb(before, frame)
        } else null
        val placeholder = sharpPlaceholder ?: if (ready == null && edit.isIdentity) placeholderFor(frame) else null
        val kept = if (ready == null && before.current?.id == frame.id) old?.takeUnless { it.isRecycled } else null
        if (kept != null) _state.update { it.showing(frame, edit, kept, null, false) }
        else _state.update { it.showing(frame, edit, ready, placeholder, sharpPlaceholder != null) }
        if (kept == null) keepOrRecycle(before.current, before.edit, old, frame)
        retired?.takeUnless { it.isRecycled }?.recycle()
        retired = null
        refreshPosition(frame)
        val neighbors = viewModelScope.async { neighborsFor(frame) }
        viewModelScope.launch {
            val found = neighbors.await()
            if (generation == loadGeneration) _state.update { if (it.current?.id == frame.id) it.withNeighbors(found) else it }
        }
        if (ready == null) launchWindow(frame, full = false)
        if (ready == null && !loadMissing(frame, edit, waiting, placeholder, sharpPlaceholder != null, generation, neighbors)) return
        if (kept != null && _state.value.bitmap !== kept) kept.takeUnless { it.isRecycled }?.recycle()
        if (generation == loadGeneration) onReady?.invoke()
        launchWindow(frame)
        restoreDetections(frame, edit, generation)
    }

    private suspend fun restoreDetections(frame: FrameEntity, edit: FrameEdit, generation: Long) {
        val stored = withContext(Dispatchers.IO) { ViewerDetections.load(c.frameTags, frame, edit, ignoreStored = _state.value.projectless) }
        if (generation != loadGeneration) return
        _state.update { it.copy(detections = stored.detections, detectionsReady = stored.complete) }
        if (!stored.complete && _state.value.showDetections) detect.run()
    }

    private suspend fun loadMissing(
        frame: FrameEntity,
        edit: FrameEdit,
        waiting: Boolean,
        placeholder: Bitmap?,
        sharp: Boolean,
        generation: Long,
        neighbors: kotlinx.coroutines.Deferred<*>
    ): Boolean {
        if (!sharp) upgradePlaceholder(frame, edit, generation)
        val bmp = (if (waiting) awaitPrefetch(frame.id, edit) else null)
            ?: withContext(Dispatchers.IO) { loader.load(frame, sourceUri, edit) }
        if (generation != loadGeneration) { bmp?.takeUnless { it.isRecycled }?.recycle(); neighbors.cancel(); return false }
        if (bmp == null) {
            _state.update { it.copy(loading = false, message = com.frameout.core.util.FrameStrings.viewerLoadFailed(), busyMessage = null, messageSaved = null, messageIcon = null) }
            return true
        }
        _state.update { it.copy(bitmap = bmp, placeholder = null, placeholderSharp = false, loading = false) }
        return true
    }

    override fun onCleared() {
        player.stop()
        savedDelete.commit()
        navJob?.cancel()
        windowJob?.cancel()
        inFlight.values.forEach { it.cancel() }
        inFlight.clear()
        cache.clear()
        handoff?.bitmap?.takeUnless { it.isRecycled }?.recycle()
        handoff = null
        _state.value.bitmap?.takeUnless { it.isRecycled }?.recycle()
        retired?.takeUnless { it.isRecycled }?.recycle()
        retired = null
        super.onCleared()
    }

    private suspend fun loadSeamless(frame: FrameEntity) {
        val generation = ++loadGeneration
        resetLiveText()
        detect.cancel()
        val uri = sourceUri
        val edit = editHistory.current(frame.id)?.let(::fromEntity) ?: FrameEdit(frame.id)
        direction = 1
        val bmp = cache.take(frame.id, edit)
            ?: (if (inFlight.containsKey(frame.id)) awaitPrefetch(frame.id, edit) else null)
            ?: withContext(Dispatchers.IO) { loader.load(frame, uri, edit) }
        if (generation != loadGeneration) { bmp?.takeUnless { it.isRecycled }?.recycle(); return }
        val old = _state.value.bitmap
        if (bmp == null) {
            _state.update { it.copy(current = frame, edit = edit) }
            return
        }
        _state.update { it.copy(current = frame, bitmap = bmp, placeholder = null, loading = false, edit = edit, detections = emptyList(), detectionsReady = false, detecting = false, showDetections = false, previous = null, next = null, previousBitmap = null, nextBitmap = null) }
        retired?.takeUnless { it.isRecycled }?.recycle()
        retired = old
        refreshPosition(frame)
        launchWindow(frame)
    }
}
