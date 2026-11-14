package com.frameout.app.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.haptics.FrameHaptic
import com.frameout.designsystem.haptics.rememberFrameHaptics
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameCardEdge
import kotlinx.coroutines.delay

private const val STAGGER_MS = 55L
private const val ENTER_MS = 420

@Composable
internal fun HomeEntrance(index: Int, active: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val reduce = LocalSystemA11y.current.reduceMotion
    var shown by remember { mutableStateOf(reduce) }
    LaunchedEffect(active, reduce) {
        if (!active) {
            if (!reduce) shown = false
            return@LaunchedEffect
        }
        if (!reduce) delay(index * STAGGER_MS)
        shown = true
    }
    val t by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(if (reduce) 0 else ENTER_MS, easing = FastOutSlowInEasing),
        label = "homeEnter"
    )
    Box(
        modifier.graphicsLayer {
            alpha = t
            translationY = (1f - t) * 28.dp.toPx()
            val scale = 0.96f + 0.04f * t
            scaleX = scale
            scaleY = scale
        }
    ) { content() }
}

@Composable
internal fun Modifier.homePressable(onClick: (() -> Unit)?): Modifier {
    val reduce = LocalSystemA11y.current.reduceMotion
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && !reduce) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "homePress"
    )
    val haptics = rememberFrameHaptics()
    val clickable = if (onClick == null) Modifier else Modifier.clickable(
        interactionSource = source,
        indication = null,
        role = Role.Button,
        onClick = {
            haptics.perform(FrameHaptic.Tap)
            onClick()
        }
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }.then(clickable)
}

@Composable
internal fun HomeCard(
    title: String,
    description: String,
    asset: String,
    fallbackIcon: Int,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onClick: (() -> Unit)? = null,
    actions: (@Composable () -> Unit)? = null
) {
    val colors = FrameTheme.colors
    Box(
        modifier
            .fillMaxSize()
            .homePressable(onClick)
            .clip(FrameShapes.xxl)
            .background(glassStageOrNull() ?: Brush.linearGradient(listOf(colors.containerHigh, colors.containerHigh)))
            .border(1.dp, frameCardEdge(colors.primary, 0.35f), FrameShapes.xxl)
    ) {
        HomeCardContent(title, description, asset, fallbackIcon, compact, actions)
    }
}

@Composable
private fun BoxScope.HomeCardContent(
    title: String,
    description: String,
    asset: String,
    fallbackIcon: Int,
    compact: Boolean,
    actions: (@Composable () -> Unit)?
) {
    if (compact) {
        AssetSlot(asset, fallbackIcon, Modifier.align(Alignment.TopCenter).fillMaxWidth().fillMaxHeight(0.66f).padding(top = 10.dp), contentScale = ContentScale.Fit)
    } else {
        AssetSlotFill(asset, fallbackIcon)
    }
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                0.30f to Color.Transparent,
                0.55f to Color.Black.copy(alpha = 0.62f),
                1f to Color.Black.copy(alpha = 0.88f)
            )
        )
    )
    Column(
        Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(if (compact) 12.dp else 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FitText(title, style = FrameTheme.typography.title, color = Color.White, minScale = 0.75f)
        FrameText(description, style = FrameTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.82f), maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (actions != null) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) { actions() }
        }
    }
}

@Composable
internal fun HomeModeCard(
    title: String,
    description: String,
    icon: @Composable (Modifier) -> Unit,
    actionLabel: String,
    highlighted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = homeAccent()
    val onAccent = homeOnAccent()
    val base = FrameTheme.accent.darkContainer
    val surface = if (highlighted) {
        Brush.linearGradient(listOf(lerp(base, Color.Black, 0.35f), lerp(base, Color.Black, 0.8f)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFF191A1F), Color(0xFF0E0F12)))
    }
    val edge = if (highlighted) accent.copy(alpha = 0.5f) else HomeGold.copy(alpha = 0.4f)
    Column(
        modifier
            .fillMaxHeight()
            .homePressable(onClick)
            .then(if (highlighted) Modifier.shadow(10.dp, FrameShapes.xxl, ambientColor = accent.copy(alpha = 0.4f), spotColor = accent) else Modifier)
            .clip(FrameShapes.xxl)
            .background(surface)
            .then(
                if (highlighted) Modifier.drawBehind {
                    drawRect(Brush.radialGradient(listOf(accent.copy(alpha = 0.34f), Color.Transparent), Offset(size.width * 0.5f, 0f), size.width * 0.95f))
                } else Modifier
            )
            .border(1.5.dp, edge, FrameShapes.xxl)
            .semantics(mergeDescendants = true) { contentDescription = "$title. $description. $actionLabel" }
            .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 12.dp)
    ) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            icon(Modifier.align(Alignment.TopStart).heightIn(max = 74.dp).fillMaxHeight().aspectRatio(1.25f, matchHeightConstraintsFirst = true))
        }
        FrameText(title, style = FrameTheme.typography.title, color = Color.White)
        FrameText(description, style = FrameTheme.typography.label, color = Color.White.copy(alpha = 0.78f))
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 34.dp)
                .then(if (highlighted) Modifier.background(accent, FrameShapes.pill) else Modifier.border(1.5.dp, HomeGold, FrameShapes.pill))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            Alignment.Center
        ) {
            FrameText(actionLabel, style = FrameTheme.typography.label, color = if (highlighted) onAccent else HomeGold, textAlign = TextAlign.Center)
        }
    }
}
