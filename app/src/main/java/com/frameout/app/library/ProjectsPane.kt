package com.frameout.app.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.frameout.core.db.ProjectEntity
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.rememberFrameAdaptive

class ProjectActions(
    val onOpen: (Long) -> Unit,
    val onToggle: (Long) -> Unit,
    val onResume: (Long) -> Unit,
    val onPause: (Long) -> Unit,
    val onDelete: (Long) -> Unit,
    val onAdd: () -> Unit
)

@Composable
fun ProjectsPane(
    projects: List<ProjectEntity>,
    selectedIds: Set<Long>,
    listState: LazyListState,
    bottomPadding: Dp,
    actions: ProjectActions
) {
    if (projects.isEmpty()) {
        LibraryEmpty(
            icon = FrameIconsExtra.Folder,
            title = FrameStrings.noProjects(),
            hint = FrameStrings.emptyProjectsHint(),
            actionLabel = FrameStrings.addVideo(),
            onAction = actions.onAdd
        )
        return
    }
    val adaptive = rememberFrameAdaptive()
    val columns = adaptive.listColumns()
    val padding = PaddingValues(top = 4.dp, bottom = bottomPadding, end = adaptive.railInset)
    if (columns > 1) {
        LazyVerticalGrid(columns = GridCells.Fixed(columns), contentPadding = padding, modifier = Modifier.fillMaxSize()) {
            gridItems(projects, key = { it.id }, contentType = { "project" }) { ProjectItem(it, selectedIds, actions) }
        }
    } else {
        LazyColumn(state = listState, contentPadding = padding, modifier = Modifier.fillMaxSize()) {
            items(projects, key = { it.id }, contentType = { "project" }) { ProjectItem(it, selectedIds, actions) }
        }
    }
}

@Composable
private fun ProjectItem(project: ProjectEntity, selectedIds: Set<Long>, actions: ProjectActions) {
    Box {
        ProjectCard(
            project = project,
            selected = project.id in selectedIds,
            onOpen = { if (selectedIds.isNotEmpty()) actions.onToggle(project.id) else actions.onOpen(project.id) },
            onLongClick = { actions.onToggle(project.id) },
            onResume = { actions.onResume(project.id) },
            onPause = { actions.onPause(project.id) },
            onDelete = { actions.onDelete(project.id) }
        )
    }
}
