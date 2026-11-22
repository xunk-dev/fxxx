package com.frameout.app.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.frameout.app.MainTabBar
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.dock.DockItem
import com.frameout.designsystem.dock.FrameDock
import com.frameout.designsystem.icons.FrameIcons

class LibrarySelectionDock(
    val actionId: String,
    val actionIcon: Int,
    val actionLabel: String,
    val onAction: () -> Unit,
    val onDelete: () -> Unit,
    val extra: List<DockItem> = emptyList()
)

class LibraryDockActions(
    val onPause: () -> Unit,
    val onResume: () -> Unit,
    val onCancel: () -> Unit
)

@Composable
internal fun LibraryDock(modifier: Modifier = Modifier, selection: LibrarySelectionDock? = null, sideFab: (@Composable () -> Unit)? = null) {
    if (selection == null) {
        MainTabBar(1, sideFab = sideFab)
        return
    }
    val items = listOf(
        DockItem(selection.actionId, selection.actionLabel, selection.actionIcon, onClick = selection.onAction)
    ) + selection.extra + listOf(
        DockItem("delete", FrameStrings.delete(), FrameIcons.Trash, destructive = true, onClick = selection.onDelete)
    )
    Box(modifier.fillMaxSize()) {
        FrameDock(items = items)
    }
}
