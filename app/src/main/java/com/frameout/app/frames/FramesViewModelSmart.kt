package com.frameout.app.frames

import androidx.lifecycle.viewModelScope
import androidx.paging.filter
import com.frameout.designsystem.icons.FrameIconsExtra
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal fun FramesViewModel.withSmart(action: () -> Unit) {
    val e = container.entitlements
    val project = _state.value.project
    if (project == null && e.enforced) return
    when (SmartAccess.decide(e, project)) {
        SmartVerdict.Allowed -> action()
        SmartVerdict.NeedsReextract -> _state.update { it.copy(smartReextract = true) }
        SmartVerdict.SpendTrial -> viewModelScope.launch {
            val p = project ?: return@launch
            if (com.frameout.core.pro.ProAccess.consumeSkipTrial()) {
                container.projects.setGateFlags(p.id, com.frameout.engine.extract.GateFlags.with(p.gateFlags, granted = true, trial = true))
                refresh(p.id, false)
                action()
            } else e.showPaywall(com.frameout.core.pro.ProFeature.SmartExtract, adEligible = false)
        }
        SmartVerdict.NeedsGate -> e.gateSkip { withSmart(action) }
    }
}

internal fun FramesViewModel.applySmartFilterImpl(filter: SmartFilter) {
    if (filter == SmartFilter.None) { clearSmartFilter(); return }
    if (_state.value.smartFilter == filter || smartRun.pending == filter) { _state.update { it.copy(filterOpen = false) }; return }
    withSmart {
        val pid = projectId
        if (pid < 0) return@withSmart
        cancelAnalysis()
        val token = smartRun.seq
        smartRun.pending = filter
        analysisJob = viewModelScope.launch {
            _state.update { it.analysisStarted(smartBusyText(filter)).copy(filterOpen = false) }
            var picked: Pair<Set<Long>, String> = emptySet<Long>() to ""
            try {
                picked = smartSelection(filter, pid)
            } finally {
                if (smartRun.seq == token) {
                    smartRun.pending = null
                    _state.update { if (it.analyzing) it.copy(analyzing = false) else it }
                }
            }
            if (projectId != pid) return@launch
            showSmartResult(filter, pid, picked.first, picked.second)
        }
    }
}

private suspend fun FramesViewModel.showSmartResult(filter: SmartFilter, pid: Long, ids: Set<Long>, text: String) {
    val icon = when (filter) {
        SmartFilter.Important -> FrameIconsExtra.Sparkle
        SmartFilter.Duplicates -> FrameIconsExtra.Duplicate
        SmartFilter.People -> FrameIconsExtra.People
        else -> FrameIconsExtra.PersonOff
    }
    if (ids.isEmpty()) {
        _state.update { it.copy(analyzing = false, message = text, messageIcon = icon) }
        return
    }
    val wasSharpest = _state.value.filter == FrameFilter.Sharpest
    _state.update {
        it.copy(
            analyzing = false, smartFilter = filter, smartIds = ids, selected = emptySet(), loading = true,
            filter = if (wasSharpest) FrameFilter.All else it.filter,
            message = com.frameout.core.util.QualityStrings.filterMatched(ids.size), messageIcon = icon
        )
    }
    refresh(pid)
}
