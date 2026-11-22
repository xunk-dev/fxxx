package com.frameout.core.collage

import kotlin.math.abs

private const val NONE = 0
private const val EXTEND = 1
private const val RAMP = 2
private const val HALF_CAP = 0.22f
private const val TOUCH = 1.5f
private const val MIN_OVERLAP = 2f

internal object BlendFade {
    fun cells(rects: List<PxRect>, blend: CollageBlend, t: Float, w: Float, h: Float): List<CellBlend> {
        val smallest = rects.minOfOrNull { minOf(it.width, it.height) } ?: 0f
        val half = minOf(halfWidth(blend) * minOf(w, h) * t, smallest * HALF_CAP)
        return rects.mapIndexed { i, r -> cell(i, r, rects, half) }
    }

    fun halfWidth(blend: CollageBlend): Float = if (blend == CollageBlend.Mist) 0.14f else 0.06f

    private fun cell(i: Int, r: PxRect, rects: List<PxRect>, half: Float): CellBlend {
        if (half <= 0f) return CellBlend(r, null)
        val l = relation(i, r, rects, 0)
        val t = relation(i, r, rects, 1)
        val rt = relation(i, r, rects, 2)
        val b = relation(i, r, rects, 3)
        val bounds = PxRect(
            r.left - if (l != NONE) half else 0f,
            r.top - if (t != NONE) half else 0f,
            r.right + if (rt != NONE) half else 0f,
            r.bottom + if (b != NONE) half else 0f
        )
        val ramps = Ramps(ramp(l, half), ramp(t, half), ramp(rt, half), ramp(b, half))
        return CellBlend(bounds, null, ramps, ramps.left, ramps.bottom)
    }

    private fun ramp(relation: Int, half: Float): Float = if (relation == RAMP) half * 2f else 0f

    private fun relation(i: Int, r: PxRect, rects: List<PxRect>, side: Int): Int {
        var result = NONE
        rects.forEachIndexed { j, o ->
            if (j != i && abuts(r, o, side)) result = if (j < i) RAMP else maxOf(result, EXTEND)
        }
        return result
    }

    private fun abuts(r: PxRect, o: PxRect, side: Int): Boolean = when (side) {
        0 -> near(o.right, r.left) && overlap(r.top, r.bottom, o.top, o.bottom)
        1 -> near(o.bottom, r.top) && overlap(r.left, r.right, o.left, o.right)
        2 -> near(o.left, r.right) && overlap(r.top, r.bottom, o.top, o.bottom)
        else -> near(o.top, r.bottom) && overlap(r.left, r.right, o.left, o.right)
    }

    private fun near(a: Float, b: Float): Boolean = abs(a - b) <= TOUCH

    private fun overlap(a0: Float, a1: Float, b0: Float, b1: Float): Boolean = minOf(a1, b1) - maxOf(a0, b0) > MIN_OVERLAP
}
