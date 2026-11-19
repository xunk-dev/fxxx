package com.frameout.core.quality

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt
object ShotSegments {
    const val CUT = 0.55

    class Shots(val of: IntArray, val sizes: IntArray) {
        val count: Int get() = sizes.size
        fun weights(): DoubleArray {
            val longest = (sizes.maxOrNull() ?: 1).coerceAtLeast(1)
            return DoubleArray(sizes.size) { sqrt(sizes[it].toDouble() / longest) }
        }
    }

    fun split(frames: List<ScoredFrame>): Shots {
        val of = IntArray(frames.size)
        var shot = 0
        for (i in 1 until frames.size) {
            val a = frames[i - 1].sig
            val b = frames[i].sig
            if (a != null && b != null && FrameSignature.gap(a, b).changedShare >= CUT) shot++
            of[i] = shot
        }
        val sizes = IntArray(if (frames.isEmpty()) 0 else shot + 1)
        for (s in of) sizes[s]++
        return Shots(of, sizes)
    }
    fun typicality(frames: List<ScoredFrame>, shots: Shots): DoubleArray {
        val sums = Array(shots.count) { DoubleArray(FrameSignature.SIZE) }
        val used = IntArray(shots.count)
        for ((i, f) in frames.withIndex()) {
            val sig = f.sig ?: continue
            if (sig.size != FrameSignature.SIZE) continue
            val s = shots.of[i]
            val acc = sums[s]
            for (c in 0 until FrameSignature.SIZE) acc[c] += (sig[c].toInt() and 0xFF).toDouble()
            used[s]++
        }
        val distance = DoubleArray(frames.size) { -1.0 }
        val worst = DoubleArray(shots.count)
        for ((i, f) in frames.withIndex()) {
            val sig = f.sig ?: continue
            if (sig.size != FrameSignature.SIZE) continue
            val s = shots.of[i]
            val acc = sums[s]
            val n = used[s]
            var total = 0.0
            for (c in 0 until FrameSignature.SIZE) total += abs((sig[c].toInt() and 0xFF) - acc[c] / n)
            distance[i] = total / FrameSignature.SIZE
            worst[s] = max(worst[s], distance[i])
        }
        return DoubleArray(frames.size) { i ->
            if (distance[i] < 0.0) 0.5
            else {
                val w = worst[shots.of[i]]
                if (w <= 1e-9) 1.0 else 1.0 - distance[i] / w
            }
        }
    }
}
