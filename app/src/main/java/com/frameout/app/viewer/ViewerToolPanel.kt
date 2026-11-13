package com.frameout.app.viewer

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import com.frameout.designsystem.theme.rememberFrameAdaptive
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.frameout.core.util.EditStrings
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.ShapeStrings
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.glass.GlassSurface
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme

private class EditorTab(val tool: EditTool, val icon: Int, val label: () -> String, val title: () -> String, val mode: DrawMode? = null) {
    fun matches(tool: EditTool, draft: EditDraft) = this.tool == tool && (mode == null || mode == draft.drawMode)
}

private val EDITOR_TABS = listOf(
    EditorTab(EditTool.Crop, FrameIcons.Crop, EditStrings::cropRotate, EditStrings::cropRotate),
    EditorTab(EditTool.Adjust, FrameIconsExtra.Adjust, EditStrings::adjust, EditStrings::adjust),
    EditorTab(EditTool.Filter, FrameIconsExtra.PhotoFilter, EditStrings::filters, EditStrings::filters),
    EditorTab(EditTool.Draw, FrameIconsExtra.Brush, EditStrings::draw, EditStrings::draw, DrawMode.Pen),
    EditorTab(EditTool.Draw, FrameIconsExtra.Shapes, ShapeStrings::title, ShapeStrings::title, DrawMode.Shapes),
    EditorTab(EditTool.Cover, FrameIconsExtra.Cover, EditStrings::cover, EditStrings::cover),
    EditorTab(EditTool.Text, FrameIconsExtra.Text, EditStrings::text, EditStrings::text)
)

@Composable
internal fun ViewerToolPanel(state: ViewerState, tools: ViewerToolEditor) {
    val adaptive = rememberFrameAdaptive()
    val side = adaptive.sideEditor
    val keyboard = if (state.tool == EditTool.Text) Modifier.imePadding() else Modifier
    val enter = rememberPanelEnter()
    val frame = if (side) Modifier.statusBarsPadding() else Modifier
    Box(Modifier.fillMaxSize().navigationBarsPadding().then(frame).then(keyboard), contentAlignment = if (side) Alignment.CenterEnd else Alignment.BottomCenter) {
        GlassSurface(
            shape = FrameShapes.lg,
            glow = FrameTheme.colors.primary,
            modifier = (if (side) Modifier.fillMaxHeight().width(SIDE_PANEL_WIDTH) else Modifier.widthIn(max = BOTTOM_PANEL_MAX_WIDTH).fillMaxWidth())
                .onSizeChanged { tools.setPanelHeight(if (side) it.width else it.height) }
                .graphicsLayer {
                    alpha = enter.value * (if (state.toolBusy) 0.55f else 1f)
                    val shift = (1f - enter.value) * 48.dp.toPx()
                    if (side) translationX = shift else translationY = shift
                }
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            val tabScroll = rememberScrollState()
            var tabsViewport by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
            Column(if (side) Modifier.fillMaxHeight() else Modifier) {
                if (side) PanelHeader(state, tools)
                val bodyModifier = if (side) Modifier.weight(1f).fillMaxWidth().heightIn(min = EDITOR_PANEL_BODY) else Modifier.fillMaxWidth().height(if (state.draft.pickerOpen) EDITOR_PANEL_BODY else EDITOR_PANEL_FIXED).animateContentSize()
                Box(bodyModifier) { PanelContent(state, tools, fill = side) }
                GlowScrollRail(tabScroll, tabsViewport)
                PanelTabs(state, tools, tabScroll) { tabsViewport = it }
            }
        }
        if (!side) CompactTopHeader(state, tools, enter.value)
    }
}

@Composable
private fun PanelContent(state: ViewerState, tools: ViewerToolEditor, fill: Boolean) {
    if (state.draft.pickerOpen) {
        Box(Modifier.fillMaxSize(), Alignment.Center) {
            ColorPickerPanel(pickerColorFor(state.draft), tools::setPickerColor) { tools.setPickerOpen(false) }
        }
    } else {
        AnimatedToolBody(state, fill) { PanelBody(it, tools, fill) }
    }
}

@Composable
private fun CompactTopHeader(state: ViewerState, tools: ViewerToolEditor, enter: Float) {
    Box(Modifier.fillMaxSize().statusBarsPadding().graphicsLayer { alpha = enter }, contentAlignment = Alignment.TopCenter) {
        GlassSurface(
            shape = FrameShapes.lg,
            modifier = Modifier.widthIn(max = BOTTOM_PANEL_MAX_WIDTH).fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
        ) { PanelHeader(state, tools, compact = true) }
    }
}

@Composable
private fun PanelHeader(state: ViewerState, tools: ViewerToolEditor, compact: Boolean = false) {
    val draft = state.draft
    val reset: Reset = when (state.tool) {
        EditTool.Crop -> Reset(state.hasCrop || draft.transformed, EditStrings.resetCropRotate()) { tools.recordingRedo(tools::resetCropRotate) }
        EditTool.Adjust -> Reset(draft.adjusted, EditStrings.resetAdjust()) { tools.recordingRedo(tools::resetAdjust) }
        EditTool.Filter -> Reset(draft.filtered, EditStrings.resetFilter()) { tools.recordingRedo(tools::resetFilter) }
        else -> Reset(draft.marks.isNotEmpty(), EditStrings.undoMark()) { tools.recordingRedo(tools::undoMark) }
    }
    val title = EDITOR_TABS.firstOrNull { it.matches(state.tool, state.draft) }?.title?.invoke().orEmpty()
    Row(
        Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, top = if (compact) 4.dp else 10.dp, bottom = if (compact) 4.dp else 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        PanelIconButton(FrameIcons.Close, FrameStrings.cancel(), onClick = tools::requestCancel)
        FrameText(
            title,
            style = FrameTheme.typography.label,
            color = FrameTheme.colors.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        PanelIconButton(FrameIconsExtra.Undo, reset.label, reset.enabled, reset.action)
        PanelIconButton(FrameIconsExtra.Redo, EditStrings.redo(), state.canRedo, tools::redo)
        PanelDoneButton(FrameStrings.done(), FrameIcons.Check, tools::commit)
    }
}

private class Reset(val enabled: Boolean, val label: String, val action: () -> Unit)

@Composable
private fun PanelTabs(state: ViewerState, tools: ViewerToolEditor, scroll: androidx.compose.foundation.ScrollState, onViewport: (Int) -> Unit) {
    val gate = com.frameout.app.billing.rememberProGate()
    Row(
        Modifier.fillMaxWidth().onSizeChanged { onViewport(it.width) }.horizontalScroll(scroll).padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        EDITOR_TABS.forEach { tab ->
            val tier = tabTier(tab.tool)
            val shown = if (gate.locked(com.frameout.core.pro.ProFeature.EditTools, tier)) tier else com.frameout.core.pro.ProTier.Free
            ToolTab(tab.icon, tab.label(), tab.matches(state.tool, state.draft), { selectTab(gate, tab, tools) }, Modifier.widthIn(min = 58.dp), shown)
        }
    }
}

private fun tabTier(tool: EditTool): com.frameout.core.pro.ProTier =
    if (tool == EditTool.Text) com.frameout.core.pro.ProCatalog.textTier() else com.frameout.core.pro.ProTier.Free

private fun selectTab(gate: com.frameout.app.billing.ProGate, tab: EditorTab, tools: ViewerToolEditor) {
    gate.use(com.frameout.core.pro.ProFeature.EditTools, tabTier(tab.tool)) {
        tools.select(tab.tool)
        tab.mode?.let { tools.shapes.setMode(it) }
    }
}

@Composable
private fun PanelBody(state: ViewerState, tools: ViewerToolEditor, fill: Boolean) {
    val dims = ImageDims(state.bitmap?.width ?: 1000, state.bitmap?.height ?: 1000)
    val tool = state.tool
    val sections: List<PanelSection> = when (tool) {
        EditTool.Crop -> cropSections(state, tools)
        EditTool.Adjust -> adjustSections(state.draft, tools)
        EditTool.Filter -> filterSections(state, tools)
        EditTool.Draw -> drawSections(state.draft, dims, tools)
        EditTool.Cover -> coverSections(state.draft, dims, tools)
        EditTool.Text -> textSections(state.displayDraft(), dims, tools)
        EditTool.None -> emptyList()
    }
    if (sections.isEmpty()) return
    SectionedBody(sections, stateKey = if (tool == EditTool.Draw) "draw-${state.draft.drawMode}" else tool, fill = fill)
}

@Composable
private fun rememberPanelEnter(): androidx.compose.runtime.State<Float> {
    var shown by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) { shown = true }
    val reduce = com.frameout.designsystem.a11y.LocalSystemA11y.current.reduceMotion
    return androidx.compose.animation.core.animateFloatAsState(
        if (shown) 1f else 0f,
        if (reduce) androidx.compose.animation.core.snap() else spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
        label = "panelEnter"
    )
}

