package com.frameout.app.trim

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.frameout.core.util.FrameA11y
import com.frameout.core.util.TrimStrings
import com.frameout.designsystem.haptics.FrameHaptic
import com.frameout.designsystem.haptics.rememberFrameHaptics
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.fieldFill
import com.frameout.designsystem.theme.frameHairline
import com.frameout.engine.timeline.TrimRange
import kotlin.math.ceil
import kotlin.math.floor

private val RulerHeight = 52.dp
private val TickGap = 9.dp
private const val MAJOR_EVERY = 5

internal fun pinnedTime(picked: Handle, range: TrimRange): Long? = when (picked) {
    Handle.Start -> range.startMs
    Handle.End -> range.endMs
    Handle.None -> null
}

private class JogSession(private val gapPx: Float) {
    private var acc = 0f
    private var cursor = 0L

    fun begin(env: TrimEnv) {
        acc = 0f
        cursor = pinnedTime(env.picked, env.clamped) ?: env.controller.positionNow()
    }

    fun steps(dx: Float): Int {
        acc += dx
        val whole = (acc / gapPx).toInt()
        acc -= whole * gapPx
        return -whole
    }

    fun jog(env: TrimEnv, frames: Int): FrameHaptic {
        val before = cursor
        cursor = env.jog(env.picked, before, frames)
        return if (cursor / 1000L != before / 1000L) FrameHaptic.Select else FrameHaptic.Tick
    }
}

@Composable
internal fun JogRuler(env: TrimEnv, modifier: Modifier = Modifier) {
    val colors = FrameTheme.colors
    val latest by rememberUpdatedState(env)
    val gap = with(LocalDensity.current) { TickGap.toPx() }
    val session = remember(gap) { JogSession(gap) }
    var phase by remember { mutableFloatStateOf(0f) }
    val marker = if (env.picked == Handle.None) Color.White else colors.primary
    val tick = colors.onSurfaceVariant
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier
                .fillMaxWidth()
                .height(RulerHeight)
                .clip(FrameShapes.md)
                .background(colors.fieldFill)
                .border(1.dp, frameHairline(), FrameShapes.md)
                .jogSemantics(session) { latest }
                .jogDrag(session, gap, { latest }) { dx -> phase = (phase + dx) % (gap * MAJOR_EVERY) }
        ) {
            Canvas(Modifier.fillMaxSize()) { drawRuler(phase, gap, marker, tick) }
        }
    }
}

private fun Modifier.jogSemantics(session: JogSession, env: () -> TrimEnv): Modifier {
    val actions = listOf(-1 to TrimStrings.nudgeEarlier(), 1 to TrimStrings.nudgeLater()).map { (frames, label) ->
        CustomAccessibilityAction(label) {
            session.begin(env())
            session.jog(env(), frames)
            true
        }
    }
    return semantics {
        contentDescription = FrameA11y.timeline()
        customActions = actions
    }
}

@Composable
private fun Modifier.jogDrag(session: JogSession, gap: Float, env: () -> TrimEnv, onPhase: (Float) -> Unit): Modifier {
    val haptics = rememberFrameHaptics()
    return pointerInput(gap) {
        detectHorizontalDragGestures(
            onDragStart = {
                session.begin(env())
                env().controller.scrub(true)
            },
            onDragEnd = { env().controller.scrub(false) },
            onDragCancel = { env().controller.scrub(false) }
        ) { change, dx ->
            change.consume()
            onPhase(dx)
            val frames = session.steps(dx)
            if (frames != 0) haptics.perform(session.jog(env(), frames))
        }
    }
}

private fun DrawScope.drawRuler(phase: Float, gap: Float, marker: Color, tick: Color) {
    val mid = size.width / 2f
    val first = floor((-mid - phase) / gap).toInt()
    val last = ceil((size.width - mid - phase) / gap).toInt()
    for (i in first..last) {
        val major = i.mod(MAJOR_EVERY) == 0
        val x = mid + phase + i * gap
        val inset = (if (major) 10.dp else 16.dp).toPx()
        drawLine(
            tick.copy(alpha = if (major) 0.9f else 0.45f),
            Offset(x, inset),
            Offset(x, size.height - inset),
            (if (major) 1.5.dp else 1.dp).toPx()
        )
    }
    drawLine(marker, Offset(mid, 4.dp.toPx()), Offset(mid, size.height - 4.dp.toPx()), 2.dp.toPx())
}
