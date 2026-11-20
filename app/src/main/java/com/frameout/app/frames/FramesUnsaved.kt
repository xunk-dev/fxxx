package com.frameout.app.frames

import android.app.Application
import com.frameout.app.billing.LockGate
import com.frameout.app.billing.lockReport
import com.frameout.core.db.FrameEntity
import com.frameout.core.db.getByIdsChunked
import com.frameout.core.edit.UnsavedEdits
import com.frameout.core.storage.FrameExporter
import com.frameout.core.util.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class FramesUnsaved(
    private val scope: CoroutineScope,
    app: Application,
    private val container: AppContainer,
    exporter: FrameExporter,
    private val state: MutableStateFlow<FramesState>,
    private val projectId: () -> Long,
    private val lockGate: () -> LockGate
) {
    private val gallery = FramesGallerySave(scope, app, exporter, state, ::frames)

    init {
        scope.launch { UnsavedEdits.version.collect { refresh() } }
    }

    private suspend fun frames(ids: List<Long> = UnsavedEdits.snapshot()): List<FrameEntity> {
        if (ids.isEmpty()) return emptyList()
        return container.frames.getByIdsChunked(ids).filter { !it.isDeleted && it.projectId == projectId() }
    }

    fun refresh() {
        scope.launch {
            val all = frames().size
            val ask = frames(UnsavedEdits.pending()).size
            state.update { if (it.unsaved == all && it.unsavedAsk == ask) it else it.copy(unsaved = all, unsavedAsk = ask) }
        }
    }

    fun leaveThen(then: () -> Unit) {
        scope.launch {
            UnsavedEdits.acknowledge(frames(UnsavedEdits.pending()).map { it.id })
            then()
        }
    }

    fun saveThen(then: () -> Unit) {
        scope.launch {
            val frames = frames()
            if (frames.isEmpty()) { then(); return@launch }
            lockGate().gate(container.lockReport(frames.map { it.id })) { note -> gallery.confirm(note, then) }
        }
    }
}
