package com.frameout.app.trim

import com.frameout.core.util.safeCoerceIn
import com.frameout.engine.player.shiftFrames
import com.frameout.engine.player.snapMs

internal fun TrimEnv.moveEdge(handle: Handle, ms: Long, fast: Boolean): Long {
    val duration = player.durationMs
    if (duration <= 0L || handle == Handle.None) return controller.positionNow()
    val target = controller.snapMs(ms)
    val next = if (handle == Handle.Start) {
        TrimLimits.moveStart(clamped, target, duration, output.maxMs)
    } else {
        TrimLimits.moveEnd(clamped, target, duration, output.maxMs)
    }
    onRange(next)
    val at = if (handle == Handle.Start) next.startMs else next.endMs
    if (fast) controller.scrubTo(at) else controller.seekTo(at)
    return at
}

internal fun TrimEnv.setLength(lengthMs: Long) {
    val duration = player.durationMs
    if (duration <= 0L) return
    val next = RangeEdit.withLength(clamped, duration, output.maxMs, lengthMs)
    onRange(next)
    val at = controller.positionNow()
    if (at < next.startMs || at > next.endMs) controller.seekTo(next.startMs)
}

internal fun TrimEnv.jog(handle: Handle, from: Long, frames: Int): Long {
    val moved = controller.shiftFrames(from, frames, RangeEdit.NUDGE_MS)
    if (handle != Handle.None) return moveEdge(handle, moved, true)
    val at = moved.safeCoerceIn(clamped.startMs, clamped.endMs)
    controller.scrubTo(at)
    return at
}

internal fun nextPick(current: Handle, handle: Handle, tap: Boolean): Handle = when {
    handle == Handle.None -> Handle.None
    tap && current == handle -> Handle.None
    else -> handle
}
