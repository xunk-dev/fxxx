package com.frameout.app.trim

import android.graphics.Bitmap
import com.frameout.core.quality.Sharpness
import com.frameout.engine.player.FramePtsIndex
import com.frameout.engine.player.PlayerController
import com.frameout.engine.player.frameTimesUs
import com.frameout.engine.player.grabFrame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs

internal object SmartShot {
    const val REACTION_MS = 180L
    const val MAX_FRAMES = 8
    const val MIN_GAIN = 1.08
    const val MAX_LUMA_DELTA = 22.0
    const val SCORE_EDGE = 320
    const val BUDGET_MS = 900L

    class Sample(val index: Int, val sharpness: Double, val luma: Double)

    fun candidates(frames: LongArray, tapMs: Long): List<Int> {
        if (frames.isEmpty()) return emptyList()
        val tap = FramePtsIndex.indexAt(frames, tapMs)
        val fromUs = (tapMs - REACTION_MS).coerceAtLeast(0L) * 1000L
        val out = ArrayList<Int>(MAX_FRAMES)
        var i = tap - 1
        while (i >= 0 && frames[i] >= fromUs && out.size < MAX_FRAMES) {
            out += i
            i--
        }
        return out
    }

    fun choose(tap: Sample, earlier: List<Sample>): Sample {
        val best = earlier
            .filter { abs(it.luma - tap.luma) <= MAX_LUMA_DELTA }
            .maxByOrNull { it.sharpness } ?: return tap
        return if (best.sharpness > tap.sharpness * MIN_GAIN) best else tap
    }
}

internal suspend fun PlayerController.smartShotMs(tapMs: Long): Long {
    val frames = frameTimesUs() ?: return tapMs
    val tapIndex = FramePtsIndex.indexAt(frames, tapMs)
    if (tapIndex < 0) return tapMs
    val earlier = SmartShot.candidates(frames, tapMs)
    if (earlier.isEmpty()) return tapMs
    val picked = withTimeoutOrNull(SmartShot.BUDGET_MS) {
        val tap = sampleOf(tapIndex) ?: return@withTimeoutOrNull null
        SmartShot.choose(tap, earlier.mapNotNull { sampleOf(it) })
    } ?: return tapMs
    return if (picked.index == tapIndex) tapMs else FramePtsIndex.frameMs(frames, picked.index)
}

private suspend fun PlayerController.sampleOf(index: Int): SmartShot.Sample? {
    val bitmap = runCatching { grabFrame(index) }.getOrNull() ?: return null
    return withContext(Dispatchers.Default) {
        try {
            if (bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) return@withContext null
            val scale = SmartShot.SCORE_EDGE.toFloat() / maxOf(bitmap.width, bitmap.height)
            val small = if (scale >= 1f) bitmap else Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt().coerceAtLeast(1),
                (bitmap.height * scale).toInt().coerceAtLeast(1),
                true
            )
            try {
                val luma = Sharpness.lumaOf(small)
                SmartShot.Sample(index, Sharpness.score(luma, small.width, small.height), luma.average())
            } finally {
                if (small !== bitmap) small.recycle()
            }
        } catch (e: Exception) {
            null
        } finally {
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }
}
