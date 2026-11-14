package com.frameout.app.trim

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.text.style.TextAlign
import com.frameout.core.prefs.AppPrefs
import com.frameout.core.util.TrimStrings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.haptics.FrameHaptic
import com.frameout.designsystem.haptics.FrameHaptics
import com.frameout.designsystem.haptics.rememberFrameHaptics
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameControl
import com.frameout.engine.player.PlayerController
import com.frameout.engine.timeline.formatTimestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

private val FRAME_STEP = 12.dp

@Stable
internal class FrameScrubState {
    var active by mutableStateOf(false)
    var frames by mutableIntStateOf(0)
    var hintVisible by mutableStateOf(false)
    var onFirstUse: (() -> Unit)? = null
    private var used = false

    fun markUsed() {
        hintVisible = false
        if (used) return
        used = true
        onFirstUse?.invoke()
    }
}

@Composable
internal fun Modifier.frameScrubDrag(controller: PlayerController, state: FrameScrubState, zoomed: () -> Boolean): Modifier {
    val haptics = rememberFrameHaptics()
    val scope = rememberCoroutineScope()
    val isZoomed by rememberUpdatedState(zoomed)
    return pointerInput(controller, state) {
        awaitEachGesture { scrubGesture(controller, state, haptics, scope) { isZoomed() } }
    }
}

private suspend fun AwaitPointerEventScope.scrubGesture(
    controller: PlayerController,
    state: FrameScrubState,
    haptics: FrameHaptics,
    scope: CoroutineScope,
    zoomed: () -> Boolean
) {
    val down = awaitFirstDown(requireUnconsumed = false)
    if (zoomed()) return
    var total = 0f
    awaitHorizontalTouchSlopOrCancellation(down.id) { change, overSlop ->
        change.consume()
        total = overSlop
    } ?: return
    val stepPx = FRAME_STEP.toPx()
    var applied = 0
    fun follow() {
        val target = (total / stepPx).toInt()
        val delta = target - applied
        if (delta == 0) return
        applied = target
        state.frames = target
        haptics.perform(FrameHaptic.Tick)
        scope.launch { controller.stepFrame(delta > 0, abs(delta)) }
    }
    state.frames = 0
    state.active = true
    state.markUsed()
    controller.pause()
    controller.scrub(true)
    haptics.perform(FrameHaptic.DragStart)
    try {
        follow()
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed || event.changes.count { it.pressed } > 1) break
            total += change.positionChange().x
            change.consume()
            follow()
        }
    } finally {
        state.active = false
        controller.scrub(false)
        haptics.perform(FrameHaptic.DragEnd)
    }
}

@Composable
internal fun BoxScope.FrameScrubBadge(state: FrameScrubState, controller: PlayerController) {
    val position by controller.position.collectAsState()
    AnimatedVisibility(
        state.active,
        Modifier.align(Alignment.TopCenter).padding(top = 10.dp),
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        val frames = state.frames
        val signed = when {
            frames > 0 -> "+$frames"
            frames < 0 -> "\u2212${-frames}"
            else -> "0"
        }
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            FrameText(
                "$signed  \u00B7  ${formatTimestamp(position)}",
                style = FrameTheme.typography.label,
                color = FrameTheme.colors.onSurface,
                maxLines = 1,
                modifier = Modifier.frameControl(FrameShapes.pill, selected = true, glowAlpha = 0.4f).padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

private const val SCRUB_HINT_VISIBLE_MS = 4500L

@Composable
internal fun rememberScrubHint(prefs: AppPrefs, speedZones: SpeedZoneState? = null): FrameScrubState {
    val state = remember(prefs) { FrameScrubState().also { it.onFirstUse = { prefs.markScrubUsed() } } }
    LaunchedEffect(state) {
        if (prefs.scrubHintShows() >= AppPrefs.SCRUB_HINT_LIMIT) return@LaunchedEffect
        if (speedZones != null && speedZones.hintVisible) snapshotFlow { speedZones.hintVisible }.first { !it }
        delay(400)
        if (state.active) return@LaunchedEffect
        prefs.markScrubHintShown()
        state.hintVisible = true
        delay(SCRUB_HINT_VISIBLE_MS)
        state.hintVisible = false
    }
    return state
}

@Composable
internal fun BoxScope.FrameScrubHint(state: FrameScrubState, atTop: Boolean = false) {
    val colors = FrameTheme.colors
    val transition = rememberInfiniteTransition(label = "scrubHint")
    val shift by transition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "scrubHintShift"
    )
    AnimatedVisibility(
        state.hintVisible && !state.active,
        if (atTop) Modifier.align(Alignment.TopCenter).padding(top = 10.dp) else Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp),
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        val description = TrimStrings.scrubHint()
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                Modifier
                    .frameControl(FrameShapes.pill, selected = true, glowAlpha = 0.4f)
                    .semantics { contentDescription = description }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FrameText("\u2039", style = FrameTheme.typography.title, color = colors.primary, modifier = Modifier.offset(x = shift.dp))
                androidx.compose.foundation.layout.Box(
                    Modifier
                        .size(8.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(colors.primary)
                )
                FrameText("\u203A", style = FrameTheme.typography.title, color = colors.primary, modifier = Modifier.offset(x = (-shift).dp))
            }
        }
    }
}
