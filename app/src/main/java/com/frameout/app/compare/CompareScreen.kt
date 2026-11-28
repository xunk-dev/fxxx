package com.frameout.app.compare

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameShapes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.frameout.app.PreferredFrameTheme
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.components.FrameSpinner
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.components.FrameToastHost
import com.frameout.designsystem.components.FrameTopBar
import com.frameout.designsystem.components.ToastKind
import com.frameout.designsystem.components.toastDurationMs
import com.frameout.designsystem.dock.FrameDock
import com.frameout.designsystem.dock.FrameFab
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.theme.FrameTheme

internal enum class CompareMode { Stack, Side, Slider, Diff }

private val Tags = listOf("A", "B", "C", "D")
private val PaneGap = 8.dp
private const val DEFAULT_RATIO = 16f / 9f
private const val LANDSCAPE_RATIO = 1.1f

@Composable
fun CompareScreen(frameIds: List<Long>, onClose: () -> Unit, vm: CompareViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    var userMode by rememberSaveable { mutableStateOf<String?>(null) }
    var linked by rememberSaveable { mutableStateOf(true) }
    var swapped by rememberSaveable { mutableStateOf(false) }
    val zoomA = remember { CompareZoom() }
    val zoomB = remember { CompareZoom() }
    val items = remember(state.frames, state.bitmaps, swapped) {
        val base = state.frames.mapIndexed { i, _ -> paneItem(i, state.bitmaps.getOrNull(i)) }
        if (swapped && base.size > 1) listOf(base[1], base[0]) + base.drop(2) else base
    }
    val mode = userMode?.let(CompareMode::valueOf) ?: autoMode(items.firstOrNull())
    CompareEffects(frameIds, mode, state, vm)
    PreferredFrameTheme {
        Box(Modifier.fillMaxSize().background(FrameTheme.colors.background)) {
            CompareColumn(
                items, mode, state, CompareZoomSet(zoomA, zoomB, linked),
                onClose = onClose,
                onSave = vm::saveAll,
                onMode = { userMode = it.name; zoomA.reset(); zoomB.reset() },
                onLink = { linked = !linked; zoomA.reset(); zoomB.reset() },
                onSwap = { swapped = !swapped; zoomA.reset(); zoomB.reset() }
            )
            CompareToast(state, vm::clearMessage, Modifier.align(Alignment.TopCenter))
            state.lockPrompt?.let { com.frameout.app.billing.LockPromptDialog(it, vm.lockGate) }
        }
    }
}

private fun autoMode(first: PaneItem?): CompareMode {
    val bitmap = first?.bitmap?.takeIf { !it.isRecycled } ?: return CompareMode.Side
    return if (bitmap.width.toFloat() / bitmap.height.coerceAtLeast(1) >= LANDSCAPE_RATIO) CompareMode.Stack else CompareMode.Side
}

@Composable
private fun CompareEffects(frameIds: List<Long>, mode: CompareMode, state: CompareState, vm: CompareViewModel) {
    val activity = androidx.activity.compose.LocalActivity.current
    androidx.compose.runtime.DisposableEffect(vm) { onDispose { if (activity?.isChangingConfigurations != true) vm.reset() } }
    LaunchedEffect(frameIds) { vm.open(frameIds) }
    LaunchedEffect(mode, state.fullReady) { if (mode == CompareMode.Diff) vm.ensureDiff() }
}

@Composable
private fun CompareColumn(
    items: List<PaneItem>, mode: CompareMode, state: CompareState, zooms: CompareZoomSet,
    onClose: () -> Unit, onSave: () -> Unit, onMode: (CompareMode) -> Unit, onLink: () -> Unit, onSwap: () -> Unit
) {
    val ready = items.size >= 2
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        FrameTopBar(
            FrameStrings.compare(),
            onBack = onClose,
            backContentDescription = FrameStrings.back(),
            actionIcon = if (ready) FrameIcons.Save else null,
            actionLabel = FrameStrings.save(),
            onAction = if (ready) onSave else null
        )
        Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), Alignment.Center) {
            when {
                state.loading && items.isEmpty() -> FrameSpinner()
                !ready -> FrameText(FrameStrings.compareNeedsTwo(), color = FrameTheme.colors.onSurfaceVariant)
                else -> CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    CompareBody(mode, items, zooms, state.diff, state.diffBusy, state.diffPercent)
                }
            }
        }
        if (ready) CompareControls(mode, onMode, zooms, onLink, onSwap)
    }
}

@Composable
private fun CompareControls(mode: CompareMode, onMode: (CompareMode) -> Unit, zooms: CompareZoomSet, onLink: () -> Unit, onSwap: () -> Unit) {
    val zoomed = zooms.zoomed
    Column(
        Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp),
        Arrangement.spacedBy(10.dp),
        Alignment.CenterHorizontally
    ) {
        if (mode != CompareMode.Diff) {
            if (!zoomed) FrameText(CompareStrings.hint(), style = FrameTheme.typography.bodySmall, color = FrameTheme.colors.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (mode == CompareMode.Stack || mode == CompareMode.Side) {
                    ToolChip(FrameIconsExtra.Link, if (zooms.linked) CompareStrings.link() else CompareStrings.unlink(), zooms.linked, onLink)
                }
                ToolChip(FrameIconsExtra.Swap, CompareStrings.swap(), false, onSwap)
                if (zoomed) ToolChip(FrameIconsExtra.Fit, CompareStrings.fit(), false, zooms::reset)
            }
        }
        ModeSwitch(mode, onMode)
    }
}

@Composable
private fun CompareToast(state: CompareState, onDismiss: () -> Unit, modifier: Modifier) {
    FrameToastHost(
        state.message,
        modifier,
        kind = if (state.isError) ToastKind.Error else ToastKind.Success,
        saved = state.messageSaved,
        busy = state.messageBusy,
        onDismiss = onDismiss
    )
}

private fun fitSize(maxW: Dp, maxH: Dp, ratio: Float): DpSize {
    var w = maxW
    var h = w / ratio
    if (h > maxH) {
        h = maxH
        w = h * ratio
    }
    return DpSize(w, h)
}

private fun ratioOf(item: PaneItem): Float {
    val bitmap = item.bitmap?.takeIf { !it.isRecycled } ?: return DEFAULT_RATIO
    return (bitmap.width.toFloat() / bitmap.height.coerceAtLeast(1)).coerceIn(0.25f, 4f)
}

@Composable
private fun CompareBody(mode: CompareMode, items: List<PaneItem>, zooms: CompareZoomSet, diff: android.graphics.Bitmap?, diffBusy: Boolean, diffPercent: Float) {
    val a = items[0]
    val b = items[1]
    val ratio = ratioOf(a)
    BoxWithConstraints(Modifier.fillMaxSize(), Alignment.Center) {
        when (mode) {
            CompareMode.Stack -> {
                val pane = fitSize(maxWidth, (maxHeight - PaneGap) / 2, ratio)
                Column(Modifier.size(pane.width, pane.height * 2 + PaneGap), Arrangement.spacedBy(PaneGap)) {
                    ZoomPane(a, zooms.a, Modifier.fillMaxWidth().weight(1f), primary = true, tagged = false)
                    ZoomPane(b, zooms.forB(), Modifier.fillMaxWidth().weight(1f), primary = !zooms.linked, tagged = false)
                }
            }
            CompareMode.Side -> {
                val pane = fitSize((maxWidth - PaneGap) / 2, maxHeight, ratio)
                Row(Modifier.size(pane.width * 2 + PaneGap, pane.height), Arrangement.spacedBy(PaneGap)) {
                    ZoomPane(a, zooms.a, Modifier.weight(1f).fillMaxHeight(), primary = true, tagged = false)
                    ZoomPane(b, zooms.forB(), Modifier.weight(1f).fillMaxHeight(), primary = !zooms.linked, tagged = false)
                }
            }
            CompareMode.Slider -> {
                val box = fitSize(maxWidth, maxHeight, ratio)
                SliderCompare(a, b, zooms.a, Modifier.size(box.width, box.height), tagged = false)
            }
            CompareMode.Diff -> {
                val box = fitSize(maxWidth, maxHeight - CaptionHeight, ratio)
                Column(Modifier.size(box.width, box.height + CaptionHeight)) {
                    DiffPane(diff, diffBusy, Modifier.fillMaxWidth().weight(1f))
                    if (diff != null) DiffCaption(diffPercent, Modifier.fillMaxWidth()) else Box(Modifier.height(CaptionHeight))
                }
            }
        }
    }
}

@Composable
private fun DiffPane(diff: android.graphics.Bitmap?, busy: Boolean, modifier: Modifier) {
    val colors = FrameTheme.colors
    Box(modifier.clip(FrameShapes.sm).background(colors.container), Alignment.Center) {
        val bitmap = diff?.takeIf { !it.isRecycled }
        if (bitmap == null) {
            if (busy) FrameText(CompareStrings.diffBusy(), color = colors.onSurfaceVariant) else FrameSpinner()
        } else {
            Image(remember(bitmap) { bitmap.asImageBitmap() }, CompareStrings.diff(), Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        }
    }
}

private fun paneItem(index: Int, bitmap: android.graphics.Bitmap?): PaneItem {
    val tag = Tags.getOrElse(index) { "?" }
    return PaneItem(tag = tag, bitmap = bitmap, title = "", subtitle = "", description = tag)
}
