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
    if (_state.value.smartFilter == filter) { _state.update { it.copy(filterOpen = false) }; return }
    withSmart {
        val pid = projectId
        if (pid < 0) return@withSmart
        cancelAnalysis()
        analysisJob = viewModelScope.launch {
            _state.update { it.analysisStarted().copy(filterOpen = false) }
            var ids: Set<Long> = emptySet()
            var text = ""
            try {
                val picked = when (filter) {
                    SmartFilter.Important -> importantSelection(container.frames, container.frameTags, pid)
                    SmartFilter.Duplicates -> duplicateSelection(container.frames, pid)
                    SmartFilter.NoPeople -> noPeopleSelection(container.frames, pid)
                    SmartFilter.None -> emptySet<Long>() to ""
                }
                ids = picked.first
                text = picked.second
            } finally {
                _state.update { if (it.analyzing) it.copy(analyzing = false) else it }
            }
            if (projectId != pid) return@launch
            val icon = when (filter) {
                SmartFilter.Important -> FrameIconsExtra.Sparkle
                SmartFilter.Duplicates -> FrameIconsExtra.Duplicate
                else -> FrameIconsExtra.PersonOff
            }
            if (ids.isEmpty()) {
                _state.update { it.copy(analyzing = false, message = text, messageIcon = icon) }
                return@launch
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
    }
}
