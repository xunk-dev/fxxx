package com.frameout.app.frames

import android.app.Application
import android.content.Context
import android.content.Intent
import com.frameout.app.billing.LockGate
import com.frameout.app.billing.ProCheck
import com.frameout.app.billing.lockReport
import com.frameout.app.search.ImageSearchLauncher
import com.frameout.app.search.SEARCH_HANDOFF_MS
import com.frameout.core.db.FrameEntity
import com.frameout.core.pro.ProFeature
import com.frameout.core.storage.FrameExporter
import com.frameout.core.util.AppContainer
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.SearchStrings
import com.frameout.designsystem.icons.FrameIcons
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class FramesOutput(
    private val scope: CoroutineScope,
    private val app: Application,
    private val container: AppContainer,
    private val exporter: FrameExporter,
    private val state: MutableStateFlow<FramesState>,
    private val projectId: () -> Long,
    private val lockGate: () -> LockGate,
    private val selectedFrames: suspend () -> List<FrameEntity>,
    private val postBusy: (String) -> Unit
) {
    private var reassembleJob: Job? = null

    fun search(context: Context) {
        scope.launch {
            val frame = selectedFrames().firstOrNull() ?: return@launch
            val busyText = SearchStrings.opening()
            postBusy(busyText)
            val message = ImageSearchLauncher.run(context, exporter, frame)
            if (message != null) state.update { it.copy(message = message, busyMessage = null, messageIcon = null) }
            else {
                delay(SEARCH_HANDOFF_MS)
                state.update { if (it.busyMessage == busyText) it.copy(message = null, busyMessage = null) else it }
            }
        }
    }

    fun share(onReady: (Intent) -> Unit) = scope.launch {
        val frames = selectedFrames()
        if (frames.size > FrameExporter.MAX_SHARE_FRAMES) state.update { it.copy(message = FrameStrings.shareLimited(FrameExporter.MAX_SHARE_FRAMES), messageIcon = FrameIcons.Share) }
        exporter.shareIntent(frames)?.let(onReady)
    }

    fun forSelection(gateLocks: Boolean, onReady: (List<FrameEntity>) -> Unit) = scope.launch {
        val frames = selectedFrames()
        if (gateLocks) lockGate().gate(container.lockReport(frames.map { it.id })) { onReady(frames) } else onReady(frames)
    }

    fun forExport(gateLocks: Boolean, onReady: (List<FrameEntity>) -> Unit) = scope.launch {
        val frames = selectedOrAll()
        if (gateLocks) lockGate().gate(container.lockReport(frames.map { it.id })) { onReady(frames) } else onReady(frames)
    }

    fun reassembleVideo() {
        if (!ProCheck.passOrAd(ProFeature.AdvancedExport) { reassembleVideo() }) return
        if (state.value.reassembling) {
            reassembleJob?.cancel()
            return
        }
        reassembleJob = scope.launch {
            val project = state.value.project ?: return@launch
            val frames = selectedOrAll()
            if (state.value.reassembling || frames.isEmpty()) return@launch
            state.update { it.copy(reassembling = true, message = null) }
            try {
                val outcome = FramesExportAction(app).video(project, frames)
                state.update { it.copy(message = outcome.message.takeIf { outcome.saved == null }, messageIcon = null, exportSaved = outcome.saved) }
            } finally {
                state.update { it.copy(reassembling = false) }
            }
        }
    }

    private suspend fun selectedOrAll(): List<FrameEntity> {
        val selected = selectedFrames()
        if (selected.isNotEmpty()) return selected
        val out = ArrayList<FrameEntity>()
        var offset = 0
        while (true) {
            val page = container.frames.pageActive(projectId(), 500, offset)
            if (page.isEmpty()) break
            out += page
            offset += page.size
        }
        return out
    }
}
