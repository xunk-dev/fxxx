package com.frameout.app.billing

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.haptics.FrameHaptic
import com.frameout.designsystem.haptics.rememberFrameHaptics
import com.frameout.designsystem.theme.FrameTheme

private const val PRESSED_SCALE = 0.04f
private const val PRESSED_TINT = 0.16f

@Composable
internal fun PressSurface(
    onClick: (() -> Unit)?,
    shape: Shape,
    fill: Color,
    edge: Color,
    modifier: Modifier = Modifier,
    edgeWidth: Dp = 1.dp,
    onClickLabel: String? = null,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: @Composable RowScope.() -> Unit
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val reduce = LocalSystemA11y.current.reduceMotion
    val press by animateFloatAsState(if (pressed) 1f else 0f, tween(if (pressed) 90 else 220), label = "pressSurface")
    val haptics = rememberFrameHaptics()
    val primary = FrameTheme.colors.primary
    val tap = if (onClick == null) Modifier else Modifier.clickable(source, null, onClickLabel = onClickLabel, role = Role.Button) {
        haptics.perform(FrameHaptic.Tap)
        onClick()
    }
    Row(
        modifier
            .graphicsLayer {
                val scale = 1f - (if (reduce) 0f else PRESSED_SCALE) * press
                scaleX = scale
                scaleY = scale
            }
            .then(tap)
            .clip(shape)
            .background(fill)
            .background(primary.copy(alpha = PRESSED_TINT * press))
            .border(edgeWidth, lerp(edge, primary, press * 0.85f), shape),
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}
