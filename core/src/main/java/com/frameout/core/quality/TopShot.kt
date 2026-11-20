package com.frameout.core.quality

import kotlin.math.max
import kotlin.math.min

object TopShot {
    const val SWAP_GAIN = 1.15
    const val MAX_RADIUS = 6

    fun radius(count: Int, picks: Int): Int = (count / max(1, picks) / 3).coerceIn(1, MAX_RADIUS)

    fun candidates(frames: List<ScoredFrame>, picks: List<Long>): Set<Long> {
        val sorted = frames.sortedBy { it.index }
        val at = sorted.withIndex().associate { it.value.id to it.index }
        val r = radius(sorted.size, picks.size)
        val out = HashSet<Long>()
        for (id in picks) {
            val i = at[id] ?: continue
            for (k in max(0, i - r)..min(sorted.lastIndex, i + r)) out += sorted[k].id
        }
        return out
    }

    fun refine(frames: List<ScoredFrame>, picks: List<Long>, faces: Map<Long, Float>): List<Long> {
        if (picks.isEmpty()) return picks
        val sorted = frames.sortedBy { it.index }
        val position = sorted.withIndex().associate { it.value.id to it.index }
        val shots = ShotSegments.split(sorted)
        val r = radius(sorted.size, picks.size)
        val chosen = picks.map { sorted[position[it] ?: return picks] }.toMutableList()
        for (k in chosen.indices) {
            val others = chosen.filterIndexed { j, _ -> j != k }
            chosen[k] = bestTake(chosen[k], sorted, shots, position.getValue(chosen[k].id), r, faces, others)
        }
        return chosen.sortedBy { it.index }.map { it.id }
    }

    private fun value(frame: ScoredFrame, faces: Map<Long, Float>): Double =
        frame.copy(faceScore = faces[frame.id]).effective * frame.tone

    private fun clashes(frame: ScoredFrame, others: List<ScoredFrame>): Boolean = others.any { o ->
        o.id == frame.id || (frame.sig != null && o.sig != null && FrameSignature.same(frame.sig, o.sig, 1.0, frame.noise, o.noise))
    }

    private fun bestTake(current: ScoredFrame, sorted: List<ScoredFrame>, shots: ShotSegments.Shots, at: Int, r: Int, faces: Map<Long, Float>, others: List<ScoredFrame>): ScoredFrame {
        var best = current
        var bestValue = value(current, faces) * SWAP_GAIN
        for (i in max(0, at - r)..min(sorted.lastIndex, at + r)) {
            val c = sorted[i]
            if (c.id == current.id || shots.of[i] != shots.of[at] || clashes(c, others)) continue
            val v = value(c, faces)
            if (v > bestValue) {
                best = c
                bestValue = v
            }
        }
        return best
    }
}
