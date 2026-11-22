package com.frameout.app.frames

import com.frameout.core.util.QualityStrings
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex

internal data class SmartKey(val projectId: Long, val filter: SmartFilter, val frames: Int)

internal class SmartRun {
    var seq = 0
    var pending: SmartFilter? = null
    val cache = HashMap<SmartKey, Pair<Set<Long>, String>>()
    val refreshLock = Mutex()

    fun reset() {
        seq++
        pending = null
    }

    fun remember(key: SmartKey, value: Pair<Set<Long>, String>) {
        if (cache.size >= MAX_CACHED) cache.clear()
        cache[key] = value
    }

    private companion object {
        const val MAX_CACHED = 12
    }
}

internal fun FramesViewModel.cancelAnalysis() {
    analysisJob?.cancel()
    analysisJob = null
    smartRun.reset()
    _state.update { if (it.analyzing || it.refining) it.copy(analyzing = false, refining = false, analyzeDone = 0, analyzeTotal = 0, message = null, messageIcon = null) else it }
}

internal fun smartBusyText(filter: SmartFilter): String = when (filter) {
    SmartFilter.Important -> QualityStrings.analyzingImportant()
    SmartFilter.Duplicates -> QualityStrings.analyzingDuplicates()
    SmartFilter.People, SmartFilter.NoPeople -> QualityStrings.analyzingPeople()
    SmartFilter.None -> QualityStrings.analyzingFrames()
}

internal suspend fun FramesViewModel.smartSelection(filter: SmartFilter, pid: Long): Pair<Set<Long>, String> {
    val cacheable = filter == SmartFilter.Duplicates || filter == SmartFilter.People || filter == SmartFilter.NoPeople
    val key = SmartKey(pid, filter, container.frames.countActive(pid))
    if (cacheable) smartRun.cache[key]?.let { return it }
    val picked = when (filter) {
        SmartFilter.Important -> importantSelection(container.frames, container.frameTags, pid)
        SmartFilter.Duplicates -> duplicateSelection(container.frames, pid)
        SmartFilter.People -> peopleSelection(container.frames, pid)
        SmartFilter.NoPeople -> noPeopleSelection(container.frames, pid)
        SmartFilter.None -> emptySet<Long>() to ""
    }
    if (cacheable && picked.first.isNotEmpty()) smartRun.remember(key, picked)
    return picked
}
