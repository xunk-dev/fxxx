package com.frameout.app.billing

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.frameout.core.pro.ProStrings
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.haptics.FrameHaptic
import com.frameout.designsystem.haptics.rememberFrameHaptics
import com.frameout.designsystem.theme.FrameTheme
import kotlinx.coroutines.delay

private const val INTRO_MS = 3200
private const val PULSE_MS = 1600
private const val SPIN_MS = 14000

@Composable
fun ProUnlockedDialog(onDone: () -> Unit) {
    Dialog(onDismissRequest = onDone, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        RetintedTheme(null) { ProUnlockedContent(onDone) }
    }
}

@Composable
private fun ProUnlockedContent(onDone: () -> Unit) {
    val accent = FrameTheme.colors.accentA
    val reduce = LocalSystemA11y.current.reduceMotion
    val intro = rememberIntro(reduce)
    val pulse = rememberLoop(reduce, PULSE_MS, RepeatMode.Reverse, 0.5f)
    val spin = rememberLoop(reduce, SPIN_MS, RepeatMode.Restart, 0f)
    var anchor by remember { mutableStateOf(Offset.Zero) }
    UnlockHaptics(reduce)
    Box(Modifier.fillMaxSize().drawBehind { drawScrim(anchor, accent) }) {
        Canvas(Modifier.fillMaxSize()) { drawCelebration(anchor, intro.value, pulse.value, spin.value, accent) }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxWidth().heightIn(min = maxHeight).verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 24.dp),
                Arrangement.Center,
                Alignment.CenterHorizontally
            ) {
                Medallion(intro, pulse, Modifier.onGloballyPositioned { anchor = it.positionInRoot() + Offset(it.size.width / 2f, it.size.height / 2f) })
                UnlockCopy(intro)
                DonePerks(intro)
                DoneCta(Modifier.padding(top = 26.dp).revealAt(intro, 0.80f, 0.95f), intro.value >= 0.85f, onDone)
            }
        }
    }
}

@Composable
private fun UnlockCopy(intro: State<Float>) {
    val shimmer = rememberShimmer(3600, 1800)
    val title = FrameTheme.typography.display.copy(
        brush = Brush.horizontalGradient(listOf(ProGold.Light, Color.White, ProGold.Light, ProGold.Deep)),
        fontSize = 34.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
    Column(
        Modifier.padding(top = 28.dp, bottom = 22.dp).semantics(mergeDescendants = true) { contentDescription = ProStrings.doneTitle() + ". " + ProStrings.doneBody() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BasicText(ProStrings.doneTitle(), Modifier.revealAt(intro, 0.30f, 0.50f).glint(shimmer, Color.White).semantics { heading() }, title)
        FrameText(ProStrings.doneBody(), FrameTheme.typography.body, Color.White.copy(alpha = 0.78f), Modifier.padding(top = 8.dp).revealAt(intro, 0.38f, 0.58f), TextAlign.Center)
    }
}

@Composable
private fun UnlockHaptics(reduce: Boolean) {
    val haptics = rememberFrameHaptics()
    LaunchedEffect(Unit) {
        haptics.perform(FrameHaptic.Confirm)
        if (reduce) return@LaunchedEffect
        delay(280)
        haptics.perform(FrameHaptic.Tick)
        delay(220)
        haptics.perform(FrameHaptic.Tick)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawScrim(anchor: Offset, accent: Color) {
    val base = Color(0xFC050608)
    val center = if (anchor == Offset.Zero) Offset(size.width / 2f, size.height * 0.3f) else anchor
    drawRect(base)
    drawRect(Brush.radialGradient(listOf(Color(0xFF3A2A08).copy(alpha = 0.55f), accent.copy(alpha = 0.10f), Color.Transparent), center, size.maxDimension * 0.8f))
}

@Composable
private fun rememberIntro(reduce: Boolean): State<Float> {
    val t = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(reduce) { if (!reduce) t.animateTo(1f, tween(INTRO_MS, easing = LinearEasing)) }
    return t.asState()
}

@Composable
private fun rememberLoop(reduce: Boolean, periodMs: Int, mode: RepeatMode, still: Float): State<Float> {
    if (reduce) return remember { mutableFloatStateOf(still) }
    val transition = rememberInfiniteTransition(label = "celebrationLoop")
    return transition.animateFloat(0f, 1f, infiniteRepeatable(tween(periodMs, easing = LinearEasing), mode), label = "celebrationLoopValue")
}
