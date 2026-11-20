package com.frameout.app.library

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.frameout.core.util.DetailStrings
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.glass.framePopupSurface
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme

@Composable
internal fun ProjectMoreAction(onDetails: () -> Unit, onDelete: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    val gap = with(LocalDensity.current) { 4.dp.roundToPx() }
    Box {
        CardAction(FrameIconsExtra.More, FrameStrings.more(), FrameTheme.colors.onSurfaceVariant, open, { open = true })
        if (open) {
            Popup(BelowOrAbove(gap), { open = false }, PopupProperties(focusable = true)) {
                Column(
                    Modifier.width(IntrinsicSize.Max).widthIn(min = 196.dp, max = 280.dp).framePopupSurface(FrameShapes.xl).padding(vertical = 8.dp)
                ) {
                    ProjectMenuRow(FrameIcons.Info, DetailStrings.details(), false) { open = false; onDetails() }
                    ProjectMenuRow(FrameIcons.Trash, FrameStrings.delete(), true) { open = false; onDelete() }
                }
            }
        }
    }
}

@Composable
private fun ProjectMenuRow(icon: Int, label: String, destructive: Boolean, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    val tint = if (destructive) colors.error else colors.onSurface
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 6.dp)
            .clip(FrameShapes.sm)
            .frameClickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Image(painterResource(icon), null, Modifier.size(20.dp), colorFilter = ColorFilter.tint(if (destructive) colors.error else colors.onSurfaceVariant))
        FrameText(label, style = FrameTheme.typography.body, color = tint, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private class BelowOrAbove(private val gap: Int) : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
        val maxX = (windowSize.width - popupContentSize.width).coerceAtLeast(0)
        val maxY = (windowSize.height - popupContentSize.height).coerceAtLeast(0)
        val x = (if (layoutDirection == LayoutDirection.Rtl) anchorBounds.left else anchorBounds.right - popupContentSize.width).coerceIn(0, maxX)
        val below = anchorBounds.bottom + gap
        val fits = below + popupContentSize.height <= windowSize.height
        val y = (if (fits) below else anchorBounds.top - gap - popupContentSize.height).coerceIn(0, maxY)
        return IntOffset(x, y)
    }
}
