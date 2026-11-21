package com.frameout.app.collage

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import kotlinx.coroutines.delay
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.frameout.app.viewer.ColorPickerPanel
import com.frameout.app.viewer.PanelIconButton
import com.frameout.core.util.CollageStrings
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.a11y.frameSelectable
import com.frameout.designsystem.theme.frameControl
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.glass.GlassSurface
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme

private val BODY_HEIGHT = 48.dp
private const val INTRO_MS = 2600L
private val PICKER_HEIGHT = 196.dp
private val MERGE_HEIGHT = 112.dp

internal enum class CollageTab(val icon: Int, val label: () -> String) {
    Frames(FrameIcons.GridSize, CollageStrings::tabFrames),
    Merge(FrameIconsExtra.CollageMerge, CollageStrings::tabMerge),
    Style(FrameIconsExtra.Palette, CollageStrings::tabStyle),
    Canvas(FrameIconsExtra.Fit, CollageStrings::tabCanvas)
}

@Composable
fun CollagePanel(state: CollageState, modifier: Modifier = Modifier) {
    var tab by rememberSaveable { mutableStateOf(CollageTab.Frames) }
    var intro by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(INTRO_MS)
        intro = false
    }
    GlassSurface(modifier.fillMaxWidth(), FrameShapes.lg) {
        Column {
            PanelHeader(tab, state, intro) { state.pickerOpen = false; intro = false; tab = it }
            Box(Modifier.fillMaxWidth().height(1.dp).background(FrameTheme.colors.divider))
            PanelBody(tab, state)
        }
    }
}

@Composable
private fun PanelHeader(tab: CollageTab, state: CollageState, intro: Boolean, onTab: (CollageTab) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            CollageTab.entries.forEach { TabPill(it, tab == it, intro) { onTab(it) } }
        }
        PanelIconButton(FrameIconsExtra.Undo, FrameStrings.undo(), state.history.canUndo) { state.history.undo() }
        PanelIconButton(FrameIconsExtra.Redo, FrameStrings.redo(), state.history.canRedo) { state.history.redo() }
    }
}

@Composable
private fun TabPill(tab: CollageTab, selected: Boolean, intro: Boolean, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    val tint by animateColorAsState(if (selected) colors.primary else colors.onSurfaceVariant, tween(200), label = "tabPillTint")
    val label = tab.label()
    Row(
        Modifier
            .height(40.dp)
            .then(if (selected) Modifier.frameControl(FrameShapes.pill, selected = true, glowAlpha = 0f) else Modifier)
            .clip(FrameShapes.pill)
            .frameSelectable(selected, Role.Tab, onClick)
            .semantics { contentDescription = label }
            .padding(horizontal = if (selected) 12.dp else 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(painterResource(tab.icon), null, Modifier.size(20.dp), colorFilter = ColorFilter.tint(tint))
        if (selected || intro) FrameText(label, style = FrameTheme.typography.label.copy(fontWeight = FontWeight.SemiBold), color = tint, maxLines = 1)
    }
}

@Composable
private fun PanelBody(tab: CollageTab, state: CollageState) {
    if (state.pickerOpen) {
        Box(Modifier.fillMaxWidth().height(PICKER_HEIGHT).verticalScroll(rememberScrollState()), Alignment.Center) {
            ColorPickerPanel(state.bgColor, { state.bgColor = it }) { state.pickerOpen = false }
        }
        return
    }
    key(tab) {
        Box(Modifier.fillMaxWidth().height(if (tab == CollageTab.Merge) MERGE_HEIGHT else BODY_HEIGHT).padding(horizontal = 12.dp), Alignment.Center) {
            when (tab) {
                CollageTab.Frames -> FramesTab(state)
                CollageTab.Merge -> MergeTab(state)
                CollageTab.Style -> StyleTab(state)
                CollageTab.Canvas -> CanvasTab(state)
            }
        }
    }
}
