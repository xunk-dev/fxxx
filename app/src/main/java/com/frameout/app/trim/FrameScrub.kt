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
import androidx.compose.runtime.DisposableEffect
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import com.frameout.engine.player.frameCount
import com.frameout.engine.player.frameIndexNow
import com.frameout.engine.player.scrubToFrame
import com.frameout.core.util.safeCoerceIn

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
    val glider = remember(controller) { ScrubGlider(scope) }
    DisposableEffect(glider) { onDispose { glider.stop() } }
    return pointerInput(controller, state) {
        awaitEachGesture { scrubGesture(controller, state, haptics, scope, glider) { isZoomed() } }
    }
}

private class ScrubSession(
    private val controller: PlayerController,
    private val state: FrameScrubState,
    private val haptics: FrameHaptics,
    private val scope: CoroutineScope
) {
    val motion = ScrubMotion(controller.frameCount(), controller.frameIndexNow())
    private var applied = 0
    var gliding = false
        private set

    fun begin() {
        state.frames = 0
        state.active = true
        state.markUsed()
        controller.pause()
        controller.scrub(true)
        haptics.perform(FrameHaptic.DragStart)
    }

    fun show(offset: Int) {
        val delta = offset - applied
        if (delta == 0) return
        applied = offset
        state.frames = offset
        if (abs(delta) == 1) haptics.perform(FrameHaptic.Tick)
        if (motion.indexed) controller.scrubToFrame(motion.frame)
        else scope.launch { controller.stepFrame(delta > 0, abs(delta)) }
    }

    fun end() {
        state.active = false
        controller.scrub(false)
        haptics.perform(if (motion.atEdge(motion.frame)) FrameHaptic.Reject else FrameHaptic.DragEnd)
    }

    fun glide(glider: ScrubGlider, plan: ScrubGlide) {
        gliding = true
        glider.start(
            plan,
            { position ->
                show(motion.offsetOf(position.roundToInt().coerceAtLeast(0)))
                controller.isScrubbing
            },
            { end() }
        )
    }
}

private suspend fun AwaitPointerEventScope.scrubGesture(
    controller: PlayerController,
    state: FrameScrubState,
    haptics: FrameHaptics,
    scope: CoroutineScope,
    glider: ScrubGlider,
    zoomed: () -> Boolean
) {
    val down = awaitFirstDown(requireUnconsumed = false)
    glider.stop()
    if (zoomed()) return
    var overSlopX = 0f
    awaitHorizontalTouchSlopOrCancellation(down.id) { change, overSlop ->
        change.consume()
        overSlopX = overSlop
    } ?: return
    val session = ScrubSession(controller, state, haptics, scope)
    session.begin()
    var lastT = down.uptimeMillis
    try {
        session.show(session.motion.move(lastT, down.position.x + overSlopX, overSlopX, density))
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed || event.changes.count { it.pressed } > 1) break
            lastT = change.uptimeMillis
            session.show(session.motion.move(lastT, change.position.x, change.positionChange().x, density))
            change.consume()
        }
        session.motion.release(lastT, density)?.let { session.glide(glider, it) }
    } finally {
        if (!session.gliding) session.end()
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
