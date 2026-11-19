package com.frameout.app.trim

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.frameout.core.prefs.AppPrefs
import com.frameout.core.util.ClipStrings
import com.frameout.core.util.ExtractFabStrings
import com.frameout.core.util.FrameStrings
import com.frameout.engine.timeline.TrimRange
import kotlinx.coroutines.Job

internal class TrimUiState(autoSave: Boolean, output: TrimOutput = TrimOutput.Frames) {
    var range by mutableStateOf(TrimRange())
    var output by mutableStateOf(output)
    var sheet by mutableStateOf(TrimSheet.None)
    var status by mutableStateOf<CaptureStatusState?>(null)
    var busy by mutableStateOf(false)
    var autoSave by mutableStateOf(autoSave)
    var saveJob: Job? = null
    private var opened: TrimSnapshot? = null
    private var stash: TrimRange? = null
    private var leaveArmed = false
    private var lastPercent = -1

    fun showFull(durationMs: Long) {
        range = TrimLimits.fit(TrimRange.full(durationMs), output.maxMs)
    }

    fun selectOutput(next: TrimOutput, positionMs: Long = 0L) {
        output = next
        val switched = TrimLimits.switchTo(range, stash, next.maxMs, positionMs)
        range = switched.range
        stash = switched.stash
    }

    fun change(next: TrimRange) {
        stash = null
        range = TrimLimits.enforce(range, next, output.maxMs)
    }

    fun markOpened() {
        if (opened == null && range.endMs > 0L) opened = TrimSnapshot(range, output)
    }

    fun markSaved() {
        if (range.endMs > 0L) opened = TrimSnapshot(range, output)
    }

    val dirty: Boolean get() = TrimDirty.changed(opened, TrimSnapshot(range, output))

    fun toggleAutoSave(prefs: AppPrefs) {
        autoSave = !autoSave
        prefs.setAutoSave(autoSave)
    }

    fun holdBack(): Boolean {
        if (!busy) return false
        if (leaveArmed) {
            saveJob?.cancel()
            saveEnded()
            status = null
            return false
        }
        leaveArmed = true
        showProgress()
        return true
    }

    fun progress(percent: Int) {
        if (!busy || percent <= lastPercent) return
        lastPercent = percent
        showProgress()
    }

    private fun showProgress() {
        val head = if (leaveArmed) ClipStrings.saveWait() else FrameStrings.saving()
        status = CaptureStatusState(if (lastPercent < 0) head else "$head ${ExtractFabStrings.percent(lastPercent)}", busy = true)
    }

    fun saveEnded(job: Job? = null) {
        if (job != null && job !== saveJob) return
        busy = false
        lastPercent = -1
        leaveArmed = false
        saveJob = null
    }
}
