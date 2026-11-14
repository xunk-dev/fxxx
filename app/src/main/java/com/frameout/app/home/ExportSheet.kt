package com.frameout.app.home

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.frameout.app.frames.FramesSheet
import com.frameout.core.util.HomeStrings
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.haptics.FrameHaptic
import com.frameout.designsystem.haptics.rememberFrameHaptics
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameCardEdge

@Composable
internal fun ExportStyleSheet(visible: Boolean, initialTab: Int, onClose: () -> Unit, onPick: (ExportStyle) -> Unit) {
    val reduce = LocalSystemA11y.current.reduceMotion
    val videoMode = initialTab == 0
    val title = if (videoMode) HomeStrings.videoTitle() else HomeStrings.imagesTitle()
    val styles = if (videoMode) ExportStyle.VideoStyles else ExportStyle.ImageStyles
    FramesSheet(visible, title, onClose) {
        val phase = rememberPreviewPhase(reduce)
        Column(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
            styles.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
                    pair.forEach { style -> ExportCard(style, phase, Modifier.weight(1f)) { onPick(style) } }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ExportCard(style: ExportStyle, phase: State<Float>, modifier: Modifier, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    val reduce = LocalSystemA11y.current.reduceMotion
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && !reduce) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "exportCardPress"
    )
    val haptics = rememberFrameHaptics()
    Column(
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(FrameShapes.xl)
            .background(colors.containerHigh)
            .border(1.dp, frameCardEdge(colors.primary, 0.30f), FrameShapes.xl)
            .clickable(interactionSource = source, indication = null, role = Role.Button) {
                haptics.perform(FrameHaptic.Tap)
                onClick()
            }
    ) {
        ExportPreview(style, phase, Modifier.fillMaxWidth().aspectRatio(1.35f))
        ExportCardLabel(style)
    }
}

@Composable
private fun ExportCardLabel(style: ExportStyle) {
    val colors = FrameTheme.colors
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), Arrangement.spacedBy(3.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Image(painterResource(style.icon), contentDescription = null, modifier = Modifier.size(18.dp), colorFilter = ColorFilter.tint(colors.primary))
            FrameText(style.title(), style = FrameTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        FrameText(
            style.description(),
            style = FrameTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
