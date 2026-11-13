package com.frameout.app.settings

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameHairline

@Composable
internal fun ContentTagGrid(group: ContentGroup, title: String, selected: Set<String>, reservedRows: Int, onSet: (List<String>, Boolean) -> Unit) {
    val colors = FrameTheme.colors
    val tags = ContentCatalog.inGroup(group)
    val count = tags.count { it.id in selected }
    val allOn = count == tags.size
    Column(Modifier.fillMaxWidth().heightIn(min = 44.dp + ContentGridGap + ContentCellHeight * reservedRows + ContentGridGap * (reservedRows - 1))) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), Arrangement.spacedBy(8.dp), Alignment.CenterVertically) {
            FrameText(title, style = FrameTheme.typography.label, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            FrameText("$count / ${tags.size}", style = FrameTheme.typography.label, color = if (count > 0) colors.primary else colors.onSurfaceVariant)
            Box(
                Modifier.heightIn(min = 44.dp).clip(FrameShapes.pill).frameClickable(onClick = { onSet(tags.map { it.id }, !allOn) }).padding(horizontal = 8.dp),
                Alignment.Center
            ) {
                FrameText(
                    if (allOn) ContentStrings.groupNone() else ContentStrings.groupAll(),
                    style = FrameTheme.typography.label, color = colors.primary, maxLines = 1
                )
            }
        }
        Spacer(Modifier.height(ContentGridGap))
        Column(verticalArrangement = Arrangement.spacedBy(ContentGridGap)) {
            tags.chunked(CONTENT_GRID_COLUMNS).forEach { line ->
                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(ContentGridGap)) {
                    line.forEach { tag ->
                        TagCell(tag, tag.id in selected, Modifier.weight(1f)) { onSet(listOf(tag.id), tag.id !in selected) }
                    }
                    repeat(CONTENT_GRID_COLUMNS - line.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun TagCell(tag: ContentTag, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    val fill by animateColorAsState(if (selected) colors.primary.copy(alpha = 0.16f) else colors.onSurface.copy(alpha = 0.06f), frameSpring(), label = "cellFill")
    val edge by animateColorAsState(if (selected) colors.primary else frameHairline(), frameSpring(), label = "cellEdge")
    val ink by animateColorAsState(if (selected) colors.primary else colors.onSurfaceVariant, frameSpring(), label = "cellInk")
    Column(
        modifier
            .height(ContentCellHeight)
            .clip(FrameShapes.md)
            .background(fill)
            .border(if (selected) 1.5.dp else 1.dp, edge, FrameShapes.md)
            .frameSelectable(selected, Role.Checkbox, onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        Arrangement.spacedBy(4.dp, Alignment.CenterVertically), Alignment.CenterHorizontally
    ) {
        Image(painterResource(contentTagIcon(tag.id)), null, Modifier.size(24.dp), colorFilter = ColorFilter.tint(ink))
        FrameText(
            tag.title(FrameStrings.lang()),
            style = FrameTheme.typography.bodySmall.copy(fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal),
            color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center
        )
    }
}

@Composable
internal fun ContentContentOptions(spec: ContentSpec, onChange: (ContentSpec) -> Unit) {
    val colors = FrameTheme.colors
    val levels = listOf(ContentSpec.STRICT_LOOSE, ContentSpec.STRICT_BALANCED, ContentSpec.STRICT_STRICT)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(FrameShapes.lg)
                .background(colors.onSurface.copy(alpha = 0.04f))
                .border(1.dp, frameHairline(), FrameShapes.lg)
                .padding(12.dp),
            Arrangement.spacedBy(10.dp)
        ) {
            SettingRow(
                ContentStrings.matchLabel(),
                listOf(ContentStrings.matchAny(), ContentStrings.matchAll()),
                if (spec.all) 1 else 0,
                ContentStrings.matchShort(spec.all)
            ) { onChange(spec.copy(all = it == 1)) }
            Box(Modifier.fillMaxWidth().height(1.dp).background(frameHairline()))
            SettingRow(
                ContentStrings.strictLabel(),
                levels.map { ContentStrings.strictName(it) },
                levels.indexOf(spec.strict).coerceAtLeast(0),
                ContentStrings.strictShort(spec.strict)
            ) { onChange(spec.copy(strict = levels[it])) }
        }
        FrameText(
            ContentStrings.slow(), style = FrameTheme.typography.bodySmall, color = colors.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun SettingRow(label: String, options: List<String>, selectedIndex: Int, help: String, onSelect: (Int) -> Unit) {
    val colors = FrameTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(10.dp), Alignment.CenterVertically) {
            FrameText(
                label, style = FrameTheme.typography.label, color = colors.onSurfaceVariant,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(min = 56.dp, max = 96.dp)
            )
            SlidingSegments(options, selectedIndex, Modifier.weight(1f), onSelect)
        }
        Crossfade(help, animationSpec = frameSpring(), label = "settingHelp") { text ->
            FrameText(
                text, style = FrameTheme.typography.bodySmall, color = colors.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SlidingSegments(options: List<String>, selectedIndex: Int, modifier: Modifier, onSelect: (Int) -> Unit) {
    val colors = FrameTheme.colors
    BoxWithConstraints(
        modifier
            .height(44.dp)
            .clip(FrameShapes.pill)
            .background(colors.onSurface.copy(alpha = 0.05f))
            .border(1.dp, frameHairline(), FrameShapes.pill)
    ) {
        val segment = maxWidth / options.size
        val offset by animateDpAsState(segment * selectedIndex, frameSpring(), label = "segmentThumb")
        Box(
            Modifier
                .offset(x = offset)
                .width(segment)
                .fillMaxHeight()
                .padding(2.dp)
                .clip(FrameShapes.pill)
                .background(colors.primary.copy(alpha = 0.16f))
                .border(1.5.dp, colors.primary, FrameShapes.pill)
        )
        Row(Modifier.fillMaxSize()) {
            options.forEachIndexed { index, text -> SegmentLabel(text, index == selectedIndex, Modifier.weight(1f)) { onSelect(index) } }
        }
    }
}

@Composable
private fun SegmentLabel(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    val ink by animateColorAsState(if (selected) colors.primary else colors.onSurfaceVariant, frameSpring(), label = "segmentInk")
    Box(
        modifier
            .fillMaxHeight()
            .clip(FrameShapes.pill)
            .frameSelectable(selected, Role.RadioButton, onClick)
            .padding(horizontal = 6.dp),
        Alignment.Center
    ) {
        FrameText(
            text, style = FrameTheme.typography.label.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal),
            color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center
        )
    }
}
