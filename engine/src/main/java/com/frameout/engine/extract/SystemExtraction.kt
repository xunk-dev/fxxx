package com.frameout.engine.extract

import android.content.Context
import android.net.Uri
import com.frameout.core.db.FrameDao
import com.frameout.core.db.ProjectDao
import com.frameout.core.db.VideoIndexDao
import com.frameout.core.model.ProjectStatus
import com.frameout.core.storage.StorageManager
import kotlinx.coroutines.flow.Flow
import com.frameout.core.util.DiagLog
import com.frameout.engine.index.IndexReuse
import com.frameout.engine.index.MediaIndexer
import com.frameout.engine.index.ScanWindow
import com.frameout.engine.index.SampleInfo
import com.frameout.engine.index.VideoIndexMetadata

class SystemExtraction(
    private val context: Context,
    private val projectDao: ProjectDao,
    private val frameDao: FrameDao,
    private val storage: StorageManager,
    private val videoIndexDao: VideoIndexDao,
    private val checkpointStore: CheckpointStore,
    private val cancelled: () -> Boolean,
    private val paused: () -> Boolean,
    private val progress: (Int, Int, Long) -> Unit,
    private val indexingProgress: (Int, Int) -> Unit
) {
    private val indexer = MediaIndexer(context)

    private companion object {
        const val INDEX_REPORT_MS = 250L
    }

    suspend fun run(request: ExtractRequest, uri: Uri) {
        val initial = if (paused()) ProjectStatus.Paused else ProjectStatus.Indexing
        projectDao.setStatus(request.projectId, initial.name, System.currentTimeMillis())
        if (runSparse(request, uri)) return
        val index = buildIndex(request, uri)
        val project = projectDao.getById(request.projectId)
        val existing = frameDao.countAll(request.projectId)
        val checkpoint = if (existing > 0) project?.lastPtsUs ?: 0L else -1L
        val checkpointIndex = if (existing > 0) project?.lastSampleIndex ?: -1 else -1
        val source = IndexSampleSource(videoIndexDao, request.projectId, index.durationUs)
        val remaining = source.selectedCount(request, checkpoint, checkpointIndex)
        if (remaining <= 0) {
            finishWithoutWork(request, existing, checkpoint)
            return
        }
        if (!storage.hasSpaceForResolution(SpaceEstimate.frames(request.mode, remaining), index.width, index.height, request.autoSave, request.saveFormat)) {
            throw LowSpaceException()
        }
        val flow = source.sampleFlow(request, checkpoint, checkpointIndex)
        extractSamples(request, uri, flow, existing + remaining, existing, checkpoint)
    }

    private suspend fun runSparse(request: ExtractRequest, uri: Uri): Boolean {
        if (!SparseGate.supports(request.mode)) return false
        val existing = frameDao.countAll(request.projectId)
        if (videoIndexDao.count(request.projectId) > 0) return false
        val began = System.currentTimeMillis()
        val probe = indexer.probe(uri) ?: return false
        val meta = probe.meta
        val startUs = request.startMs.coerceAtLeast(0L) * 1000L
        val endUs = if (request.endMs > 0L) request.endMs * 1000L else meta.durationUs.coerceAtLeast(startUs + 1L)
        val plan = SparsePlan.plan(request.mode, startUs, maxOf(endUs, startUs + 1L), meta.durationUs, probe.firstPtsUs, meta.estimatedFrames) ?: return false
        val project = projectDao.getById(request.projectId)
        val checkpoint = if (existing > 0) project?.lastPtsUs ?: 0L else -1L
        val checkpointIndex = if (existing > 0) project?.lastSampleIndex ?: -1 else -1
        val remaining = plan.count - (checkpointIndex + 1).coerceAtLeast(0)
        DiagLog.append("extract", "sparse plan ${encodeMode(request.mode)}: ${plan.count} targets, probe ${System.currentTimeMillis() - began}ms, no full index")
        indexingProgress(plan.count, plan.count)
        updateIndex(request, meta)
        if (remaining <= 0) {
            finishWithoutWork(request, existing, checkpoint)
            return true
        }
        if (!storage.hasSpaceForResolution(SpaceEstimate.frames(request.mode, remaining), meta.width, meta.height, request.autoSave, request.saveFormat)) {
            throw LowSpaceException()
        }
        extractSamples(request, uri, plan.flow(checkpointIndex), existing + remaining, existing, checkpoint)
        return true
    }

    private suspend fun reusableIndex(request: ExtractRequest, uri: Uri): VideoIndexMetadata? {
        val dir = storage.projectDir(request.projectId)
        val stored = videoIndexDao.count(request.projectId)
        val marked = IndexReuse.readMarker(dir)
        if (stored <= 0 || marked != stored) return null
        val meta = indexer.storedMetadata(uri, stored) ?: return null
        val durationMs = projectDao.getById(request.projectId)?.durationMs ?: 0L
        if (!IndexReuse.canReuse(marked, stored, durationMs, meta.durationUs)) return null
        val requested = ScanWindow.of(request.startMs, request.endMs, meta.durationUs)
        return meta.takeIf { IndexReuse.coversRequest(IndexReuse.readWindow(dir), requested) }
    }

    private suspend fun buildIndex(request: ExtractRequest, uri: Uri): VideoIndexMetadata {
        val began = System.currentTimeMillis()
        reusableIndex(request, uri)?.let { reused ->
            DiagLog.append("extract", "index reused: ${reused.frameCount} samples, check ${System.currentTimeMillis() - began}ms")
            indexingProgress(reused.frameCount, reused.frameCount)
            updateIndex(request, reused)
            return reused
        }
        var lastReport = 0L
        val index = indexer.indexToDatabase(uri, request.projectId, videoIndexDao, cancelled, paused, request.startMs, request.endMs) { done, estimate ->
            val now = System.currentTimeMillis()
            if (done == 1 || now - lastReport >= INDEX_REPORT_MS) {
                indexingProgress(done, estimate)
                lastReport = now
            }
        }
        val dir = storage.projectDir(request.projectId)
        IndexReuse.writeMarker(dir, index.frameCount)
        IndexReuse.writeWindow(dir, index.window)
        val scope = index.window?.let { "window ${it.startUs / 1000}-${it.endUs / 1000}ms of ${index.durationUs / 1000}ms" } ?: "whole track"
        DiagLog.append("extract", "index built: ${index.frameCount} samples ($scope) in ${System.currentTimeMillis() - began}ms")
        indexingProgress(index.frameCount, index.frameCount)
        updateIndex(request, index)
        return index
    }

    private suspend fun finishWithoutWork(request: ExtractRequest, existing: Int, checkpoint: Long) {
        val status = if (paused()) ProjectStatus.Paused else ProjectStatus.Completed
        projectDao.updateProgress(
            request.projectId,
            status.name,
            existing,
            checkpoint.coerceAtLeast(0L),
            System.currentTimeMillis()
        )
        progress(existing, existing, checkpoint)
    }

    private suspend fun extractSamples(
        request: ExtractRequest,
        uri: Uri,
        flow: Flow<SampleInfo>,
        total: Int,
        existing: Int,
        checkpoint: Long
    ) {
        val startStatus = if (paused()) ProjectStatus.Paused else ProjectStatus.Extracting
        projectDao.updateProgress(request.projectId, startStatus.name, existing, checkpoint, System.currentTimeMillis())
        val runner = ExtractRunner(context, projectDao, frameDao, storage, checkpointStore, cancelled, paused, progress)
        runner.run(request, uri, flow, total, checkpoint)
    }

    private suspend fun updateIndex(request: ExtractRequest, index: VideoIndexMetadata) {
        projectDao.updateIndexMeta(
            request.projectId,
            index.durationUs / 1000L,
            index.frameCount,
            encodeMode(request.mode),
            request.autoSave,
            request.saveFormat,
            request.jpegQuality,
            request.startMs,
            request.endMs,
            System.currentTimeMillis()
        )
    }

    private fun encodeMode(mode: ExtractMode): String = when (mode) {
        ExtractMode.EveryFrame -> "EveryFrame"
        is ExtractMode.EveryNFrames -> "EveryNFrames:${mode.n}"
        is ExtractMode.EveryNMillis -> "EveryNMillis:${mode.millis}"
        ExtractMode.KeyframesOnly -> "KeyframesOnly"
        is ExtractMode.FixedCount -> "FixedCount:${mode.count}"
        ExtractMode.SceneChange -> "SceneChange"
        ExtractMode.Slides -> "Slides"
        is ExtractMode.Content -> "Content:${mode.spec.encode()}"
    }
}

internal object SparseGate {
    fun supports(mode: ExtractMode): Boolean =
        mode is ExtractMode.FixedCount || mode is ExtractMode.EveryNMillis || mode is ExtractMode.Content
}
