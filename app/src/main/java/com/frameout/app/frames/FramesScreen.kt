package com.frameout.app.frames

import com.frameout.app.asChooserWithoutSelf
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.first
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.frameout.app.PreferredFrameTheme
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.QualityStrings
import com.frameout.app.FrameOutApp
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.motion.frameBarEnter
import com.frameout.designsystem.motion.frameBarExit
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameAmbientGlow
import com.frameout.designsystem.theme.frameRailPadding

@OptIn(FlowPreview::class)
@Composable
fun FramesScreen(projectId: Long, onBack: () -> Unit, onCollage: (List<Long>) -> Unit = {}, onOpenFrame: (Long) -> Unit = {}, onCompare: (List<Long>) -> Unit = {}, vm: FramesViewModel = viewModel(), fab: (@Composable () -> Unit)? = null, navItems: List<com.frameout.designsystem.dock.DockItem> = emptyList(), focusFrameId: Long? = null, extracting: Boolean = fab != null) {
    val state by vm.state.collectAsState()
    val deleteConsent = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult()) { vm.onGalleryDeleteResult(it.resultCode == android.app.Activity.RESULT_OK) }
    LaunchedEffect(state.pendingGalleryDelete) {
        state.pendingGalleryDelete?.takeIf { !it.launched }?.let {
            it.launched = true
            deleteConsent.launch(androidx.activity.result.IntentSenderRequest.Builder(it.sender).build())
        }
    }
    val context = LocalContext.current
    val container = (context.applicationContext as FrameOutApp).container
    val prefs = container.prefs
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val exporter = remember { FramesExportAction(context) }
    val datasetAction = remember { DatasetExportAction(context) }
    val scrollSettled = rememberScrollSettled(gridState)
    val dockCollapsed by remember(scrollSettled, extracting) { derivedStateOf { !extracting && !scrollSettled && (gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 48) } }
    var columns by remember { mutableIntStateOf(prefs.gridColumns()) }
    var albumColumns by remember { mutableIntStateOf(2) }
    var scrubbing by remember { mutableStateOf(false) }
    var contactOpen by remember { mutableStateOf(false) }
    var datasetOpen by remember { mutableStateOf(false) }
    val topBarScroll = rememberTopBarScroll(gridState)
    val barVisible = topBarScroll.visible(state.selectionMode || extracting, scrollSettled) && (state.selectionMode || !scrubbing)
    val timeMsAt = remember(projectId, state.filter, state.tagFilter, state.sharpestVersion, state.smartFilter) {
        val smart = state.smartFilter != SmartFilter.None
        val tags = state.tagFilter.toList()
        val tagged = state.filter == FrameFilter.Tagged
        val lookup: suspend (Int) -> Long? = { offset ->
            if (smart) null else if (tagged) container.frameTags.ptsAtOffset(projectId, tags, tags.size, offset)?.div(1000) else if (state.filter == FrameFilter.Sharpest) container.sharpest.ptsMsAtSharpest(projectId, offset) else container.frames.ptsMsAt(projectId, state.filter, offset)
        }
        lookup
    }
    LaunchedEffect(projectId) {
        vm.load(projectId)
    }
    val leave: () -> Unit = { if (!vm.holdBack()) onBack() }
    val backConsumes = state.messageBusy || contactOpen || datasetOpen || state.cropAll != null || state.integrityOpen || state.tagsOpen || state.toolsOpen || state.filterOpen || state.selectionMode || state.isDeletedView
    androidx.activity.compose.BackHandler(enabled = backConsumes) {
        when {
            state.messageBusy -> leave()
            contactOpen -> contactOpen = false
            datasetOpen -> datasetOpen = false
            state.cropAll != null -> vm.cancelCropAll()
            state.integrityOpen -> vm.toggleIntegrity()
            state.tagsOpen -> vm.closeTags()
            state.toolsOpen -> vm.closeTools()
            state.filterOpen -> vm.closeFilter()
            state.selectionMode -> vm.clearSelection()
            state.isDeletedView -> vm.setFilter(FrameFilter.All)
        }
    }
    LaunchedEffect(state.message, state.analyzing, state.messageBusy) {
        val message = state.message
        if (message != null && !state.analyzing && !state.messageBusy) {
            val isError = isStatusError(message)
            kotlinx.coroutines.delay(toastWindowMs(message, isError, state.undoGalleryIds.isNotEmpty()))
            vm.clearMessage()
        }
    }
    LaunchedEffect(state.undoDelete) {
        if (state.undoDelete.isNotEmpty()) {
            kotlinx.coroutines.delay(6000)
            vm.clearUndoDelete()
        }
    }
    val hasFrames = state.frameCount > 0
    var hintId by remember(projectId, focusFrameId) { mutableStateOf(focusFrameId) }
    LaunchedEffect(projectId, focusFrameId, hasFrames, state.filter) {
        val target = focusFrameId ?: return@LaunchedEffect
        if (!hasFrames) return@LaunchedEffect
        if (state.filter != FrameFilter.All) { vm.setFilter(FrameFilter.All); return@LaunchedEffect }
        val position = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            container.frames.getActiveById(target)?.let { container.frames.countActiveBefore(it.projectId, it.index) }
        }
        if (position == null) { hintId = null; return@LaunchedEffect }
        kotlinx.coroutines.withTimeoutOrNull(4000) { snapshotFlow { gridState.layoutInfo.totalItemsCount }.first { it > position } }
        gridState.scrollToItem((position - columns).coerceAtLeast(0))
        kotlinx.coroutines.delay(5000)
        hintId = null
    }
    LaunchedEffect(projectId, hasFrames, state.filter) {
        if (hasFrames && state.filter == FrameFilter.All && focusFrameId == null) {
            gridState.scrollToItem(prefs.gridIndex(projectId), prefs.gridOffset(projectId))
        }
    }
    LaunchedEffect(state.filter, state.tagFilter) {
        if (state.filter != FrameFilter.All) gridState.scrollToItem(0)
    }
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.isScrollInProgress }.collect { com.frameout.core.quality.ScanThrottle.scrolling.value = it }
    }
    DisposableEffect(Unit) { onDispose { com.frameout.core.quality.ScanThrottle.scrolling.value = false } }
    LaunchedEffect(projectId) {
        snapshotFlow { gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset }
            .debounce(400)
            .collect { (index, offset) -> if (state.filter == FrameFilter.All) prefs.setGridPosition(projectId, index, offset) }
    }
    PreferredFrameTheme {
        val colors = FrameTheme.colors
        Box(Modifier.fillMaxSize().background(colors.background).frameAmbientGlow()) {
            Column(Modifier.fillMaxSize().nestedScroll(topBarScroll.connection)) {
                AnimatedVisibility(barVisible, enter = frameBarEnter(), exit = frameBarExit()) {
                    Column(Modifier.frameRailPadding()) {
                        FramesTopBar(
                            state.project?.name ?: FrameStrings.frames(), state.fit, state.frameCount, state.selected.size, leave, { vm.rename(it) }, { vm.toggleFit() },
                            onClearSelection = vm::clearSelection,
                            onSelectAll = if (state.isDeletedView) null else vm::selectAll,
                            onExport = if (state.isDeletedView) null else vm::openTools,
                            onIntegrity = if (state.integrity != null) vm::toggleIntegrity else null,
                            integrityAlert = state.integrity?.let { !it.healthy } == true,
                            onTrash = { vm.setFilter(if (state.isDeletedView) FrameFilter.All else FrameFilter.Deleted) },
                            inTrash = state.isDeletedView,
                            trashCount = state.deletedCount
                        )
                        val tabsTrailing: (@Composable () -> Unit)? = when {
                            state.selectionMode || state.isDeletedView || state.filter == FrameFilter.Tagged -> null
                            state.isAlbumsView -> ({ HeaderButton(com.frameout.designsystem.icons.FrameIconsExtra.Rescan, QualityStrings.contentRescan(), { if (!state.scanning) vm.rescanTags() }, active = state.scanning) })
                            else -> ({ HeaderButton(com.frameout.designsystem.icons.FrameIconsExtra.FilterSliders, QualityStrings.filterTitle(), vm::openFilter, active = state.smartFilter != SmartFilter.None || state.filter == FrameFilter.Sharpest) })
                        }
                        FramesTabsSlot(!(extracting && state.frameCount == 0 && state.filter == FrameFilter.All), state.filter, state.allCount, state.favoriteCount, state.sharpestCount, state.tagCounts.size, vm::setFilter, tabsTrailing)
                        if (state.smartFilter != SmartFilter.None && state.filter != FrameFilter.Tagged && !state.isAlbumsView) FramesSmartChip(state, vm::clearSmartFilter)
                        if (state.filter == FrameFilter.Tagged) FramesAlbumBar(state, { vm.setFilter(FrameFilter.Albums) }, vm::toggleTag, vm::openTags)
                    }
                }
                if (state.analyzing && state.filter == FrameFilter.Sharpest) CenterMsg(QualityStrings.analyzing())
                else if (state.loading) CenterMsg(FrameStrings.loading())
                else if (state.isAlbumsView) FramesAlbums(state, vm::albumCover, vm::openAlbum, vm::rescanTags, clearance = dockClearance(extracting), columns = albumColumns, onColumns = { albumColumns = it })
                else if (state.frameCount == 0) FramesEmptyState(state.filter, extracting = extracting, onClearFilter = { vm.clearSmartFilter(); vm.clearTags() })
                else FramesGrid(projectId, state, vm, gridState, columns, { columns = it; prefs.setGridColumns(it) }, onOpenFrame, timeMsAt, { scrubbing = it }, clearance = dockClearance(extracting || navItems.isNotEmpty()), highlightId = hintId)
            }
            Box(Modifier.fillMaxSize()) {
                FramesToolbar(state.selected.size, state.isDeletedView, if (state.isDeletedView) vm::permanentDeleteSelected else vm::softDeleteSelected, vm::restoreSelected, vm::toggleFavoriteSelected, vm::applyRotate90, vm::applyFlipH, vm::applyFlipV, vm::selectAll, vm::clearSelection, { vm.framesForSelection { onCollage(it.map { f -> f.id }) } }, vm::saveSelected, { vm.shareIntent { context.startActivity(it.asChooserWithoutSelf(context, FrameStrings.share())) } }, vm::openCropAll, { compareSelected(vm, onCompare) }, vm::undoSelected, vm::redoSelected, dockCollapsed, state.selected.size == 1, { vm.searchSelected(context) }, if (state.isDeletedView) vm::emptyTrash else null, fab = fab, navItems = navItems)
            }
            state.cropAll?.let { CropAllOverlay(it, vm::updateCropAll, vm::resetCropAll, vm::cancelCropAll, vm::applyCropAll) }
            state.integrity?.takeIf { state.integrityOpen }?.let { IntegrityPanel(it, vm::toggleIntegrity) }
            FramesToolsSheet(
                state, vm::closeTools,
                { exportSlides(vm, exporter, scope, projectId) },
                vm::reassembleVideo,
                { exportSelected(vm, exporter, scope, true) }, { exportSelected(vm, exporter, scope, false) },
                { exportZip(vm, exporter, scope) }, { exportCsv(vm, exporter, scope) }, { exportEvidence(vm, exporter, scope) },
                onContactSheet = { if (com.frameout.app.billing.ProCheck.passOrAd(com.frameout.core.pro.ProFeature.AdvancedExport) { contactOpen = true }) contactOpen = true },
                onDataset = { if (com.frameout.app.billing.ProCheck.passOrAd(com.frameout.core.pro.ProFeature.AdvancedExport) { datasetOpen = true }) datasetOpen = true }
            )
            FramesFilterSheet(state, vm::closeFilter, vm::showAllFrames, vm::applySmartFilter, vm::showSharpest)
            DatasetExportSheet(datasetOpen, state.frameCount, { datasetOpen = false }) { format, options -> exportDataset(vm, datasetAction, scope, projectId, format, options) }
            val contactTotal = if (state.selected.isNotEmpty()) state.selected.size else state.allCount
            val contactPreview = rememberFramesContactPreview(vm, contactOpen, state.selected, contactTotal)
            ContactSheetSheet(
                contactOpen,
                contactTotal,
                prefs,
                { contactOpen = false },
                preview = contactPreview
            ) { options -> exportContactSheet(vm, exporter, scope, options) }
            TagPanel(state, vm::toggleTag, vm::closeTags)
            FramesUndoBar(
                state.undoDelete.takeIf { it.isNotEmpty() }?.let { FrameStrings.framesDeleted(it.size) },
                vm::undoDelete,
                Modifier.align(Alignment.TopCenter)
            )
            FramesToast(state, vm, Modifier.align(Alignment.TopCenter))
            if (state.smartReextract) {
                com.frameout.designsystem.components.FrameConfirmDialog(
                    title = com.frameout.core.util.QualityStrings.toolsTitle(),
                    body = com.frameout.core.util.QualityStrings.reextractForSmart(),
                    confirmLabel = FrameStrings.close(),
                    dismissLabel = null,
                    onConfirm = vm::dismissSmartReextract,
                    onDismiss = vm::dismissSmartReextract,
                    tone = com.frameout.designsystem.components.DialogTone.Info,
                    icon = com.frameout.designsystem.icons.FrameIconsExtra.Sparkle
                )
            }
            if (state.confirmGallerySave > 0) {
                com.frameout.designsystem.components.FrameConfirmDialog(
                    title = FrameStrings.galleryConfirmTitle(state.confirmGallerySave),
                    body = FrameStrings.galleryConfirmBody(),
                    confirmLabel = FrameStrings.galleryConfirmAction(),
                    dismissLabel = FrameStrings.galleryConfirmKeep(),
                    onConfirm = vm::confirmGallerySave,
                    onDismiss = vm::dismissGallerySave,
                    icon = com.frameout.designsystem.icons.FrameIcons.Save
                )
            }
        }
    }
}

@Composable
private fun CenterMsg(text: String) {
    Box(Modifier.fillMaxSize().frameRailPadding(), contentAlignment = Alignment.Center) { FrameText(text, color = FrameTheme.colors.onSurfaceVariant) }
}

private fun dockClearance(hasFab: Boolean) = if (hasFab) 116.dp else 100.dp

@Suppress("UNUSED_PARAMETER")
private fun overlayBottom(hasFab: Boolean, hasSelection: Boolean) = if (hasFab) 108.dp else 112.dp

private fun toastWindowMs(message: String, isError: Boolean, undo: Boolean): Long {
    val kind = if (isError) com.frameout.designsystem.components.ToastKind.Error else com.frameout.designsystem.components.ToastKind.Success
    val base = com.frameout.designsystem.components.toastDurationMs(message, kind)
    return if (undo) maxOf(base, GALLERY_UNDO_WINDOW_MS) else base
}
