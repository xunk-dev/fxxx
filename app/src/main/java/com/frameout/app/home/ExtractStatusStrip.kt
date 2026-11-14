package com.frameout.app.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.frameout.app.LocalTabActive
import com.frameout.app.LocalTabNav
import com.frameout.app.library.FabModel
import com.frameout.app.library.FabPhase
import com.frameout.app.library.LibraryViewModel
import com.frameout.app.library.fabModel
import com.frameout.app.library.percentOf
import com.frameout.app.library.toFab
import com.frameout.core.db.ProjectEntity
import com.frameout.core.util.ExtractFabStrings
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.dock.FrameStatusStrip
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.engine.work.LiveExtractProgress
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan

private val DockClearance = 84.dp

internal data class StripModel(val projectId: Long, val name: String, val fab: FabModel, val status: String)

private data class StripSnapshot(
    val projects: List<ProjectEntity> = emptyList(),
    val focus: Long? = null,
    val flash: Boolean = false,
    val last: Long? = null
)

private class Held {
    var value: StripModel? = null
}

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
    return remember(vm) { stripFlow(vm) }.collectAsState(initial = null)
}

@Composable
internal fun BoxScope.ExtractStatusHost() {
    val vm: LibraryViewModel = viewModel()
    val nav = LocalTabNav.current
    val active = LocalTabActive.current
    val reduce = LocalSystemA11y.current.reduceMotion
    val colors = FrameTheme.colors
    val model by remember(vm) { stripFlow(vm) }.collectAsState(initial = null)
    val held = remember { Held() }
    model?.let { held.value = it }
    val navBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val enter: EnterTransition = fadeIn(tween(200)) + if (reduce) EnterTransition.None else slideInVertically(tween(220)) { it / 2 }
    val exit: ExitTransition = fadeOut(tween(160)) + if (reduce) ExitTransition.None else slideOutVertically(tween(180)) { it / 2 }
    AnimatedVisibility(
        visible = active && model != null,
        modifier = Modifier.align(Alignment.BottomCenter).padding(start = 16.dp, end = 16.dp, bottom = navBar + DockClearance),
        enter = enter,
        exit = exit
    ) {
        val shown = held.value ?: return@AnimatedVisibility
        val open = { nav.onOpenProject(shown.projectId) }
        val modifier = Modifier.frameClickable(onClick = open)
        when {
            shown.fab.phase == FabPhase.Success -> FrameStatusStrip(
                title = FrameStrings.completed() + " ✓",
                detail = "",
                actionLabel = FrameStrings.openFrames(),
                onAction = open,
                modifier = modifier,
                actionColor = colors.primary,
                progress = 1f
            )
            else -> {
                val paused = shown.fab.phase == FabPhase.Resume
                val fraction = shown.fab.progress
                FrameStatusStrip(
                    title = (if (paused) FrameStrings.paused() else FrameStrings.extracting()) + FrameStrings.sep() + shown.name,
                    detail = fraction?.let { ExtractFabStrings.percent(percentOf(it)) }.orEmpty(),
                    actionLabel = if (paused) FrameStrings.resume() else FrameStrings.pause(),
                    onAction = { if (paused) vm.resume(shown.projectId) else vm.pause(shown.projectId) },
                    modifier = modifier,
                    actionColor = colors.primary,
                    progress = fraction,
                    indeterminate = fraction == null && !paused
                )
            }
        }
    }
}
