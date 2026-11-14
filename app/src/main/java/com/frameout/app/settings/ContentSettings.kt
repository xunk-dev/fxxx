package com.frameout.app.settings

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.frameout.core.quality.content.ContentCatalog
import com.frameout.core.quality.content.ContentGroup
import com.frameout.core.quality.content.ContentSpec
import com.frameout.core.quality.content.ContentTag
import com.frameout.core.util.ContentStrings
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.a11y.frameSelectable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.motion.frameSpring
import com.frameout.designsystem.theme.FrameGradients
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameHairline

internal fun contentSummary(spec: ContentSpec, lang: String): String =
    if (spec.tags.isEmpty()) ContentStrings.noneSelected() else ContentCatalog.summary(spec.tags, lang)

@Composable
internal fun ContentSheet(visible: Boolean, spec: ContentSpec, onChange: (ContentSpec) -> Unit, onClose: () -> Unit) {
    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.72f).dp
    BottomSheetFrame(visible, onClose, ContentStrings.title()) {
        Column(Modifier.heightIn(max = maxHeight).verticalScroll(rememberScrollState())) {
            ContentSheetBody(spec, onChange, Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        }
        Box(
            Modifier
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .fillMaxWidth()
                .height(48.dp)
                .clip(FrameShapes.pill)
                .background(FrameGradients.Brand)
                .frameClickable(onClick = onClose),
            Alignment.Center
        ) {
            FrameText(FrameStrings.done(), style = FrameTheme.typography.label, color = Color.White)
        }
    }
}

@Composable
internal fun ContentSheetBody(spec: ContentSpec, onChange: (ContentSpec) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, Arrangement.spacedBy(8.dp)) {
        ContentTagPicker(spec, onChange)
        ContentContentOptions(spec, onChange)
    }
}

internal const val CONTENT_GRID_COLUMNS = 3
internal val ContentCellHeight = 64.dp
internal val ContentGridGap = 8.dp

@Composable
internal fun ContentTagPicker(spec: ContentSpec, onChange: (ContentSpec) -> Unit, modifier: Modifier = Modifier) {
    val lang = FrameStrings.lang()
    val groups = ContentGroup.values().filter { ContentCatalog.inGroup(it).isNotEmpty() }
    var open by rememberSaveable { mutableStateOf(groups.firstOrNull { g -> ContentCatalog.inGroup(g).any { it.id in spec.tags } }?.id ?: groups.first().id) }
    val current = groups.firstOrNull { it.id == open } ?: groups.first()
    val rows = groups.map { (ContentCatalog.inGroup(it).size + CONTENT_GRID_COLUMNS - 1) / CONTENT_GRID_COLUMNS }.sorted().let { it[it.size / 2] }
    val set: (List<String>, Boolean) -> Unit = { ids, on ->
        onChange(spec.copy(tags = if (on) spec.tags + ids else spec.tags - ids.toSet()))
    }
    Column(modifier, Arrangement.spacedBy(8.dp)) {
        SelectionViewer(spec.tags, { set(listOf(it), false) }) { onChange(spec.copy(tags = emptySet())) }
        CategoryTabs(groups, lang, spec.tags, current.id) { open = it }
        Crossfade(current, animationSpec = frameSpring(), label = "tagGroup") { group ->
            ContentTagGrid(group, group.title(lang), spec.tags, rows, set)
        }
    }
}

@Composable
private fun SelectionViewer(selected: Set<String>, onRemove: (String) -> Unit, onClear: () -> Unit) {
    val colors = FrameTheme.colors
    val lang = FrameStrings.lang()
    val tags = ContentCatalog.selected(selected)
    val count = tags.size
    val edge by animateColorAsState(if (count > 0) colors.primary.copy(alpha = 0.5f) else frameHairline(), frameSpring(), label = "viewerEdge")
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(FrameShapes.pill)
            .background(colors.onSurface.copy(alpha = 0.04f))
            .border(1.dp, edge, FrameShapes.pill)
            .padding(horizontal = 4.dp),
        Arrangement.spacedBy(4.dp), Alignment.CenterVertically
    ) {
        if (count == 0) {
            FrameText(
                ContentStrings.pickHint(), style = FrameTheme.typography.bodySmall, color = colors.onSurfaceVariant,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
            )
        } else {
            LazyRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                items(tags, key = { it.id }) { tag -> ViewerChip(tag, lang) { onRemove(tag.id) } }
            }
            FrameText(count.toString(), style = FrameTheme.typography.label, color = colors.primary)
            Box(
                Modifier.heightIn(min = 44.dp).widthIn(min = 44.dp).clip(FrameShapes.pill).frameClickable(onClick = onClear).padding(horizontal = 8.dp),
                Alignment.Center
            ) {
                FrameText(ContentStrings.clear(), style = FrameTheme.typography.label, color = colors.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

@Composable
private fun ViewerChip(tag: ContentTag, lang: String, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    Box(Modifier.heightIn(min = 44.dp).clip(FrameShapes.pill).frameClickable(onClick = onClick), Alignment.Center) {
        Row(
            Modifier
                .height(32.dp)
                .clip(FrameShapes.pill)
                .background(colors.primary.copy(alpha = 0.16f))
                .border(1.dp, colors.primary, FrameShapes.pill)
                .padding(horizontal = 10.dp),
            Arrangement.spacedBy(6.dp), Alignment.CenterVertically
        ) {
            Image(painterResource(contentTagIcon(tag.id)), null, Modifier.size(16.dp), colorFilter = ColorFilter.tint(colors.primary))
            FrameText(tag.title(lang), style = FrameTheme.typography.label, color = colors.primary, maxLines = 1)
        }
    }
}

@Composable
private fun CategoryTabs(groups: List<ContentGroup>, lang: String, selected: Set<String>, open: String, onOpen: (String) -> Unit) {
    val state = rememberLazyListState()
    val index = groups.indexOfFirst { it.id == open }.coerceAtLeast(0)
    LaunchedEffect(index) { state.animateScrollToItem(index) }
    LazyRow(Modifier.fillMaxWidth(), state, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        itemsIndexed(groups, key = { _, g -> g.id }) { _, group ->
            val count = ContentCatalog.inGroup(group).count { it.id in selected }
            CategoryTab(group, group.title(lang), count, group.id == open) { onOpen(group.id) }
        }
    }
}

@Composable
private fun CategoryTab(group: ContentGroup, title: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    val fill by animateColorAsState(if (selected) colors.primary.copy(alpha = 0.16f) else colors.onSurface.copy(alpha = 0.04f), frameSpring(), label = "tabFill")
    val edge by animateColorAsState(if (selected) colors.primary else frameHairline(), frameSpring(), label = "tabEdge")
    val ink by animateColorAsState(if (selected) colors.primary else colors.onSurfaceVariant, frameSpring(), label = "tabInk")
    Row(
        Modifier
            .height(48.dp)
            .clip(FrameShapes.pill)
            .background(fill)
            .border(if (selected) 1.5.dp else 1.dp, edge, FrameShapes.pill)
            .frameSelectable(selected, Role.Tab, onClick)
            .padding(horizontal = 14.dp),
        Arrangement.spacedBy(6.dp), Alignment.CenterVertically
    ) {
        Image(painterResource(contentGroupIcon(group.id)), null, Modifier.size(20.dp), colorFilter = ColorFilter.tint(ink))
        FrameText(
            title, style = FrameTheme.typography.label.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal),
            color = ink, maxLines = 1
        )
        if (count > 0) {
            Box(
                Modifier.height(18.dp).widthIn(min = 18.dp).clip(FrameShapes.pill).background(colors.primary).padding(horizontal = 5.dp),
                Alignment.Center
            ) {
                FrameText(count.toString(), style = FrameTheme.typography.label, color = colors.onPrimary, maxLines = 1)
            }
        }
    }
}
