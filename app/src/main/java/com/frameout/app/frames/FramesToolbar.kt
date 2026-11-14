package com.frameout.app.frames

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.components.FrameConfirmDialog
import com.frameout.designsystem.dock.DockItem
import com.frameout.designsystem.dock.FrameDock
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra

@Composable
fun FramesToolbar(
    count: Int,
    deletedView: Boolean,
    onDelete: () -> Unit,
    onRestore: () -> Unit,
    onFavorite: () -> Unit,
    onRotate: () -> Unit,
    onFlipH: () -> Unit,
    onFlipV: () -> Unit,
    onSelectAll: () -> Unit,
    onClear: () -> Unit,
    onCollage: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onCropAll: () -> Unit,
    onCompare: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    collapsed: Boolean,
    canSearch: Boolean = false,
    onSearch: () -> Unit = {},
    onEmptyTrash: (() -> Unit)? = null,
    fab: (@Composable () -> Unit)? = null,
    navItems: List<DockItem> = emptyList()
) {
    var more by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmEmpty by remember { mutableStateOf(false) }
    LaunchedEffect(count) {
        if (count == 0) {
            more = false
            confirmDelete = false
            confirmEmpty = false
        }
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        if (count > 0 || fab != null || navItems.isNotEmpty()) {
            val selecting = count > 0
            FrameDock(
                items = if (selecting) dockItems(count, deletedView, onSelectAll, onShare, onSave, onCollage, onClear, onRestore) { more = !more } else navItems,
                sideFab = if (selecting) null else fab,
                collapsed = collapsed
            )
        }
        MoreSheet(
            visible = more && count > 0,
            selected = count,
            actions = moreActions(
                deletedView, canSearch, onRestore, onFavorite, onRotate, onFlipH, onFlipV,
                onCropAll, onCompare, onUndo, onRedo, onSearch,
                onDelete = {
                    more = false
                    if (deletedView) confirmDelete = true else onDelete()
                },
                onEmptyTrash = onEmptyTrash?.let { {
                    more = false
                    confirmEmpty = true
                } }
            ),
            onClose = { more = false }
        )
        if (confirmDelete) {
            FrameConfirmDialog(
                title = FrameStrings.confirmPermanentDelete(),
                body = FrameStrings.confirmPermanentDeleteBody(),
                confirmLabel = FrameStrings.deletePermanently(),
                dismissLabel = FrameStrings.cancel(),
                onConfirm = { confirmDelete = false; onDelete() },
                onDismiss = { confirmDelete = false },
                destructive = true
            )
        }
        if (confirmEmpty && onEmptyTrash != null) {
            FrameConfirmDialog(
                title = FrameStrings.confirmEmptyTrash(),
                body = FrameStrings.confirmEmptyTrashBody(),
                confirmLabel = FrameStrings.emptyTrash(),
                dismissLabel = FrameStrings.cancel(),
                onConfirm = { confirmEmpty = false; onEmptyTrash() },
                onDismiss = { confirmEmpty = false },
                destructive = true
            )
        }
    }
}

private fun dockItems(count: Int, deleted: Boolean, selectAll: () -> Unit, share: () -> Unit, save: () -> Unit, collage: () -> Unit, clear: () -> Unit, restore: () -> Unit, more: () -> Unit): List<DockItem> {
    if (count == 0) return emptyList()
    val second = if (deleted) DockItem("restore", FrameStrings.restore(), FrameIconsExtra.Resume, onClick = restore) else DockItem("save", FrameStrings.save(), FrameIcons.Save, onClick = save)
    val third = if (deleted) DockItem("close", FrameStrings.close(), FrameIcons.Close, onClick = clear) else DockItem("collage", FrameStrings.collage(), FrameIcons.Collage, onClick = collage)
    val first = if (deleted) DockItem("select", FrameStrings.selectAll(), FrameIcons.SelectAll, onClick = selectAll) else DockItem("share", FrameStrings.share(), FrameIcons.Share, onClick = share)
    return listOf(first, second, third, DockItem("more", FrameStrings.more(), FrameIconsExtra.More, onClick = more))
}
