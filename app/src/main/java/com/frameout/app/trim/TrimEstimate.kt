package com.frameout.app.trim

import com.frameout.engine.extract.ExtractMode
import com.frameout.engine.extract.ExtractModeCodec
import com.frameout.engine.player.PlayerUiState
import com.frameout.engine.timeline.TrimRange

private const val FRAME_MS = 33L
private const val SCENE_EVERY_MS = 2_500L
private const val SLIDE_EVERY_MS = 10_000L

internal fun estimateFrames(durationMs: Long, range: TrimRange, mode: String): Int =
    estimateFrames(if (range.useFull) durationMs else range.spanMs(), ExtractModeCodec.decode(mode))

internal fun estimateFrames(durationMs: Long, range: TrimRange, mode: String, probe: SourceProbe?): Int {
    val decoded = ExtractModeCodec.decode(mode)
    val start = if (range.useFull) 0L else range.startMs
    val end = if (range.useFull) durationMs else range.endMs
    probe?.timeline?.count(decoded, start, end)?.let { return it.coerceAtLeast(1) }
    val span = if (range.useFull) durationMs else range.spanMs()
    return estimateFrames(span, decoded, probe?.fps?.takeIf { it > 1f })
}

internal fun estimateFrames(spanMs: Long, mode: ExtractMode, fps: Float? = null): Int {
    val ms = spanMs.coerceAtLeast(1L)
    val allFrames = (if (fps != null) Math.round(ms * fps / 1000f).toLong() else ms / FRAME_MS).coerceAtLeast(1L)
    return when (mode) {
        ExtractMode.EveryFrame -> allFrames
        is ExtractMode.EveryNFrames -> (allFrames + mode.n.coerceAtLeast(1) - 1L) / mode.n.coerceAtLeast(1)
        is ExtractMode.EveryNMillis -> (ms + mode.millis.coerceAtLeast(1) - 1L) / mode.millis.coerceAtLeast(1)
        ExtractMode.KeyframesOnly -> ms / 1000L
        is ExtractMode.FixedCount -> minOf(mode.count.toLong(), allFrames)
        ExtractMode.SceneChange -> ms / SCENE_EVERY_MS
        ExtractMode.Slides -> ms / SLIDE_EVERY_MS
        is ExtractMode.Content -> (ms + com.frameout.core.quality.content.ContentSpec.SAMPLE_STEP_MS - 1L) / com.frameout.core.quality.content.ContentSpec.SAMPLE_STEP_MS
    }.coerceAtLeast(1L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
}

internal fun estimateMb(durationMs: Long, range: TrimRange, mode: String, state: PlayerUiState, probe: SourceProbe? = null): Float {
    val frames = estimateFrames(durationMs, range, mode, probe)
    val basis = if (state.durationMs > 0) 0.18f else 0.15f
    return frames * basis
}

internal fun isExactEstimate(mode: ExtractMode): Boolean = when (mode) {
    ExtractMode.SceneChange, ExtractMode.Slides, is ExtractMode.Content -> false
    else -> true
}
