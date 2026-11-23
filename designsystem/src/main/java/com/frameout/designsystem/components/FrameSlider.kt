package com.frameout.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.frameout.designsystem.haptics.FrameHaptic
import com.frameout.designsystem.haptics.FrameHaptics
import com.frameout.designsystem.haptics.rememberFrameHaptics
import com.frameout.designsystem.theme.FrameTheme
import kotlin.math.abs
import kotlin.math.roundToInt
import com.frameout.designsystem.util.safeCoerceIn

private const val PAD_DP = 12
private const val HAPTIC_STEPS = 20
private const val SNAP_BAND = 0.025f

@Composable
fun FrameSlider(
    value: Float,
    onChange: (Float) -> Unit,
    min: Float,
    max: Float,
    label: String,
    modifier: Modifier = Modifier,
    neutral: Float? = null
) {
    val latestChange by rememberUpdatedState(onChange)
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val lo = if (min.isFinite()) min else 0f
    val hi = if (max.isFinite() && max >= lo) max else lo
    val span = hi - lo
    val fraction = if (span > 0f && value.isFinite()) ((value - lo) / span).safeCoerceIn(0f, 1f) else 0f
    val colors = FrameTheme.colors
    val haptics = rememberFrameHaptics()
    val anchor = neutral?.takeIf { span > 0f && it.isFinite() }?.let { ((it - lo) / span).safeCoerceIn(0f, 1f) }
    val feel = remember(haptics, anchor) { SliderFeel(haptics, anchor) }
    val onFraction: (Float) -> Unit = { latestChange((lo + feel.move(it.safeCoerceIn(0f, 1f)) * span).safeCoerceIn(lo, hi)) }
    val onReset: (() -> Unit)? = neutral?.takeIf { it.isFinite() }?.let { target -> { feel.reset(); latestChange(target.safeCoerceIn(lo, hi)) } }
    Canvas(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .semantics {
                contentDescription = label
                progressBarRangeInfo = ProgressBarRangeInfo(value.safeCoerceIn(lo, hi), lo..hi)
                setProgress { target -> latestChange(target.safeCoerceIn(lo, hi)); true }
            }
            .sliderInput(listOf(lo, hi, rtl, neutral), rtl, onFraction, onReset)
    ) { drawSlider(fraction, rtl, colors.outline, colors.primary) }
}

private class SliderFeel(private val haptics: FrameHaptics, private val anchor: Float?) {
    private var lastStep = -1
    private var lastFraction = -1f

    fun move(raw: Float): Float {
        val f = if (anchor != null && abs(raw - anchor) <= SNAP_BAND) anchor else raw
        val step = (f * HAPTIC_STEPS).roundToInt()
        val enteredAnchor = anchor != null && f == anchor && lastFraction != f
        if (enteredAnchor) {
            lastStep = step
            haptics.perform(FrameHaptic.Confirm)
        } else if (step != lastStep) {
            lastStep = step
            haptics.perform(if (step == 0 || step == HAPTIC_STEPS) FrameHaptic.Select else FrameHaptic.Tick)
        }
        lastFraction = f
        return f
    }

    fun reset() {
        haptics.perform(FrameHaptic.Confirm)
        lastFraction = anchor ?: -1f
    }
}

private fun toFraction(x: Float, width: Int, pad: Float, rtl: Boolean): Float {
    val usable = width - pad * 2f
    if (usable <= 0f || !x.isFinite()) return 0f
    val raw = ((x - pad) / usable).coerceIn(0f, 1f)
    return if (rtl) 1f - raw else raw
}

private fun Modifier.sliderInput(key: Any, rtl: Boolean, onFraction: (Float) -> Unit, onReset: (() -> Unit)?): Modifier = this
    .pointerInput(key) {
        val pad = PAD_DP.dp.toPx()
        detectTapGestures(
            onDoubleTap = onReset?.let { reset -> { _: Offset -> reset() } },
            onTap = { onFraction(toFraction(it.x, size.width, pad, rtl)) }
        )
    }
    .pointerInput(key) {
        val pad = PAD_DP.dp.toPx()
        detectHorizontalDragGestures(
            onDragStart = { onFraction(toFraction(it.x, size.width, pad, rtl)) },
            onHorizontalDrag = { change, _ ->
                change.consume()
                onFraction(toFraction(change.position.x, size.width, pad, rtl))
            }
        )
    }

private fun DrawScope.drawSlider(fraction: Float, rtl: Boolean, track: Color, fill: Color) {
    val pad = PAD_DP.dp.toPx()
    val usable = size.width - pad * 2f
    val start = pad + if (rtl) usable else 0f
    val thumb = pad + usable * (if (rtl) 1f - fraction else fraction)
    val cy = size.height / 2f
    val stroke = 4.dp.toPx()
    drawLine(track, Offset(pad, cy), Offset(size.width - pad, cy), stroke, StrokeCap.Round)
    drawLine(fill, Offset(start, cy), Offset(thumb, cy), stroke, StrokeCap.Round)
    drawCircle(fill, 10.dp.toPx(), Offset(thumb, cy))
}
