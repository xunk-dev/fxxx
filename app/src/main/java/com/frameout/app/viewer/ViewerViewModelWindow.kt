package com.frameout.app.viewer

import android.graphics.Bitmap
import androidx.lifecycle.viewModelScope
import com.frameout.core.db.FrameEntity
import com.frameout.core.edit.FrameEdit
import com.frameout.core.util.EditedThumb
import com.frameout.core.util.ThumbCache
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

internal fun ViewerViewModel.neighborThumb(before: ViewerState, frame: FrameEntity): Bitmap? = when (frame.id) {
    before.next?.id -> before.nextBitmap
    before.previous?.id -> before.previousBitmap
    else -> null
}?.takeUnless { it.isRecycled }

internal suspend fun ViewerViewModel.placeholderFor(frame: FrameEntity): Bitmap? {
    val path = frame.thumbnailPath ?: frame.fullPath
    return ThumbCache.peek(path) ?: withContext(Dispatchers.IO) { ThumbCache.getShared(path) }
}

internal fun ViewerViewModel.upgradePlaceholder(frame: FrameEntity, edit: FrameEdit, generation: Long) {
    viewModelScope.launch {
        val sharp = EditedThumb.loadSharp(frame.fullPath, edit, screenPx) ?: return@launch
        if (generation != loadGeneration) return@launch
        _state.update { if (it.current?.id == frame.id && it.bitmap == null) it.copy(placeholder = sharp, placeholderSharp = true) else it }
    }
}

internal suspend fun ViewerViewModel.awaitPrefetch(id: Long, edit: FrameEdit): Bitmap? {
    inFlight[id]?.join()
    return cache.take(id, edit)
}

internal fun ViewerViewModel.keepOrRecycle(frame: FrameEntity?, edit: FrameEdit, bitmap: Bitmap?, now: FrameEntity) {
    bitmap ?: return
    if (bitmap.isRecycled) return
    if (frame == null || frame.id == now.id) bitmap.recycle() else cache.put(frame.id, edit, bitmap)
}

internal fun ViewerViewModel.launchWindow(frame: FrameEntity, full: Boolean = true) {
    windowJob?.cancel()
    val dir = direction
    windowJob = viewModelScope.launch {
        val (ahead, behind) = withContext(Dispatchers.IO) {
            val forward = c.frames.nextActiveList(frame.projectId, frame.index, if (dir > 0) WARM_AHEAD else WARM_BEHIND)
            val backward = c.frames.previousActiveList(frame.projectId, frame.index, if (dir > 0) WARM_BEHIND else WARM_AHEAD)
            if (dir > 0) forward to backward else backward to forward
        }
        val fullList = (listOfNotNull(ahead.getOrNull(0), behind.getOrNull(0)) + ahead.drop(1).take(FULL_AHEAD - 1)).distinctBy { it.id }
        val order = (ahead.take(1) + behind.take(1) + ahead.drop(1) + behind.drop(1)).distinctBy { it.id }
        if (full) {
            val fullIds = fullList.map { it.id }
            cache.setWindow(fullIds)
            inFlight.filterKeys { it !in fullIds }.values.forEach { it.cancel() }
        }
        coroutineScope {
            order.take(2).map { async { warmSharp(it) } }.awaitAll()
            if (full) fullList.forEach { startPrefetch(it) }
            order.drop(2).map { async { warmSharp(it) } }.awaitAll()
        }
    }
}

internal suspend fun ViewerViewModel.warmSharp(frame: FrameEntity) {
    val edit = editHistory.current(frame.id)?.let(::fromEntity)
    EditedThumb.loadSharp(frame.fullPath, edit, screenPx)
}

internal fun ViewerViewModel.startPrefetch(frame: FrameEntity) {
    if (cache.contains(frame.id) || inFlight.containsKey(frame.id)) return
    val job = viewModelScope.launch(start = CoroutineStart.LAZY) {
        val self = coroutineContext.job
        try {
            val edit = editHistory.current(frame.id)?.let(::fromEntity) ?: FrameEdit(frame.id)
            val bmp = decodeGate.withPermit { withContext(Dispatchers.IO) { loader.load(frame, sourceUri, edit) } } ?: return@launch
            if (cache.wants(frame.id)) cache.put(frame.id, edit, bmp) else bmp.recycle()
        } finally {
            if (inFlight[frame.id] === self) inFlight.remove(frame.id)
        }
    }
    inFlight[frame.id] = job
    job.start()
}
