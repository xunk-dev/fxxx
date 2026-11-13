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

    fun cancel() {
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
        state.update { it.copy(detecting = true) }
        job = scope.launch {
            val result = try {
                withContext(Dispatchers.Default) {
                    if (persist) ViewerDetections.scanAndStore(dao, frame, bitmap) else ViewerDetections.scan(bitmap)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                com.frameout.core.util.DetectorHealth.failed("detect", e)
                ViewerDetections.Live(emptyList(), true)
            }
            finish(frame, bitmap, result)
        }
    }

    private fun finish(frame: FrameEntity, bitmap: Bitmap, result: ViewerDetections.Live) {
        state.update { cur ->
            if (cur.current?.id != frame.id || cur.bitmap !== bitmap) return@update cur.copy(detecting = false)
            when {
                result.failedAll -> cur.copy(detecting = false, showDetections = false, message = QualityStrings.detectionsFailed(), messageIcon = FrameIconsExtra.Detect)
                result.detections.isEmpty() -> cur.copy(detecting = false, detectionsReady = true, showDetections = false, detections = emptyList(), message = QualityStrings.detectionsNone(), messageIcon = FrameIconsExtra.Detect)
                else -> cur.copy(detecting = false, detectionsReady = true, detections = result.detections)
            }
        }
    }
}
