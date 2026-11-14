package com.frameout.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.frameout.designsystem.theme.FrameTheme

private val TrackWidth = 56.dp
private val TrackHeight = 4.dp

@Composable
fun FrameScrollRow(
    modifier: Modifier = Modifier,
    spacing: Dp = 10.dp,
    state: ScrollState = rememberScrollState(),
    content: @Composable RowScope.() -> Unit
) {
    var viewport by remember { mutableIntStateOf(0) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            Modifier.fillMaxWidth().onSizeChanged { viewport = it.width }.horizontalScroll(state),
            Arrangement.spacedBy(spacing),
            content = content
        )
        FrameScrollIndicator(state, viewport)
    }
}

@Composable
fun FrameScrollIndicator(state: ScrollState, viewport: Int, modifier: Modifier = Modifier) {
    val max = state.maxValue
    if (viewport <= 0 || max <= 0 || max == Int.MAX_VALUE) return
    val ink = FrameTheme.colors.onSurface
    val active by animateFloatAsState(if (state.isScrollInProgress) 1f else 0f, tween(200), label = "scrollIndicator")
    val fraction = viewport.toFloat() / (viewport + max)
    Canvas(modifier.width(TrackWidth).height(TrackHeight)) {
        val radius = CornerRadius(size.height / 2f)
        drawRoundRect(ink.copy(alpha = 0.10f), cornerRadius = radius)
        val thumb = (size.width * fraction).coerceAtLeast(size.height * 3f)
        val progress = (state.value.toFloat() / max).coerceIn(0f, 1f)
        val offset = (size.width - thumb) * progress
        val x = if (layoutDirection == LayoutDirection.Rtl) size.width - thumb - offset else offset
        drawRoundRect(ink.copy(alpha = 0.32f + 0.30f * active), Offset(x, 0f), Size(thumb, size.height), radius)
    }
}
