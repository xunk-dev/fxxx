package com.frameout.core.hdr

import android.graphics.Bitmap
import android.graphics.Matrix
import java.util.Collections
import java.util.WeakHashMap
import kotlin.math.ceil

class LegacyGain(
    val map: Bitmap,
    val ratioMin: FloatArray,
    val ratioMax: FloatArray,
    val epsilon: Float,
    val fullHdrRatio: Float
) {
    fun withMap(next: Bitmap) = LegacyGain(next, ratioMin, ratioMax, epsilon, fullHdrRatio)
}

object LegacyGainRegistry {
    private val gains = Collections.synchronizedMap(WeakHashMap<Bitmap, LegacyGain>())

    fun attach(bitmap: Bitmap, gain: LegacyGain) {
        gains[bitmap] = gain
    }

    fun peek(bitmap: Bitmap): LegacyGain? = if (bitmap.isRecycled) null else gains[bitmap]?.takeUnless { it.map.isRecycled }

    fun has(bitmap: Bitmap): Boolean = peek(bitmap) != null

    fun drop(bitmap: Bitmap) {
        gains.remove(bitmap)
    }

    fun copy(from: Bitmap, to: Bitmap) {
        val gain = peek(from) ?: return
        gains[to] = gain
    }

    fun crop(from: Bitmap, to: Bitmap, left: Int, top: Int, right: Int, bottom: Int) {
        val gain = peek(from) ?: return
        val contents = gain.map
        val sx = contents.width / from.width.toFloat()
        val sy = contents.height / from.height.toFloat()
        val gl = (left * sx).toInt().coerceIn(0, contents.width - 1)
        val gt = (top * sy).toInt().coerceIn(0, contents.height - 1)
        val gr = ceil(right * sx).toInt().coerceIn(gl + 1, contents.width)
        val gb = ceil(bottom * sy).toInt().coerceIn(gt + 1, contents.height)
        gains[to] = gain.withMap(Bitmap.createBitmap(contents, gl, gt, gr - gl, gb - gt))
    }

    fun transform(from: Bitmap, to: Bitmap, rotation: Int, flipH: Boolean, flipV: Boolean) {
        val gain = peek(from) ?: return
        val contents = gain.map
        val matrix = Matrix().apply {
            postScale(if (flipH) -1f else 1f, if (flipV) -1f else 1f)
            postRotate(rotation.toFloat())
        }
        gains[to] = gain.withMap(Bitmap.createBitmap(contents, 0, 0, contents.width, contents.height, matrix, false))
    }
}
