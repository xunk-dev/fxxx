package com.frameout.app.viewer

import com.frameout.app.billing.LockGate
import com.frameout.app.billing.lockReport
import com.frameout.core.db.FrameEntity
import com.frameout.core.db.getByIdsChunked
import com.frameout.core.edit.UnsavedEdits
import com.frameout.core.util.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class ViewerLeave(
    private val scope: CoroutineScope,
    private val state: MutableStateFlow<ViewerState>,
    private val c: AppContainer,
    private val lockGate: LockGate,
    private val export: ViewerExport,
    private val replaceSavedId: () -> Long?,
    private val replaceFrameId: () -> Long?
) {
    private var leaveAction: (() -> Unit)? = null
    private var sessionStart = 0L
    private var promptIds: List<Long> = emptyList()

    private var sessionKey: List<Long?>? = null

    fun begin(projectId: Long, frameId: Long, savedId: Long?) {
        val key = listOf(projectId, frameId, savedId)
        if (key == sessionKey) return
        sessionKey = key
        sessionStart = UnsavedEdits.now()
        promptIds = emptyList()
    }

    private fun endSession() {
        sessionKey = null
    }

    private suspend fun unsavedFrames(): List<FrameEntity> {
        val s = state.value
        val current = s.current ?: return emptyList()
        val ids = UnsavedEdits.pending(sessionStart)
        if (ids.isEmpty()) return emptyList()
        val frames = c.frames.getByIdsChunked(ids).filter { !it.isDeleted }
        return if (s.savedMode) frames.filter { it.id == current.id } else frames.filter { it.projectId == current.projectId }
    }

    suspend fun refreshUnsaved() {
        val count = unsavedFrames().size
        state.update { if (it.unsaved == count) it else it.copy(unsaved = count) }
    }

    fun request(onLeave: () -> Unit) {
        leaveAction = onLeave
        scope.launch {
            val frames = unsavedFrames()
            promptIds = frames.map { it.id }
            if (frames.isEmpty()) { endSession(); onLeave() } else state.update { it.copy(leavePrompt = true) }
        }
    }

    fun stay() {
        leaveAction = null
        promptIds = emptyList()
        state.update { it.copy(leavePrompt = false) }
    }

    fun discard() {
        val action = leaveAction
        UnsavedEdits.acknowledge(promptIds)
        stay()
        endSession()
        action?.invoke()
    }

    fun saveAndLeave() {
        val action = leaveAction ?: return
        state.update { it.copy(leavePrompt = false) }
        scope.launch {
            val frames = unsavedFrames()
            if (frames.isEmpty()) { endSession(); action(); return@launch }
            lockGate.gate(c.lockReport(frames.map { it.id })) { note ->
                val then: (Boolean) -> Unit = { ok -> if (ok) { leaveAction = null; endSession(); action() } }
                val only = frames.singleOrNull()
                val savedId = replaceSavedId()
                when {
                    only != null && savedId != null && only.id == replaceFrameId() -> export.replaceSaved(savedId, only, note, then)
                    only != null -> export.save(only, note, then)
                    else -> export.saveMany(frames, note, then)
                }
            }
        }
    }
}
