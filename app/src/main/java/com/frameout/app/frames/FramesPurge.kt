package com.frameout.app.frames

import com.frameout.app.deletionMessage
import com.frameout.core.db.FrameEntity
import com.frameout.core.db.getByIdsChunked
import com.frameout.core.db.permanentDeleteAtomic
import com.frameout.core.util.AppContainer
import com.frameout.designsystem.icons.FrameIcons
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class FramesPurge(
    private val scope: CoroutineScope,
    private val container: AppContainer,
    private val state: MutableStateFlow<FramesState>,
    private val projectId: () -> Long,
    private val refresh: suspend (Long) -> Unit
) {
    fun deleteSelected() {
        val ids = state.value.selected.toList()
        if (ids.isEmpty()) return
        scope.launch {
            val frames = container.frames.getByIdsChunked(ids)
            val removable = withContext(Dispatchers.IO) { frames.filter { deleteFiles(it) } }
            if (removable.isNotEmpty()) container.database.permanentDeleteAtomic(removable.map { it.id })
            state.update { it.clearSelection().copy(message = deletionMessage(removable.size, ids.size - removable.size), messageIcon = FrameIcons.Trash) }
            refresh(projectId())
        }
    }

    fun emptyTrash() {
        scope.launch {
            val ids = container.frames.listDeletedIds(projectId())
            if (ids.isEmpty()) return@launch
            val frames = container.frames.getByIdsChunked(ids)
            val removable = withContext(Dispatchers.IO) { frames.filter { deleteFiles(it) } }
            if (removable.size == frames.size) {
                container.frames.emptyTrash(projectId())
            } else if (removable.isNotEmpty()) {
                container.database.permanentDeleteAtomic(removable.map { it.id })
            }
            state.update { it.copy(message = deletionMessage(removable.size, frames.size - removable.size), messageIcon = FrameIcons.Trash) }
            refresh(projectId())
        }
    }

    private fun deleteFiles(frame: FrameEntity): Boolean =
        listOfNotNull(frame.thumbnailPath, frame.fullPath, com.frameout.core.quality.AnalysisSource.copyPath(frame.thumbnailPath, frame.index)).all { path -> File(path).let { !it.exists() || it.delete() } }
}
