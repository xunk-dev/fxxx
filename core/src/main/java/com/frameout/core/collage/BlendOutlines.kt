package com.frameout.core.collage

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt

private const val DENSE = 180
private const val TAU = (2.0 * PI).toFloat()

private class Poly {
    val data = ArrayList<Float>()

    fun pt(x: Float, y: Float): Poly {
        data.add(x)
        data.add(y)
        return this
    }

    fun arc(cx: Float, cy: Float, r: Float, fromDeg: Float, toDeg: Float, steps: Int): Poly {
        for (i in 0..steps) {
            val a = Math.toRadians((fromDeg + (toDeg - fromDeg) * i / steps).toDouble())
            pt(cx + r * cos(a).toFloat(), cy + r * sin(a).toFloat())
        }
        return this
    }

    fun done(): FloatArray = data.toFloatArray()
}

internal object BlendOutlines {
    const val POINTS = 72

    val rect: FloatArray by lazy { resample(Poly().pt(0.5f, 0f).pt(1f, 0f).pt(1f, 1f).pt(0f, 1f).pt(0f, 0f).done(), POINTS) }

    private val shapes: Map<CollageBlend, FloatArray> by lazy {
        CollageBlend.all.filter { it.family == BlendFamily.Cell }.associateWith { resample(outline(it), POINTS) }
    }

    fun shape(kind: CollageBlend): FloatArray = shapes[kind] ?: rect

    private fun outline(kind: CollageBlend): FloatArray = when (kind) {
        CollageBlend.Circle -> radial(1f)
        CollageBlend.Squircle -> radial(0.5f)
        CollageBlend.Diamond -> Poly().pt(0.5f, 0f).pt(1f, 0.5f).pt(0.5f, 1f).pt(0f, 0.5f).done()
        CollageBlend.Hexagon -> hexagon()
        CollageBlend.Octagon -> octagon()
        CollageBlend.Ticket -> ticket()
        CollageBlend.Heart -> heart()
        CollageBlend.Arch -> arch()
        CollageBlend.Leaf -> leaf()
        CollageBlend.Blob -> blob()
        else -> rect
    }

    private fun radial(power: Float): FloatArray {
        val p = Poly()
        for (i in 0 until DENSE) {
            val a = TAU * i / DENSE
            val sx = sin(a)
            val cy = cos(a)
            p.pt(0.5f + 0.5f * sign(sx) * pow(abs(sx), power), 0.5f - 0.5f * sign(cy) * pow(abs(cy), power))
        }
        return p.done()
    }

    private fun pow(v: Float, e: Float): Float = if (e == 1f) v else if (e == 0.5f) sqrt(v) else Math.pow(v.toDouble(), e.toDouble()).toFloat()

    private fun hexagon(): FloatArray {
        val p = Poly()
        for (k in 0 until 6) {
            val a = Math.toRadians((-90.0 + 60.0 * k))
            p.pt(0.5f + 0.5f * cos(a).toFloat() / 0.8660254f, 0.5f + 0.5f * sin(a).toFloat())
        }
        return p.done()
    }

    private fun octagon(): FloatArray {
        val c = 0.29f
        return Poly().pt(0.5f, 0f).pt(1f - c, 0f).pt(1f, c).pt(1f, 1f - c).pt(1f - c, 1f).pt(c, 1f).pt(0f, 1f - c).pt(0f, c).pt(c, 0f).done()
    }

    private fun ticket(): FloatArray {
        val r = 0.12f
        return Poly().pt(0.5f, 0f).pt(1f, 0f).pt(1f, 0.5f - r)
            .arc(1f, 0.5f, r, -90f, -270f, 16)
            .pt(1f, 1f).pt(0f, 1f).pt(0f, 0.5f + r)
            .arc(0f, 0.5f, r, 90f, -90f, 16)
            .pt(0f, 0f).done()
    }

    private fun arch(): FloatArray = Poly().arc(0.5f, 0.5f, 0.5f, -90f, 0f, 24).pt(1f, 1f).pt(0f, 1f).arc(0.5f, 0.5f, 0.5f, 180f, 270f, 24).done()

    private fun leaf(): FloatArray = Poly().pt(0.5f, 0f).pt(1f, 0f).arc(0.5f, 0.5f, 0.5f, 0f, 90f, 24).pt(0f, 1f).arc(0.5f, 0.5f, 0.5f, 180f, 270f, 24).done()

    private fun blob(): FloatArray {
        val p = Poly()
        for (i in 0 until DENSE) {
            val a = TAU * i / DENSE
            val r = 0.43f * (1f + 0.08f * sin(3f * a + 0.6f) + 0.05f * sin(5f * a + 1.9f))
            p.pt(0.5f + r * sin(a), 0.5f - r * cos(a))
        }
        return p.done()
    }

    private fun heart(): FloatArray {
        val raw = FloatArray(DENSE * 2)
        for (i in 0 until DENSE) {
            val t = TAU * i / DENSE
            raw[2 * i] = 16f * sin(t) * sin(t) * sin(t)
            raw[2 * i + 1] = 13f * cos(t) - 5f * cos(2f * t) - 2f * cos(3f * t) - cos(4f * t)
        }
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        for (i in 0 until DENSE) {
            minY = minOf(minY, raw[2 * i + 1])
            maxY = maxOf(maxY, raw[2 * i + 1])
        }
        val out = FloatArray(DENSE * 2)
        for (i in 0 until DENSE) {
            out[2 * i] = (raw[2 * i] + 16f) / 32f
            out[2 * i + 1] = 1f - (raw[2 * i + 1] - minY) / (maxY - minY)
        }
        return out
    }

    fun resample(poly: FloatArray, n: Int): FloatArray {
        val count = poly.size / 2
        val cum = FloatArray(count + 1)
        for (i in 0 until count) {
            val j = (i + 1) % count
            cum[i + 1] = cum[i] + hypot(poly[2 * j] - poly[2 * i], poly[2 * j + 1] - poly[2 * i + 1])
        }
        return sample(poly, cum, count, n)
    }

    private fun sample(poly: FloatArray, cum: FloatArray, count: Int, n: Int): FloatArray {
        val out = FloatArray(n * 2)
        var seg = 0
        for (k in 0 until n) {
            val d = cum[count] * k / n
            while (seg < count - 1 && cum[seg + 1] < d) seg++
            val len = cum[seg + 1] - cum[seg]
            val f = if (len <= 0f) 0f else (d - cum[seg]) / len
            val j = (seg + 1) % count
            out[2 * k] = poly[2 * seg] + (poly[2 * j] - poly[2 * seg]) * f
            out[2 * k + 1] = poly[2 * seg + 1] + (poly[2 * j + 1] - poly[2 * seg + 1]) * f
        }
        return out
    }
}
