package com.frameout.app.viewer

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.designsystem.a11y.frameSelectable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameControl

internal class PanelSection(
    val icon: Int,
    val label: String,
    val modified: Boolean = false,
    val content: @Composable ColumnScope.() -> Unit
)

private val STRIP_HEIGHT = 38.dp
private val CHIP_HEIGHT = 30.dp

@Composable
internal fun SectionedBody(sections: List<PanelSection>, stateKey: Any, modifier: Modifier = Modifier, fill: Boolean = true) {
    var selected by rememberSaveable(stateKey) { mutableIntStateOf(0) }
    val index = selected.coerceIn(0, (sections.size - 1).coerceAtLeast(0))
    Column(if (fill) modifier.fillMaxSize() else modifier.fillMaxWidth()) {
        if (sections.size > 1) {
            Row(
                Modifier.fillMaxWidth().height(STRIP_HEIGHT).horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                sections.forEachIndexed { i, section -> SectionChip(section, i == index) { selected = i } }
            }
        }
        Box(
            (if (fill) Modifier.weight(1f) else Modifier).fillMaxWidth().padding(horizontal = 14.dp, vertical = if (fill) 0.dp else 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) { sections.getOrNull(index)?.content?.invoke(this) }
        }
    }
}

@Composable
private fun SectionChip(section: PanelSection, selected: Boolean, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    val tint by animateColorAsState(if (selected) colors.primary else colors.onSurfaceVariant, tween(180), label = "sectionTint")
    Box(
        Modifier.pressScale().height(STRIP_HEIGHT)
            .frameSelectable(selected, Role.Tab, onClick)
            .semantics { contentDescription = section.label },
        contentAlignment = Alignment.Center
    ) {
        Row(
            Modifier.height(CHIP_HEIGHT)
                .animateContentSize(spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow))
                .then(if (selected) Modifier.frameControl(FrameShapes.pill, selected = true, glowAlpha = 0f) else Modifier)
                .clip(FrameShapes.pill)
                .padding(horizontal = if (selected) 11.dp else 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box {
                Image(painterResource(section.icon), null, Modifier.size(16.dp), colorFilter = ColorFilter.tint(tint))
                if (section.modified && !selected) {
                    Box(Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-3).dp).size(5.dp).clip(CircleShape).background(colors.primary))
                }
            }
            if (selected) {
                Spacer(Modifier.width(5.dp))
                FrameText(section.label, style = FrameTheme.typography.label.copy(fontSize = 11.sp), color = tint, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
