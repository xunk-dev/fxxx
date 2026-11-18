package com.frameout.app.viewer

import android.graphics.Bitmap
import com.frameout.core.db.FrameEntity
import com.frameout.core.db.FrameTagDao
import com.frameout.core.util.QualityStrings
import com.frameout.designsystem.icons.FrameIconsExtra
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class ViewerDetect(
    private val scope: CoroutineScope,
    private val state: MutableStateFlow<ViewerState>,
    private val dao: FrameTagDao,
    private val stopPlay: () -> Unit
) {
    private var job: Job? = null
    private var watchdog: Job? = null

    private var token = 0L

    fun cancel() {
        token++
        watchdog?.cancel()
        watchdog = null
        job?.cancel()
        job = null
        if (state.value.detecting) state.update { it.copy(detecting = false) }
    }

    fun toggle() {
        val s = state.value
        if (s.showDetections) {
            cancel()
            state.update { it.copy(showDetections = false) }
            return
        }
        if (!com.frameout.app.billing.ProCheck.passOrAd(com.frameout.core.pro.ProFeature.Insights) { toggle() }) return
        stopPlay()
        state.update { it.copy(showDetections = true) }
        if (!s.detectionsReady) run()
    }

    fun run() {
        val s = state.value
        if (s.detecting) return
        val bitmap = s.bitmap?.takeUnless { it.isRecycled } ?: return
        val frame = s.current ?: return
        val persist = !s.savedMode && !s.stillMode && s.edit.isIdentity
        job?.cancel()
        watchdog?.cancel()
        val mine = ++token
        state.update { it.copy(detecting = true) }
        watchdog = armWatchdog(mine)
        job = scope.launch {
            try {
                val result = scanOrFailure(frame, bitmap, persist)
                if (token == mine) finish(frame, bitmap, result)
            } finally {
                if (token == mine) {
                    watchdog?.cancel()
                    state.update { if (it.detecting) it.copy(detecting = false) else it }
                }
            }
        }
    }

    private fun armWatchdog(mine: Long): Job = scope.launch {
        delay(HARD_LIMIT_MS)
        if (token != mine) return@launch
        token++
        job?.cancel()
        state.update { cur ->
            if (cur.detecting) cur.copy(detecting = false, showDetections = false, message = QualityStrings.detectionsFailed(), messageIcon = FrameIconsExtra.Detect) else cur
        }
    }

    private suspend fun scanOrFailure(frame: FrameEntity, bitmap: Bitmap, persist: Boolean): ViewerDetections.Live = try {
        withContext(Dispatchers.Default) {
            if (persist) ViewerDetections.scanAndStore(dao, frame, bitmap) else ViewerDetections.scan(bitmap)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        com.frameout.core.util.DetectorHealth.failed("detect", e)
        ViewerDetections.Live(emptyList(), true)
    }

    private fun finish(frame: FrameEntity, bitmap: Bitmap, result: ViewerDetections.Live) {
        state.update { cur ->
            if (cur.current?.id != frame.id || cur.bitmap !== bitmap) return@update cur.copy(detecting = false)
            when {
                result.failedAll -> cur.copy(detecting = false, showDetections = false, message = QualityStrings.detectionsFailed(), messageIcon = FrameIconsExtra.Detect)
                result.detections.isEmpty() && result.partial -> cur.copy(detecting = false, showDetections = false, detections = emptyList(), message = QualityStrings.detectionsFailed(), messageIcon = FrameIconsExtra.Detect)
                result.detections.isEmpty() -> cur.copy(detecting = false, detectionsReady = true, showDetections = false, detections = emptyList(), message = QualityStrings.detectionsNone(), messageIcon = FrameIconsExtra.Detect)
                else -> cur.copy(detecting = false, detectionsReady = !result.partial, detections = result.detections)
            }
        }
    }

    private companion object {
        const val HARD_LIMIT_MS = 45_000L
    }
}
