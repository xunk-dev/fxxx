package com.frameout.engine.player

import android.graphics.Bitmap

fun PlayerController.frameCount(): Int = ptsIndex?.size ?: 0

fun PlayerController.frameTimesUs(): LongArray? = ptsIndex?.takeIf { it.isNotEmpty() }

fun PlayerController.frameNumberAt(positionMs: Long): Int {
    val index = ptsIndex ?: return 0
    return FramePtsIndex.indexAt(index, positionMs) + 1
}

fun PlayerController.snapMs(positionMs: Long): Long {
    val index = ptsIndex ?: return positionMs
    return FramePtsIndex.snapMs(index, positionMs)
}

fun PlayerController.shiftFrames(positionMs: Long, frames: Int, fallbackStepMs: Long): Long {
    val index = ptsIndex?.takeIf { it.isNotEmpty() } ?: return positionMs + frames * fallbackStepMs
    return FramePtsIndex.shiftMs(index, positionMs, frames)
}

fun PlayerController.frameIndexNow(): Int {
    val index = ptsIndex ?: return -1
    return FramePtsIndex.indexAt(index, positionNow())
}

fun PlayerController.scrubToFrame(frame: Int) {
    val index = ptsIndex?.takeIf { it.isNotEmpty() } ?: return
    scrubTo(FramePtsIndex.frameMs(index, frame))
}

suspend fun PlayerController.grabFrame(frame: Int): Bitmap? {
    val index = ptsIndex?.takeIf { it.isNotEmpty() } ?: return null
    if (frame !in index.indices) return null
    return grabber?.grab(index[frame] / 1000L, index[frame])
}
