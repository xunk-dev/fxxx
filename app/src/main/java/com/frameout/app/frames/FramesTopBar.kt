package com.frameout.app.frames

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.glass.framePopupSurface
import com.frameout.designsystem.icons.FrameIconsExtra
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.BasicTextField
import com.frameout.core.util.NameSanitizer
import com.frameout.designsystem.components.FrameMarqueeText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.QualityStrings
import com.frameout.core.util.ScrubStrings
import com.frameout.designsystem.a11y.liveMessage
import com.frameout.designsystem.a11y.screenHeading
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.fieldFill
import com.frameout.designsystem.theme.frameHairline

private val TopBarHeight = 72.dp

@Composable
fun FramesTopBar(
    name: String,
    fit: GridFit,
    count: Int,
    selected: Int,
    onBack: () -> Unit,
    onRename: (String) -> Unit,
    onToggleFit: () -> Unit,
    onClearSelection: () -> Unit = {},
    onSelectAll: (() -> Unit)? = null,
    onExport: (() -> Unit)? = null,
    exportAlert: Boolean = false,
    onTrash: (() -> Unit)? = null,
    inTrash: Boolean = false,
    trashCount: Int = 0,
    onIntegrity: (() -> Unit)? = null,
    integrityAlert: Boolean = false
) {
    val colors = FrameTheme.colors
    var editing by remember(name) { mutableStateOf(false) }
    var text by remember(name) { mutableStateOf(name) }
    val selecting = selected > 0
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().height(TopBarHeight).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (editing) {
            val focus = remember { FocusRequester() }
            LaunchedEffect(Unit) { focus.requestFocus() }
            val commit = { if (text.isNotBlank()) onRename(NameSanitizer.clean(text, max = NameSanitizer.MAX_NAME_CHARS)); editing = false }
            Box(
                Modifier.weight(1f).clip(FrameShapes.md).background(colors.fieldFill).border(1.dp, colors.primary.copy(alpha = 0.5f), FrameShapes.md).padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                if (text.isEmpty()) FrameText(QualityStrings.renameHint(), color = colors.onSurfaceVariant, maxLines = 1)
                BasicTextField(
                    value = text,
                    onValueChange = { text = it.filter { c -> !c.isISOControl() }.take(NameSanitizer.MAX_NAME_CHARS) },
                    textStyle = FrameTheme.typography.body.copy(color = colors.onSurface),
                    cursorBrush = SolidColor(colors.primary),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { commit() }),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus)
                )
            }
            HeaderButton(FrameIcons.Close, FrameStrings.cancel(), { text = name; editing = false })
            HeaderButton(FrameIcons.Check, FrameStrings.save(), { commit() }, active = true)
        } else {
            if (selecting) HeaderButton(FrameIcons.Close, FrameStrings.close(), onClearSelection)
            else HeaderButton(FrameIcons.Back, FrameStrings.back(), onBack)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)) {
                if (selecting) {
                    FrameText(
                        "$selected/$count",
                        style = FrameTheme.typography.title,
                        color = colors.primary,
                        modifier = Modifier.liveMessage().semantics { contentDescription = ScrubStrings.selectedOfTotal(selected, count) }
                    )
                    FrameCountLine(0)
                } else {
                    FrameMarqueeText(
                        name,
                        style = FrameTheme.typography.title.copy(textDirection = TextDirection.Content),
                        modifier = Modifier.screenHeading().frameClickable { text = name; editing = true }
                    )
                    FrameCountLine(count)
                }
            }
            onExport?.let { HeaderButton(FrameIconsExtra.ExportOut, QualityStrings.toolsExport(), it, badge = exportAlert && !selecting) }
            if (selecting) onSelectAll?.let { HeaderButton(FrameIcons.SelectAll, FrameStrings.selectAll(), it) }
            if (!selecting) {
                if (inTrash && onTrash != null) HeaderButton(FrameIcons.Trash, FrameStrings.deleted(), onTrash, active = true, count = trashCount)
                else HeaderOverflow(fit, onToggleFit, trashCount, onTrash, onIntegrity, integrityAlert)
            }
        }
    }
}

@Composable
private fun FrameCountLine(count: Int) {
    val shown = count > 0
    FrameText(
        FrameStrings.frameCount(count.coerceAtLeast(1)),
        style = FrameTheme.typography.bodySmall,
        color = FrameTheme.colors.onSurfaceVariant,
        maxLines = 1,
        modifier = if (shown) Modifier else Modifier.alpha(0f).clearAndSetSemantics { }
    )
}

@Composable
internal fun HeaderButton(icon: Int, label: String, onClick: () -> Unit, active: Boolean = false, badge: Boolean = false, count: Int = 0) {
    val colors = FrameTheme.colors
    Box(
        Modifier
            .size(48.dp)
            .frameClickable(onClick = onClick)
            .semantics { contentDescription = if (count > 0) "$label $count" else label },
        Alignment.Center
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(FrameShapes.md)
                .background(if (active) colors.primary.copy(alpha = 0.16f) else colors.fieldFill)
                .border(1.dp, if (active) colors.primary.copy(alpha = 0.4f) else frameHairline(), FrameShapes.md),
            Alignment.Center
        ) {
            Image(painterResource(icon), null, Modifier.size(22.dp), colorFilter = ColorFilter.tint(if (active) colors.primary else colors.onSurface))
            if (badge && count <= 0) Box(Modifier.align(Alignment.TopEnd).padding(8.dp).size(8.dp).background(colors.error, FrameShapes.pill))
        }
        if (count > 0) CountBadge(count, Modifier.align(Alignment.TopEnd))
    }
}

@Composable
private fun CountBadge(count: Int, modifier: Modifier) {
    FrameText(
        if (count > 99) "99+" else count.toString(),
        style = FrameTheme.typography.label.copy(fontSize = 10.sp, textDirection = TextDirection.Ltr),
        color = Color.White,
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier = modifier
            .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
            .background(FrameTheme.colors.error, FrameShapes.pill)
            .padding(horizontal = 5.dp, vertical = 2.dp)
    )
}

@Composable
private fun HeaderOverflow(fit: GridFit, onToggleFit: () -> Unit, trashCount: Int, onTrash: (() -> Unit)?, onIntegrity: (() -> Unit)?, integrityAlert: Boolean) {
    var open by remember { mutableStateOf(false) }
    val gap = with(LocalDensity.current) { 4.dp.roundToPx() }
    Box {
        HeaderButton(FrameIconsExtra.More, FrameStrings.more(), { open = true }, active = open, badge = integrityAlert, count = trashCount)
        if (open) {
            Popup(BelowAnchor(gap), { open = false }, PopupProperties(focusable = true)) {
                Column(Modifier.width(IntrinsicSize.Max).widthIn(min = 210.dp, max = 300.dp).framePopupSurface(FrameShapes.xl).padding(vertical = 8.dp)) {
                    OverflowRow(if (fit == GridFit.Fill) FrameIcons.GridFill else FrameIcons.GridFit, if (fit == GridFit.Fill) FrameStrings.fill() else FrameStrings.fit(), null) { open = false; onToggleFit() }
                    if (onIntegrity != null) OverflowRow(if (integrityAlert) FrameIconsExtra.Warning else FrameIcons.Info, QualityStrings.integrity(), null, alert = integrityAlert) { open = false; onIntegrity() }
                    if (trashCount > 0 && onTrash != null) OverflowRow(FrameIcons.Trash, FrameStrings.deleted(), trashCount) { open = false; onTrash() }
                }
            }
        }
    }
}

@Composable
private fun OverflowRow(icon: Int, label: String, count: Int?, alert: Boolean = false, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    Row(
        Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp).frameClickable(onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(painterResource(icon), null, Modifier.size(22.dp), colorFilter = ColorFilter.tint(colors.onSurface))
        FrameText(label, modifier = Modifier.weight(1f).padding(horizontal = 14.dp))
        if (alert) Box(Modifier.size(8.dp).background(colors.error, FrameShapes.pill))
        if (count != null) FrameText(count.toString(), style = FrameTheme.typography.monoSmall, color = colors.onSurfaceVariant)
    }
}

private class BelowAnchor(private val gap: Int) : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
        val maxX = (windowSize.width - popupContentSize.width).coerceAtLeast(0)
        val maxY = (windowSize.height - popupContentSize.height).coerceAtLeast(0)
        val x = (if (layoutDirection == LayoutDirection.Rtl) anchorBounds.left else anchorBounds.right - popupContentSize.width).coerceIn(0, maxX)
        val y = (anchorBounds.bottom + gap).coerceIn(0, maxY)
        return IntOffset(x, y)
    }
}
