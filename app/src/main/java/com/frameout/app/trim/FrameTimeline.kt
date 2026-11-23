package com.frameout.app.trim

import com.frameout.core.quality.content.ContentSpec
import com.frameout.engine.extract.ExtractMode

class FrameTimeline private constructor(private val ptsUs: LongArray, private val keyUs: LongArray) {
    val size: Int get() = ptsUs.size

    fun countIn(startMs: Long, endMs: Long): Int {
        val (from, to) = bounds(startMs, endMs)
        return lowerBound(ptsUs, to) - lowerBound(ptsUs, from)
    }

    fun keyframesIn(startMs: Long, endMs: Long): Int {
        val (from, to) = bounds(startMs, endMs)
        return lowerBound(keyUs, to) - lowerBound(keyUs, from)
    }

    fun everyNth(startMs: Long, endMs: Long, n: Int): Int {
        val step = n.coerceAtLeast(1)
        return (countIn(startMs, endMs) + step - 1) / step
    }

    fun onTimeGrid(startMs: Long, endMs: Long, stepMs: Long): Int {
        val (from, to) = bounds(startMs, endMs)
        val stepUs = stepMs.coerceAtLeast(1L) * 1000L
        var target = from
        var index = lowerBound(ptsUs, from)
        var count = 0
        while (target < to) {
            while (index < ptsUs.size && ptsUs[index] < target) index++
            if (index >= ptsUs.size || ptsUs[index] >= to) break
            count++
            target += ((ptsUs[index] - target) / stepUs + 1L) * stepUs
        }
        return count
    }

    fun count(mode: ExtractMode, startMs: Long, endMs: Long): Int? = when (mode) {
        ExtractMode.EveryFrame -> countIn(startMs, endMs)
        is ExtractMode.EveryNFrames -> everyNth(startMs, endMs, mode.n)
        is ExtractMode.EveryNMillis -> onTimeGrid(startMs, endMs, mode.millis)
        ExtractMode.KeyframesOnly -> keyframesIn(startMs, endMs)
        is ExtractMode.FixedCount -> minOf(mode.count.coerceAtLeast(1), countIn(startMs, endMs))
        is ExtractMode.Content -> onTimeGrid(startMs, endMs, ContentSpec.SAMPLE_STEP_MS)
        ExtractMode.SceneChange, ExtractMode.Slides -> null
    }

    private fun bounds(startMs: Long, endMs: Long): Pair<Long, Long> {
        val from = startMs.coerceAtLeast(0L) * 1000L
        return from to maxOf(endMs * 1000L, from + 1L)
    }

    private fun lowerBound(values: LongArray, target: Long): Int {
        var lo = 0
        var hi = values.size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (values[mid] < target) lo = mid + 1 else hi = mid
        }
        return lo
    }

    class Builder(private val limit: Int) {
        private var pts = LongArray(INITIAL)
        private var keys = LongArray(INITIAL)
        private var size = 0
        private var keySize = 0

        fun add(ptsUs: Long, keyframe: Boolean): Boolean {
            if (size >= limit) return false
            if (size == pts.size) pts = pts.copyOf(size * 2)
            pts[size++] = ptsUs
            if (keyframe) {
                if (keySize == keys.size) keys = keys.copyOf(keySize * 2)
                keys[keySize++] = ptsUs
            }
            return true
        }

        fun build(): FrameTimeline? {
            if (size == 0) return null
            return FrameTimeline(pts.copyOf(size).also { it.sort() }, keys.copyOf(keySize).also { it.sort() })
        }

        private companion object {
            const val INITIAL = 4096
        }
    }

    companion object {
        fun of(ptsUs: LongArray, keyUs: LongArray = LongArray(0)): FrameTimeline =
            FrameTimeline(ptsUs.copyOf().also { it.sort() }, keyUs.copyOf().also { it.sort() })
    }
}
