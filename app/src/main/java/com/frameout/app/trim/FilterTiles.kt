package com.frameout.app.trim

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.frameout.app.settings.SettingsIconTile
import com.frameout.app.settings.SettingsSwitch
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.a11y.frameToggle
import com.frameout.designsystem.components.DialogButton
import com.frameout.designsystem.components.DialogTone
import com.frameout.designsystem.components.FrameDialog
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.motion.frameSpring
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.fieldFill
import com.frameout.designsystem.theme.frameHairline

@Composable
internal fun FilterTile(
    icon: Int,
    title: String,
    help: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    onChange: (Boolean) -> Unit
) {
    val colors = FrameTheme.colors
    val shape = FrameShapes.md
    var info by remember { mutableStateOf(false) }
    val edge by animateColorAsState(if (checked) colors.primary else frameHairline(), frameSpring(), label = "filterEdge")
    Box(modifier.heightIn(min = 108.dp)) {
        Column(
            Modifier
                .fillMaxSize()
                .clip(shape)
                .background(if (checked) colors.primary.copy(alpha = 0.12f) else colors.fieldFill)
                .border(if (checked) 1.5.dp else 1.dp, edge, shape)
                .frameToggle(checked, onChange)
                .padding(horizontal = 6.dp, vertical = 12.dp),
            Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            Alignment.CenterHorizontally
        ) {
            SettingsIconTile(icon, checked)
            FrameText(
                title,
                style = FrameTheme.typography.label,
                color = if (checked) colors.primary else colors.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
            SettingsSwitch(checked)
        }
        FilterInfoDot(title, Modifier.align(Alignment.TopEnd).padding(4.dp)) { info = true }
    }
    if (info) {
        FrameDialog(
            onDismiss = { info = false },
            tone = DialogTone.Info,
            title = title,
            primary = DialogButton(FrameStrings.done()) { info = false },
            body = help
        )
    }
}

@Composable
private fun FilterInfoDot(label: String, modifier: Modifier, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    Box(
        modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(colors.onSurface.copy(alpha = 0.06f))
            .frameClickable(onClickLabel = label, onClick = onClick)
            .semantics { contentDescription = label },
        Alignment.Center
    ) {
        Image(painterResource(FrameIcons.Info), null, Modifier.size(15.dp), colorFilter = ColorFilter.tint(colors.onSurfaceVariant))
    }
}

internal class FilterItem(
    val icon: Int,
    val title: String,
    val dialogTitle: String,
    val help: String,
    val checked: Boolean,
    val pro: Boolean = false,
    val onChange: (Boolean) -> Unit
)

@Composable
internal fun SmartFilterCard(items: List<FilterItem>, modifier: Modifier = Modifier) {
    val colors = FrameTheme.colors
    val shape = FrameShapes.lg
    Column(
        modifier.fillMaxWidth().clip(shape).background(colors.fieldFill).border(1.dp, frameHairline(), shape).padding(8.dp),
        Arrangement.spacedBy(8.dp)
    ) {
        items.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max), Arrangement.spacedBy(8.dp)) {
                row.forEach { FilterGridTile(it, Modifier.weight(1f).fillMaxHeight()) }
            }
        }
    }
}

@Composable
private fun FilterTileLabel(item: FilterItem, modifier: Modifier) {
    val colors = FrameTheme.colors
    val checked = item.checked
    Column(modifier, Arrangement.spacedBy(2.dp)) {
        FrameText(item.title, style = FrameTheme.typography.label, color = colors.onSurface, maxLines = 2)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            FrameText(
                if (checked) FrameStrings.on() else FrameStrings.off(),
                style = FrameTheme.typography.bodySmall,
                color = if (checked) colors.primary else colors.onSurfaceVariant,
                maxLines = 1
            )
            if (item.pro) com.frameout.app.billing.ProBadge(compact = true)
        }
    }
}

@Composable
private fun FilterGridTile(item: FilterItem, modifier: Modifier) {
    val colors = FrameTheme.colors
    val shape = FrameShapes.md
    var info by remember { mutableStateOf(false) }
    val checked = item.checked
    val edge by animateColorAsState(if (checked) colors.primary.copy(alpha = 0.65f) else frameHairline(), frameSpring(), label = "gridTileEdge")
    val fill = if (checked) Brush.linearGradient(listOf(colors.primary.copy(alpha = 0.28f), colors.primary.copy(alpha = 0.08f)))
    else Brush.linearGradient(listOf(colors.onSurface.copy(alpha = 0.04f), colors.onSurface.copy(alpha = 0.04f)))
    Box(modifier.heightIn(min = 68.dp)) {
        Row(
            Modifier
                .fillMaxSize()
                .clip(shape)
                .background(fill)
                .border(1.dp, edge, shape)
                .frameToggle(checked, item.onChange)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            Arrangement.spacedBy(10.dp),
            Alignment.CenterVertically
        ) {
            SettingsIconTile(item.icon, checked)
            FilterTileLabel(item, Modifier.weight(1f).padding(end = 12.dp))
        }
        FilterInfoDot(item.dialogTitle, Modifier.align(Alignment.TopEnd).padding(2.dp)) { info = true }
    }
    if (info) {
        FrameDialog(
            onDismiss = { info = false },
            tone = DialogTone.Info,
            title = item.dialogTitle,
            primary = DialogButton(FrameStrings.done()) { info = false },
            body = item.help
        )
    }
}
