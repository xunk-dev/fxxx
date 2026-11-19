package com.frameout.app.viewer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.frameout.core.util.EditStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameTheme

private val ActionHeight = 48.dp

@Composable
internal fun DetectionBlurAction(
    color: Color,
    ink: Color,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val label = EditStrings.blur()
    Row(
        modifier
            .alpha(if (enabled) 1f else 0.45f)
            .height(ActionHeight)
            .clip(CircleShape)
            .background(color)
            .frameClickable(onClickLabel = label, enabled = enabled, onClick = onClick)
            .padding(horizontal = 22.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(painterResource(FrameIconsExtra.Blur), null, Modifier.size(22.dp), colorFilter = ColorFilter.tint(ink))
        FrameText(label, style = FrameTheme.typography.label, color = ink)
    }
}
