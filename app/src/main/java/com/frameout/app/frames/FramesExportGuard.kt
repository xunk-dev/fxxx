package com.frameout.app.frames

import com.frameout.core.util.ClipStrings
import kotlinx.coroutines.Job

internal class FramesExportGuard(
    private val running: () -> Boolean,
    private val postBusy: (String) -> Unit
) {
    private var job: Job? = null
    private var armed = false

    fun started(job: Job?) {
        this.job = job
        armed = false
    }

    fun ended() {
        job = null
        armed = false
    }

    fun holdBack(): Boolean {
        if (!running()) return false
        if (armed) {
            job?.cancel()
            return false
        }
        armed = true
        postBusy(ClipStrings.saveWait())
        return true
    }
}
