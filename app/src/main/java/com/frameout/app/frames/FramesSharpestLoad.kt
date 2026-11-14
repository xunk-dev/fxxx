package com.frameout.app.frames

import android.app.Application
import com.frameout.core.quality.FrameSharpnessAnalyzer
import com.frameout.core.util.logFailure
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

private suspend fun FramesViewModel.showQuick(pid: Long, ids: List<Long>) {
    if (projectId != pid) return
    container.sharpest.replace(pid, ids)
    _state.update { it.copy(sharpestVersion = it.sharpestVersion + 1, analyzing = false, refining = true) }
    refresh(pid)
}

internal suspend fun FramesViewModel.loadSharpest(pid: Long) {
    val app = getApplication<Application>()
    var finished = false
    try {
        val total = container.frames.countAll(pid)
        val cached = SharpestCache.isValid(app, pid, total) && (total == 0 || container.sharpest.count(pid) > 0)
        if (!cached) {
            _state.update { it.copy(analyzing = true) }
            val ids = try {
                withContext(Dispatchers.Default) {
                    FrameSharpnessAnalyzer.bestIds(
                        container.frames, pid,
                        onProgress = { done, all -> _state.update { s -> if (s.analyzing) s.copy(analyzeDone = done, analyzeTotal = all) else s } },
                        onQuick = { quick -> showQuick(pid, quick) }
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                logFailure("FramesSharpestLoad", e)
                null
            }
            if (ids == null) {
                if (!_state.value.refining) {
                    SharpestCache.clear(app, pid)
                    runCatching { container.sharpest.replace(pid, emptyList()) }
                }
            } else {
                container.sharpest.replace(pid, ids)
                if (ids.isNotEmpty() || total == 0) SharpestCache.mark(app, pid, total) else SharpestCache.clear(app, pid)
            }
        }
        finished = true
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        logFailure("FramesSharpestLoad", e)
        finished = true
    } finally {
        _state.update { if (projectId == pid && (it.analyzing || it.refining)) it.copy(analyzing = false, refining = false, analyzeDone = 0, analyzeTotal = 0) else it }
    }
    if (finished && projectId == pid) {
        _state.update { it.copy(sharpestVersion = if (it.sharpestVersion == 0) 1 else it.sharpestVersion) }
        refresh(pid)
    }
}
