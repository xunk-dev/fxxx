package com.frameout.app.frames

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import com.frameout.app.FrameOutApp
import com.frameout.app.billing.lockReport
import com.frameout.core.db.FrameEntity
import com.frameout.core.db.getByIdsChunked
import com.frameout.core.db.setFavoriteAtomic
import com.frameout.core.edit.FrameEdit
import com.frameout.core.edit.toEdit
import com.frameout.core.storage.FrameExporter
import com.frameout.core.util.logged
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock

private const val GALLERY_CONFIRM_THRESHOLD = 200
internal const val GALLERY_UNDO_WINDOW_MS = 10000L
private const val COUNTS_THROTTLE_MS = 400L

class FramesViewModel(app: Application) : AndroidViewModel(app) {
    internal val container = (app as FrameOutApp).container
    private val exporter = FrameExporter(app, container.storage, container.savedImages, container.frameEdits, container.projects, container.videoIndex)
    private val queries = FramesQueries(container)
    internal val _state = MutableStateFlow(FramesState())
    val state: StateFlow<FramesState> = _state.asStateFlow()
    internal var projectId = -1L
    internal var analysisJob: kotlinx.coroutines.Job? = null

    internal val smartRun = SmartRun()
    fun load(id: Long, heavy: Boolean = true) {
        if (projectId != id) {
            cancelAnalysis()
            if (projectId >= 0) {
                tags.stop()
                countsJob?.cancel()
                countsJob = null
                _state.update { FramesState(fit = it.fit) }
            }
        }
        projectId = id
        watchCounts(id)
        viewModelScope.launch { refresh(id, heavy); if (heavy && SmartAccess.allowedSilently(container.entitlements, _state.value.project)) tags.ensureScan() }
    }
    private var countsJob: kotlinx.coroutines.Job? = null
    private var countsId = -1L

    private fun watchCounts(id: Long) {
        if (countsId == id && countsJob?.isActive == true) return
        countsJob?.cancel()
        countsId = id
        countsJob = viewModelScope.launch {
            combine(container.frames.observeActiveCount(id), container.frames.observeFavoriteCount(id), container.frames.observeDeletedCount(id), container.projects.observeById(id).map { it?.status }.distinctUntilChanged()) { a, f, d, st -> listOf(a, f, d, st) }
                .distinctUntilChanged()
                .conflate()
                .collect {
                    if (projectId == id) refresh(id, false)
                    delay(COUNTS_THROTTLE_MS)
                }
        }
    }

    fun onExtractionFinished(id: Long) {
        _state.update { it.copy(integrity = null, integrityOpen = false) }
        load(id, heavy = true)
        viewModelScope.launch {
            refresh(id, false)
            if (projectId != id) return@launch
            val s = _state.value
            if (s.filter == FrameFilter.Sharpest && s.sharpestStale && !s.analyzing) showSharpest()
            if (s.smartFilter != SmartFilter.None) refreshSmartAfterExtraction(s.smartFilter, id)
        }
    }

    fun postMessage(text: String, busy: Boolean = false, @androidx.annotation.DrawableRes icon: Int? = null) = _state.update { it.copy(message = text, busyMessage = if (busy) text else null, messageSaved = null, messageIcon = icon, exportSaved = null, exportNote = null) }
    fun postBusy(text: String) = postMessage(text, busy = true)
    fun clearBusy() = _state.update { if (it.messageBusy) it.copy(message = null, busyMessage = null) else it }
    val exportRunning: Boolean get() = _state.value.messageBusy
    private val exportGuard = FramesExportGuard({ exportRunning }, ::postBusy)
    internal fun exportStarted(job: kotlinx.coroutines.Job?) = exportGuard.started(job)
    internal fun exportEnded() = exportGuard.ended()
    fun holdBack(): Boolean = exportGuard.holdBack()
    fun postExport(outcome: ExportOutcome) = _state.update { it.copy(message = outcome.message.takeIf { outcome.saved == null && it.isNotBlank() }, busyMessage = null, messageIcon = null, exportSaved = outcome.saved, exportNote = outcome.note.takeIf { outcome.saved != null }) }
    fun clearMessage() = _state.update { if (it.messageBusy) it else it.copy(message = null, busyMessage = null, messageSaved = null, messageIcon = null, undoGalleryIds = emptyList()) }
    fun clearUndoDelete() = _state.update { it.copy(undoDelete = emptyList()) }
    fun undoDelete() {
        val ids = _state.value.undoDelete
        if (ids.isEmpty()) return
        _state.update { it.copy(undoDelete = emptyList()) }
        viewModelScope.launch { container.editHistory.restoreMany(ids); refresh(projectId) }
    }
    fun clearExport() = _state.update { it.copy(exportSaved = null, exportNote = null) }
    fun setFilter(filter: FrameFilter) {
        if (filter == FrameFilter.Tagged) return
        if (filter == FrameFilter.Sharpest) { showSharpest(); return }
        if (filter == FrameFilter.Albums) {
            if (_state.value.filter != FrameFilter.Albums) { cancelAnalysis(); withSmart { tags.showAlbums() } }
            return
        }
        if (_state.value.filter == filter && _state.value.tagFilter.isEmpty() && !_state.value.analyzing) return
        cancelAnalysis()
        _state.update { it.copy(filter = filter, tagFilter = emptySet(), smartFilter = SmartFilter.None, smartIds = emptySet(), selected = emptySet(), loading = true) }
        viewModelScope.launch { refresh(projectId) }
    }
    private val tags = FramesTags(viewModelScope, container, _state, { projectId }, ::refresh)
    private val edits = FramesEdits(viewModelScope, container, _state)
    private val purge = FramesPurge(viewModelScope, container, _state, { projectId }, ::refresh)
    private val crop = FramesCropAll(viewModelScope, container, _state, getApplication<Application>(), ::selectedFrames)
    fun openTools() = _state.update { it.copy(toolsOpen = true) }
    fun closeTools() = _state.update { it.copy(toolsOpen = false) }
    fun openFilter() = _state.update { it.copy(filterOpen = true) }
    fun closeFilter() = _state.update { it.copy(filterOpen = false) }
    fun openTags() = withSmart { tags.open() }
    fun closeTags() = tags.close()
    fun toggleTag(tag: String) = tags.toggle(tag)
    fun openAlbum(tag: String) = tags.toggle(tag)
    suspend fun albumCover(tag: String) = tags.cover(tag)
    suspend fun albumFocus(frameId: Long, tagSet: Set<String>) = tags.focus(frameId, tagSet)
    fun clearTags() = tags.clear()
    fun rescanTags() = withSmart { tags.rescan() }
    fun openCropAll() { crop.open() }
    fun updateCropAll(l: Float, t: Float, r: Float, b: Float) = crop.update(l, t, r, b)
    fun resetCropAll() = crop.reset()
    fun cancelCropAll() = crop.cancel()
    fun applyCropAll() = crop.apply()
    fun toggleIntegrity() = _state.update { if (it.integrity == null) it else it.copy(integrityOpen = !it.integrityOpen) }
    fun toggleFit() = _state.update { it.copy(fit = if (it.fit == GridFit.Fill) GridFit.Fit else GridFit.Fill) }
    fun toggleSelect(id: Long) = _state.update { it.toggle(id) }
    fun selectAll() {
        viewModelScope.launch {
            val ids = queries.ids(projectId, _state.value)
            _state.update { it.copy(selected = ids.toSet()) }
        }
    }
    fun clearSelection() = _state.update { it.clearSelection() }
    fun showSharpest() {
        if (projectId < 0) return
        val now = _state.value
        if (now.filter == FrameFilter.Sharpest && (now.analyzing || (now.sharpestCount > 0 && !now.sharpestStale))) return
        withSmart {
            val pid = projectId
            cancelAnalysis()
            _state.update { it.copy(filter = FrameFilter.Sharpest, tagFilter = emptySet(), smartFilter = SmartFilter.None, smartIds = emptySet(), selected = emptySet(), loading = true, analyzing = true, analyzeDone = 0, analyzeTotal = 0, refining = false) }
            val token = smartRun.seq
            analysisJob = viewModelScope.launch { loadSharpest(pid, token) }
        }
    }
    fun dismissSmartReextract() = _state.update { it.copy(smartReextract = false) }
    fun clearSmartFilter() {
        if (_state.value.smartFilter == SmartFilter.None) return
        cancelAnalysis()
        _state.update { it.copy(smartFilter = SmartFilter.None, smartIds = emptySet(), selected = emptySet(), loading = true) }
        viewModelScope.launch { refresh(projectId) }
    }
    fun showAllFrames() {
        clearSmartFilter()
        if (_state.value.filter == FrameFilter.Sharpest) setFilter(FrameFilter.All)
    }
    fun rename(name: String) {
        val value = com.frameout.core.util.NameSanitizer.clean(name)
        if (projectId < 0 || value.isBlank()) return
        viewModelScope.launch {
            container.projects.rename(projectId, value, System.currentTimeMillis())
            com.frameout.engine.work.ExtractNotifications.invalidateTitle(projectId)
            _state.update { s -> s.copy(project = s.project?.copy(name = value)) }
        }
    }
    fun softDeleteSelected() {
        val ids = _state.value.selected.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            container.editHistory.deleteMany(ids)
            _state.update { it.clearSelection().copy(message = null, undoDelete = ids) }
            refresh(projectId)
        }
    }
    fun permanentDeleteSelected() = purge.deleteSelected()
    fun emptyTrash() = purge.emptyTrash()
    fun restoreSelected() = mutateSelection { ids -> container.editHistory.restoreMany(ids) }
    fun toggleFavoriteSelected() {
        viewModelScope.launch {
            val frames = selectedFrames()
            if (frames.isEmpty()) return@launch
            val value = !frames.all { it.isFavorite }
            container.database.setFavoriteAtomic(frames.map { it.id }, value)
            refresh(projectId)
        }
    }
    fun toggleFavorite(id: Long, currently: Boolean) = viewModelScope.launch { container.frames.setFavorite(id, !currently); refresh(projectId) }
    fun applyRotate90() = edits.transform { it.rotate90() }
    fun applyFlipH() = edits.transform { it.toggleFlipH() }
    fun applyFlipV() = edits.transform { it.toggleFlipV() }
    fun undoSelected() = edits.history { container.editHistory.undoMany(it) }
    fun redoSelected() = edits.history { container.editHistory.redoMany(it) }
    fun saveSelected() {
        val count = _state.value.selected.size
        if (count > GALLERY_CONFIRM_THRESHOLD) _state.update { it.copy(confirmGallerySave = count) } else confirmGallerySave()
    }
    fun dismissGallerySave() = _state.update { it.copy(confirmGallerySave = 0) }
    private val gallery = FramesGallerySave(viewModelScope, getApplication<Application>(), exporter, _state, ::selectedFrames)
    private val unsaved = FramesUnsaved(viewModelScope, getApplication<Application>(), container, exporter, _state, { projectId }, { lockGate })
    fun refreshUnsaved() = unsaved.refresh()
    fun saveUnsavedThen(then: () -> Unit) = unsaved.saveThen(then)
    fun cancelGallerySave() = gallery.cancel()
    internal val lockGate = com.frameout.app.billing.LockGate { report -> _state.update { it.copy(lockPrompt = report) } }
    private fun gateFrames(ids: Collection<Long>, run: (String?) -> Unit) {
        viewModelScope.launch { lockGate.gate(container.lockReport(ids), run) }
    }
    fun confirmGallerySave() {
        _state.update { it.copy(confirmGallerySave = 0) }
        gateFrames(_state.value.selected) { note -> gallery.confirm(note) }
    }
    private val galleryDelete = FramesGalleryDelete(viewModelScope, container, _state)
    fun undoGallerySave() = galleryDelete.undo()
    fun onGalleryDeleteResult(approved: Boolean) = galleryDelete.onResult(approved)
    fun searchSelected(context: android.content.Context) = gateFrames(_state.value.selected) { output.search(context) }
    fun shareIntent(onReady: (Intent) -> Unit) = gateFrames(_state.value.selected) { output.share(onReady) }
    internal var lockNote: String? = null
    private val output = FramesOutput(viewModelScope, getApplication<Application>(), container, exporter, _state, { projectId }, { lockGate }, ::selectedFrames, ::postBusy) { lockNote = it }
    fun framesForSelection(gateLocks: Boolean = false, onReady: (List<FrameEntity>) -> Unit) = output.forSelection(gateLocks, onReady)
    fun framesForExport(gateLocks: Boolean = true, onReady: (List<FrameEntity>) -> Unit) = output.forExport(gateLocks, onReady)
    fun reassembleVideo() = output.reassembleVideo()
    internal fun gateSlides(slideProjectId: Long, run: () -> Unit) = viewModelScope.launch {
        val project = container.projects.getById(slideProjectId)
        val dropTransitions = project?.let { com.frameout.engine.extract.ExtractModeCodec.decode(it.extractMode) is com.frameout.engine.extract.ExtractMode.Slides } ?: false
        val ids = runCatching { com.frameout.core.quality.SlideDeck.uniqueIds(container.frames, slideProjectId, dropTransitions) }.logged("FramesViewModel").getOrDefault(emptyList())
        lockNote = null
        lockGate.gate(container.lockReport(ids)) { note -> lockNote = note; run() }
    }
    fun editsFlow(id: Long): Flow<Map<Long, FrameEdit>> = container.frameEdits.observeProject(id).map { list ->
        list.map { it.toEdit() }.filter { !it.isIdentity }.associateBy { it.frameId }
    }
    fun pagingFlow(): Flow<PagingData<FrameEntity>> {
        val ids = _state.value.let { if (it.smartFilter == SmartFilter.None) null else it.smartIds }
        return Pager(PagingConfig(60, prefetchDistance = 30, enablePlaceholders = ids == null)) {
            queries.paging(projectId, _state.value)
        }.flow.map { data -> if (ids == null) data else data.filter { it.id in ids } }.cachedIn(viewModelScope)
    }
    internal suspend fun refresh(id: Long, withIntegrity: Boolean = true) {
        val project = smartRun.refreshLock.withLock {
            val count = queries.count(id, _state.value)
            val found = container.projects.getById(id)
            val all = container.frames.countActive(id)
            val favorites = container.frames.countFavorites(id)
            val deleted = container.frames.countDeleted(id)
            val tableSharp = container.sharpest.count(id)
            val cacheOk = SharpestCache.isValid(getApplication<Application>(), id, container.frames.countAll(id))
            val known = _state.value.sharpestVersion > 0 || (cacheOk && tableSharp > 0)
            val sharp = if (known) tableSharp else _state.value.sharpestCount
            val stale = known && !cacheOk
            if (projectId == id) _state.update { it.copy(project = found, frameCount = count, allCount = all, favoriteCount = favorites, deletedCount = deleted, sharpestCount = sharp, sharpestStale = stale, loading = false) }
            found
        }
        if (projectId == id) tags.syncCounts()
        if (withIntegrity && project != null && projectId == id && _state.value.integrity == null) {
            val report = runCatching { IntegrityLoader.load(getApplication(), project) }.logged("FramesViewModel").getOrNull()
            _state.update { it.copy(integrity = report) }
        }
    }
    internal suspend fun contactPreviewFrames(limit: Int): List<FrameEntity> {
        val selected = selectedFrames()
        val frames = if (selected.isNotEmpty()) selected else container.frames.pageActive(projectId, limit, 0)
        return frames.sortedBy { it.ptsUs }.take(limit)
    }
    private suspend fun selectedFrames(): List<FrameEntity> {
        val ids = _state.value.selected.toList()
        return if (ids.isEmpty()) emptyList() else container.frames.getByIdsChunked(ids)
    }
    private fun mutateSelection(block: suspend (List<Long>) -> Unit) {
        val ids = _state.value.selected.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch { block(ids); _state.update { it.clearSelection() }; refresh(projectId) }
    }
    fun applySmartFilter(filter: SmartFilter) = applySmartFilterImpl(filter)
}
