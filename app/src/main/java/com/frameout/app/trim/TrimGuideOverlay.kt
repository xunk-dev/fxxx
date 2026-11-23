package com.frameout.app.trim

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateRectAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.TrimStrings
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.motion.frameSpring
import com.frameout.designsystem.theme.ContrastGuard
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme

private val HolePad = 6.dp
private val HoleRadius = 18.dp
private val CardGap = 14.dp

@Composable
internal fun TrimGuideOverlay(guide: TrimGuideState) {
    BackHandler(enabled = guide.visible) { guide.finish() }
    val steps = guide.steps()
    var origin by remember { mutableStateOf(Offset.Zero) }
    val block = if (steps.isEmpty()) Modifier else Modifier.pointerInput(Unit) { detectTapGestures { } }
    BoxWithConstraints(
        Modifier.fillMaxSize().onGloballyPositioned { origin = it.positionInWindow() }.then(block)
    ) {
        if (steps.isEmpty()) return@BoxWithConstraints
        val position = guide.index.coerceAtMost(steps.lastIndex)
        val step = steps[position]
        val raw = guide.boundsOf(step)?.translate(-origin) ?: return@BoxWithConstraints
        val pad = with(LocalDensity.current) { HolePad.toPx() }
        val hole by animateRectAsState(raw.inflate(pad), holeSpec(), label = "guideHole")
        GuideDim(hole)
        GuideCardHost(step, position, steps.size, hole, constraints.maxHeight.toFloat(), guide)
    }
}

@Composable
private fun holeSpec() = if (LocalSystemA11y.current.reduceMotion) snap<Rect>() else frameSpring<Rect>()

@Composable
private fun GuideDim(hole: Rect) {
    val dim = FrameTheme.colors.background.copy(alpha = 0.86f)
    val ring = FrameTheme.colors.primary
    val radius = with(LocalDensity.current) { HoleRadius.toPx() }
    Canvas(Modifier.fillMaxSize().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
        drawRect(dim)
        drawRoundRect(Color.Black, hole.topLeft, hole.size, CornerRadius(radius), blendMode = BlendMode.Clear)
        drawRoundRect(ring, hole.topLeft, hole.size, CornerRadius(radius), style = Stroke(2.dp.toPx()))
    }
}

@Composable
private fun GuideCardHost(step: TrimGuideStep, position: Int, total: Int, hole: Rect, height: Float, guide: TrimGuideState) {
    val density = LocalDensity.current
    val above = hole.center.y > height / 2f
    val edge = with(density) { if (above) (height - hole.top).toDp() else hole.bottom.toDp() }
    val gap = (edge + CardGap).coerceAtLeast(CardGap)
    val spot = if (above) Modifier.padding(bottom = gap) else Modifier.padding(top = gap)
    Column(
        Modifier.fillMaxSize().then(spot),
        if (above) Arrangement.Bottom else Arrangement.Top,
        Alignment.CenterHorizontally
    ) {
        GuideCard(step, position, total, guide)
    }
}

@Composable
private fun GuideCard(step: TrimGuideStep, position: Int, total: Int, guide: TrimGuideState) {
    val colors = FrameTheme.colors
    val shape = FrameShapes.lg
    val last = position >= total - 1
    Column(
        Modifier
            .padding(horizontal = 20.dp)
            .widthIn(max = 420.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.primary.copy(alpha = 0.5f), shape)
            .padding(18.dp),
        Arrangement.spacedBy(8.dp)
    ) {
        GuideProgress(position, total)
        FrameText(step.title(), FrameTheme.typography.title, colors.onSurface)
        FrameText(step.body(), FrameTheme.typography.body, colors.onSurfaceVariant)
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            if (last) Box(Modifier.size(1.dp)) else GuideButton(FrameStrings.onboardingSkip(), false, guide::finish)
            GuideButton(if (last) FrameStrings.done() else FrameStrings.onboardingNext(), true, guide::next)
        }
    }
}

@Composable
private fun GuideProgress(position: Int, total: Int) {
    val colors = FrameTheme.colors
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        FrameText(TrimStrings.guideStep(position + 1, total), FrameTheme.typography.label, colors.primary)
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
            repeat(total) { i ->
                val on = i == position
                Box(
                    Modifier
                        .size(width = if (on) 16.dp else 6.dp, height = 6.dp)
                        .clip(CircleShape)
                        .background(if (on) colors.primary else colors.onSurfaceVariant.copy(alpha = 0.35f))
                )
            }
        }
    }
}

@Composable
private fun GuideButton(label: String, filled: Boolean, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    val ink = if (filled) ContrastGuard.readableOn(colors.primary, listOf(colors.onPrimary, Color.White, Color.Black)) else colors.onSurfaceVariant
    Box(
        Modifier
            .heightIn(min = 44.dp)
            .clip(FrameShapes.pill)
            .background(if (filled) colors.primary else Color.Transparent)
            .frameClickable(onClick = onClick)
            .padding(horizontal = 22.dp),
        Alignment.Center
    ) {
        FrameText(label, FrameTheme.typography.label, ink)
    }
}
