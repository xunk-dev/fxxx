package com.frameout.app.frames

import com.frameout.core.db.AlbumCover
import com.frameout.core.db.TagCount
import com.frameout.core.quality.FocusRegion
import com.frameout.core.quality.FrameTagScanner
import com.frameout.core.quality.CoverLanes
import com.frameout.core.quality.FrameTagger
import com.frameout.core.quality.TagFamilies
import com.frameout.core.quality.PrefsTagVersion
import com.frameout.core.quality.ScanTiming
import com.frameout.core.quality.TagVersion
import com.frameout.core.quality.ScanProgress
import com.frameout.core.quality.ScanReport
import com.frameout.core.quality.ScanVerdict
import com.frameout.core.quality.NormBox
import com.frameout.core.quality.NormBoxCodec
import com.frameout.core.util.AppContainer
import com.frameout.core.util.QualityStrings
import com.frameout.core.util.readOrKeep
import com.frameout.designsystem.icons.FrameIconsExtra
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val UI_INTERVAL_MS = 1500L

internal class FramesTags(
    private val scope: CoroutineScope,
    private val container: AppContainer,
    private val state: MutableStateFlow<FramesState>,
    private val projectId: () -> Long,
    private val refresh: suspend (Long) -> Unit
) {
    private var scanJob: Job? = null
    private var watchJob: Job? = null
    private var watchedId = -1L
    private var manualScan = false
    private var scanSeq = 0
    private var rescanPending = false
    private var lastUiMs = 0L

    fun open() {
        if (state.value.filter == FrameFilter.Tagged) {
            state.update { it.copy(tagsOpen = true) }
            scope.launch { refreshRefine() }
        } else {
            showAlbums()
        }
    }

    fun showAlbums() {
        state.update { it.copy(filter = FrameFilter.Albums, tagFilter = emptySet(), smartFilter = SmartFilter.None, smartIds = emptySet(), refineCounts = emptyList(), tagsOpen = false, selected = emptySet(), loading = true) }
        scope.launch { refresh(projectId()); loadCountsWithBackfill() }
        watchOrStart()
    }

    suspend fun syncCounts() = refreshCounts()

    fun loadCounts() {
        scope.launch { refreshCounts() }
    }

    fun ensureScan() {
        val id = projectId()
        if (id < 0) return
        scope.launch { loadCountsWithBackfill() }
        watchOrStart()
    }

    private fun watchOrStart() {
        val id = projectId()
        if (id >= 0 && !watch(id)) startScan()
    }

    private fun watch(id: Long): Boolean {
        if (watchedId == id && watchJob?.isActive == true) return false
        watchJob?.cancel()
        watchedId = id
        watchJob = scope.launch {
            container.projects.observeById(id).map { it?.status }.distinctUntilChanged().collect { onStatus(it) }
        }
        return true
    }

    private fun onStatus(status: String?) {
        if (status != null) state.update { s -> s.project?.takeIf { it.status != status }?.let { s.copy(project = it.copy(status = status)) } ?: s }
        if (ScanTiming.mayAutoStart(status)) {
            startScan()
        } else if (ScanTiming.shouldHalt(status, scanJob?.isActive == true, manualScan)) {
            scanJob?.cancel()
        }
    }

    fun stop() {
        scanSeq++
        scanJob?.cancel()
        scanJob = null
        watchJob?.cancel()
        watchJob = null
        watchedId = -1L
        manualScan = false
    }

    fun close() = state.update { it.copy(tagsOpen = false) }

    fun toggle(tag: String) {
        val next = state.value.tagFilter.toMutableSet()
        if (!next.add(tag)) next.remove(tag)
        applyFilter(next)
    }

    fun clear() = applyFilter(emptySet())

    fun rescan() {
        if (projectId() < 0 || state.value.scanning || rescanPending || scanJob?.isActive == true) return
        if (ScanTiming.waitsForExtraction(state.value.project?.status)) {
            state.update { it.copy(message = QualityStrings.contentWaitExtraction(), messageIcon = FrameIconsExtra.Tag) }
            return
        }
        rescanPending = true
        scope.launch {
            try {
                FrameTagScanner.reset(container.frameTags, projectId())
                state.update { it.copy(tagCounts = emptyList()) }
                startScan(manual = true)
            } finally {
                rescanPending = false
            }
        }
    }

    private fun applyFilter(next: Set<String>) {
        val leaving = next.isEmpty()
        state.update { it.copy(tagFilter = next, smartFilter = SmartFilter.None, smartIds = emptySet(), filter = if (leaving) FrameFilter.Albums else FrameFilter.Tagged, tagsOpen = it.tagsOpen && !leaving, selected = emptySet(), loading = true, refineCounts = if (leaving) emptyList() else it.refineCounts) }
        scope.launch { refresh(projectId()); refreshRefine() }
    }

    private fun startScan(manual: Boolean = false) {
        val id = projectId()
        if (id < 0 || scanJob?.isActive == true) return
        if (!manual && !FrameTagScanner.autoScanAllowed()) {
            announceBlocked()
            return
        }
        manualScan = manual
        val token = ++scanSeq
        scanJob = scope.launch {
            val project = container.projects.getById(id)
            if (!SmartAccess.allowedSilently(container.entitlements, project)) return@launch
            if (!manual && !ScanTiming.mayAutoStart(project?.status)) return@launch
            runScan(id, token)
        }
    }

    private suspend fun runScan(id: Long, token: Int) {
        TagVersion.ensure(PrefsTagVersion(container.prefs), FrameTagger.VERSION) { container.frameTags.clearAllScans() }
        currentCoroutineContext().ensureActive()
        if (scanSeq != token) return
        lastUiMs = 0L
        state.update { it.copy(scanning = true, scanDone = 0, scanTotal = 0, scanFailed = 0) }
        try {
            val report = FrameTagScanner.scan(container.frameTags, id) { p -> onScanProgress(id, token, p) }
            if (scanSeq == token) reportMessage(report)?.let { text -> state.update { it.copy(message = text, messageIcon = FrameIconsExtra.Tag) } }
        } finally {
            withContext(NonCancellable) {
                release(token)
                refreshCounts()
            }
        }
    }

    private suspend fun onScanProgress(id: Long, token: Int, p: ScanProgress) {
        if (scanSeq != token) return
        val now = System.nanoTime() / 1_000_000L
        if (p.done + p.failed < p.total && now - lastUiMs < UI_INTERVAL_MS) return
        lastUiMs = now
        state.update { it.copy(scanDone = p.done, scanTotal = p.total, scanFailed = p.failed) }
        refreshCounts()
        if (state.value.filter == FrameFilter.Tagged) {
            refresh(id)
            refreshRefine()
        }
    }

    private fun release(token: Int) = state.update {
        if (scanSeq == token || scanJob?.isActive != true) it.copy(scanning = false) else it
    }

    private fun announceBlocked() = state.update {
        if (it.tagCounts.isEmpty() && it.message == null) it.copy(message = QualityStrings.contentScanStopped(FrameTagScanner.blockedDetector()), messageIcon = FrameIconsExtra.Tag) else it
    }

    private fun reportMessage(report: ScanReport): String? {
        val detector = report.worstDetector ?: "-"
        return when (report.verdict) {
            ScanVerdict.Complete -> null
            ScanVerdict.Failed -> QualityStrings.contentScanAllFailed(report.detectors.keys.sorted().joinToString())
            ScanVerdict.Stopped -> QualityStrings.contentScanStopped(detector)
            ScanVerdict.Partial -> if (report.unavailable.isNotEmpty()) QualityStrings.contentScanPartial(report.partial, detector) else QualityStrings.contentScanFailed(report.incomplete)
        }
    }

    private suspend fun backfill(): Int = readOrKeep(0) { TagFamilies.backfill(container.frameTags, projectId()) }

    private suspend fun loadCountsWithBackfill() {
        refreshCounts()
        if (backfill() > 0) refreshCounts()
    }

    private suspend fun refreshRefine() {
        val tags = state.value.tagFilter.toList()
        val id = projectId()
        if (state.value.filter != FrameFilter.Tagged || tags.isEmpty()) return
        val counts = runCatching { container.frameTags.coTagCounts(id, tags, tags.size) }.getOrDefault(emptyList()).filter { TagFamilies.visible(it.tag) }
        state.update { if (it.filter == FrameFilter.Tagged && it.tagFilter == tags.toSet() && projectId() == id) it.copy(refineCounts = counts) else it }
    }

    suspend fun cover(tag: String): AlbumCover? = runCatching {
        val id = projectId()
        val row = container.frameTags.coverTagFor(id, tag)
        val boxed = row?.let { r -> container.frames.getActiveById(r.frameId)?.let { it to r } }
        val top = boxed?.first ?: container.frameTags.coverFor(id, tag) ?: return@runCatching null
        val focus = boxed?.let { FocusRegion.largest(NormBoxCodec.decode(it.second.boxes)) }
        val pool = container.frameTags.coversFor(id, tag, CoverLanes.POOL)
        val lanes = CoverLanes.build(tag, pool, container.frameTags.labelsForFrames(pool.map { it.id }))
        AlbumCover(top, focus, emptyList(), lanes)
    }.getOrNull()

    suspend fun focus(frameId: Long, tags: Set<String>): NormBox? {
        if (tags.isEmpty()) return null
        return runCatching {
            val rows = container.frameTags.boxesFor(frameId, tags.toList())
            FocusRegion.union(rows.mapNotNull { FocusRegion.largest(NormBoxCodec.decode(it.boxes)) })
        }.getOrNull()
    }

    private suspend fun refreshCounts() {
        val id = projectId()
        val counts = readOrKeep<List<TagCount>?>(null) { container.frameTags.tagCounts(id).filter { TagFamilies.visible(it.tag) } } ?: return
        state.update { if (projectId() == id) it.copy(tagCounts = counts) else it }
    }
}
