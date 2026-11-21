package com.frameout.core.collage

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.tanh

private const val TAU = (2.0 * PI).toFloat()
private const val SKEW = 0.8f
private const val ENVELOPE = 0.05f
private const val DETAIL = 90f
private const val MAX_STEPS = 600
private const val AMP_CAP = 0.3f
private const val BLEED = 0.75f
private const val CELL_PAD = 0.16f
private val TANH4 = tanh(4f)

private class Frame(val w: Float, val h: Float, val amp: Float, val bleed: Float, val margin: Float)

object BlendLayout {
    fun gapFor(blend: CollageBlend, gap: Float): Float = if (blend.family == BlendFamily.Fade) 0f else gap

    fun compute(template: CollageTemplate, blend: CollageBlend, strength: Int, w: Float, h: Float, gap: Float): List<CellBlend> {
        val g = gapFor(blend, gap)
        val rects = template.cells.map { it.toPx(w, h, g).snapped() }
        val t = strength.coerceIn(0, BLEND_STRENGTH_MAX) / BLEND_STRENGTH_MAX.toFloat()
        return when (blend.family) {
            BlendFamily.None -> rects.map { CellBlend(it, null) }
            BlendFamily.Cell -> rects.map { cellShape(it, blend, t) }
            BlendFamily.Edge -> edges(rects, blend, t, w, h, g)
            BlendFamily.Fade -> BlendFade.cells(rects, blend, t, w, h)
        }
    }

    private fun fitsSquare(blend: CollageBlend): Boolean =
        blend == CollageBlend.Circle || blend == CollageBlend.Squircle || blend == CollageBlend.Heart || blend == CollageBlend.Blob

    private fun cellShape(rect: PxRect, blend: CollageBlend, t: Float): CellBlend {
        val shape = BlendOutlines.shape(blend)
        val box = BlendOutlines.rect
        val side = if (fitsSquare(blend)) minOf(rect.width, rect.height) else 0f
        val sw = if (side > 0f) side else rect.width
        val sh = if (side > 0f) side else rect.height
        val ox = rect.left + (rect.width - sw) / 2f
        val oy = rect.top + (rect.height - sh) / 2f
        val out = FloatArray(shape.size)
        for (i in shape.indices step 2) {
            val bx = rect.left + box[i] * rect.width
            val by = rect.top + box[i + 1] * rect.height
            out[i] = bx + (ox + shape[i] * sw - bx) * t
            out[i + 1] = by + (oy + shape[i + 1] * sh - by) * t
        }
        return CellBlend(rect, out, Ramps.None, rect.width * CELL_PAD * t, rect.height * CELL_PAD * t)
    }

    private fun edges(rects: List<PxRect>, blend: CollageBlend, t: Float, w: Float, h: Float, gap: Float): List<CellBlend> {
        val smallest = rects.minOfOrNull { minOf(it.width, it.height) } ?: 0f
        val amp = minOf(amplitude(blend) * minOf(w, h), smallest * AMP_CAP) * t
        val bleed = if (gap < 1f) BLEED else 0f
        return rects.map { r ->
            val poly = ring(r, blend, w, h, amp, bleed, gap)
            CellBlend(boundsOf(poly, w, h), poly)
        }
    }

    private fun ring(r: PxRect, blend: CollageBlend, w: Float, h: Float, amp: Float, bleed: Float, margin: Float): FloatArray {
        val spacing = (minOf(w, h) / DETAIL).coerceAtLeast(2f)
        val corners = floatArrayOf(r.left, r.top, r.right, r.top, r.right, r.bottom, r.left, r.bottom)
        val pts = ArrayList<Float>()
        for (e in 0 until 4) {
            val ax = corners[2 * e]
            val ay = corners[2 * e + 1]
            val bx = corners[2 * ((e + 1) % 4)]
            val by = corners[2 * ((e + 1) % 4) + 1]
            val n = ceil(hypot(bx - ax, by - ay) / spacing).toInt().coerceIn(1, MAX_STEPS)
            for (i in 0 until n) {
                val f = i.toFloat() / n
                pts.add(ax + (bx - ax) * f)
                pts.add(ay + (by - ay) * f)
            }
        }
        return place(pts, r, blend, Frame(w, h, amp, bleed, margin))
    }

    private fun place(pts: List<Float>, r: PxRect, blend: CollageBlend, f: Frame): FloatArray {
        val cx = (r.left + r.right) / 2f
        val cy = (r.top + r.bottom) / 2f
        val out = FloatArray(pts.size)
        for (i in pts.indices step 2) {
            val moved = warp(blend, pts[i], pts[i + 1], f.w, f.h, f.amp, f.margin)
            var x = moved.first
            var y = moved.second
            val d = hypot(x - cx, y - cy)
            if (f.bleed > 0f && d > 0f) {
                x += (x - cx) / d * f.bleed
                y += (y - cy) / d * f.bleed
            }
            out[i] = x.coerceIn(0f, f.w)
            out[i + 1] = y.coerceIn(0f, f.h)
        }
        return out
    }

    private fun boundsOf(poly: FloatArray, w: Float, h: Float): PxRect {
        var l = Float.MAX_VALUE
        var t = Float.MAX_VALUE
        var r = -Float.MAX_VALUE
        var b = -Float.MAX_VALUE
        for (i in poly.indices step 2) {
            l = minOf(l, poly[i])
            r = maxOf(r, poly[i])
            t = minOf(t, poly[i + 1])
            b = maxOf(b, poly[i + 1])
        }
        return PxRect(floor(l).coerceAtLeast(0f), floor(t).coerceAtLeast(0f), ceil(r).coerceAtMost(w), ceil(b).coerceAtMost(h))
    }

    fun amplitude(kind: CollageBlend): Float = when (kind) {
        CollageBlend.Wave -> 0.040f
        CollageBlend.Zigzag -> 0.035f
        CollageBlend.Slant -> 0.160f
        CollageBlend.Arc -> 0.060f
        CollageBlend.Steps -> 0.030f
        CollageBlend.Ripple -> 0.018f
        CollageBlend.Flow -> 0.050f
        CollageBlend.Scallop -> 0.035f
        CollageBlend.Torn -> 0.020f
        else -> 0f
    }

    private fun cycles(kind: CollageBlend): Float = when (kind) {
        CollageBlend.Wave -> 3f
        CollageBlend.Zigzag -> 5f
        CollageBlend.Steps -> 4f
        CollageBlend.Ripple -> 9f
        CollageBlend.Scallop -> 5f
        CollageBlend.Flow -> 2.5f
        CollageBlend.Torn -> 3f
        else -> 1f
    }

    private fun smooth(v: Float): Float {
        val x = v.coerceIn(0f, 1f)
        return x * x * (3f - 2f * x)
    }

    private fun envelope(u: Float, length: Float, margin: Float): Float =
        smooth((u - margin) / (ENVELOPE * length)) * smooth((length - margin - u) / (ENVELOPE * length))

    fun warp(kind: CollageBlend, x: Float, y: Float, w: Float, h: Float, amp: Float, margin: Float): Pair<Float, Float> {
        val dx = shift(kind, y / h, x / w, amp) * envelope(x, w, margin)
        val dy = if (kind == CollageBlend.Slant) 0f else shift(kind, x / w, y / h, amp) * envelope(y, h, margin)
        return Pair(x + dx, y + dy)
    }

    private fun shift(kind: CollageBlend, along: Float, across: Float, amp: Float): Float = when (kind) {
        CollageBlend.Slant -> amp * (along - 0.5f) * 2f
        CollageBlend.Arc -> amp * sin(PI.toFloat() * along)
        else -> amp * profile(kind, along * cycles(kind) + across * SKEW)
    }

    private fun profile(kind: CollageBlend, u: Float): Float = when (kind) {
        CollageBlend.Zigzag -> 2f * abs(2f * (u + 0.25f - floor(u + 0.25f)) - 1f) - 1f
        CollageBlend.Steps -> tanh(4f * sin(TAU * u)) / TANH4
        CollageBlend.Scallop -> 1f - 2f * abs(sin(PI.toFloat() * u))
        CollageBlend.Flow -> (sin(TAU * u) + 0.5f * sin(2f * TAU * u + 1f)) / 1.5f
        CollageBlend.Torn -> torn(u)
        else -> sin(TAU * u)
    }

    private fun torn(u: Float): Float =
        0.5f * sin(TAU * u + 1f) + 0.28f * sin(TAU * u * 2.7f + 2.2f) + 0.14f * sin(TAU * u * 6.3f + 0.5f) + 0.08f * sin(TAU * u * 8f)
}
