package com.frameout.app.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.frameout.app.LocalTabActive
import com.frameout.app.library.FabModel
import com.frameout.app.library.FabPhase
import com.frameout.app.library.LibraryViewModel
import com.frameout.app.library.fabModel
import com.frameout.app.library.toFab
import com.frameout.core.db.ProjectEntity
import com.frameout.engine.work.LiveExtractProgress
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan

internal data class StripModel(val projectId: Long, val name: String, val fab: FabModel, val status: String)

private data class StripSnapshot(
    val projects: List<ProjectEntity> = emptyList(),
    val focus: Long? = null,
    val flash: Boolean = false,
    val last: Long? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
private fun stripFlow(vm: LibraryViewModel): Flow<StripModel?> =
    vm.state
        .map { Triple(it.projects, it.focusId, it.flashSuccess) }
        .distinctUntilChanged()
        .scan(StripSnapshot()) { acc, (projects, focus, flash) ->
            StripSnapshot(projects, focus, flash, last = if (flash) acc.last else (focus ?: acc.last))
        }
        .flatMapLatest { snap ->
            val id = if (snap.flash) snap.last else snap.focus
            LiveExtractProgress.observe(id ?: -1L).map { live -> buildModel(snap, id, live) }
        }
        .distinctUntilChanged()

private fun buildModel(snap: StripSnapshot, id: Long?, live: Float?): StripModel? {
    if (id == null) return null
    val project = snap.projects.firstOrNull { it.id == id } ?: return null
    val fab = fabModel(snap.projects.map { it.toFab() }, id, snap.flash, if (live != null) mapOf(id to live) else emptyMap())
    if (fab.phase == FabPhase.Add) return null
    return StripModel(id, project.name, fab, project.status)
}

@Composable
internal fun rememberExtractStrip(): State<StripModel?> {
    val vm: LibraryViewModel = viewModel()
    val active = LocalTabActive.current
    val flow = remember(vm) { stripFlow(vm) }
    val shown = remember(vm) { mutableStateOf<StripModel?>(null) }
    LaunchedEffect(flow, active) {
        if (active) flow.collect { shown.value = it }
    }
    return shown
}
