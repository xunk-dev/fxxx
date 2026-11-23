package com.frameout.core.collage

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import com.frameout.core.util.safeCoerceIn

object CollageScale {
    fun draw(canvas: Canvas, src: Bitmap, window: SourceWindow, dest: RectF, paint: Paint) {
        var bmp = src
        var win = window
        while ((win.right - win.left) / dest.width() > 2f) {
            val next = halve(bmp, win) ?: break
            if (bmp !== src) bmp.recycle()
            bmp = next.first
            win = next.second
        }
        val kx = dest.width() / (win.right - win.left)
        val ky = dest.height() / (win.bottom - win.top)
        val matrix = Matrix().apply {
            postTranslate(-win.left, -win.top)
            postScale(kx, ky)
            postTranslate(dest.left, dest.top)
        }
        val save = canvas.save()
        canvas.clipRect(dest)
        canvas.drawBitmap(bmp, matrix, paint)
        canvas.restoreToCount(save)
        if (bmp !== src) bmp.recycle()
    }

    private fun halve(bmp: Bitmap, win: SourceWindow): Pair<Bitmap, SourceWindow>? {
        val l = floor(win.left).toInt().safeCoerceIn(0, bmp.width - 1)
        val t = floor(win.top).toInt().safeCoerceIn(0, bmp.height - 1)
        val r = ceil(win.right).toInt().safeCoerceIn(l + 1, bmp.width)
        val b = ceil(win.bottom).toInt().safeCoerceIn(t + 1, bmp.height)
        val w = max(1, (r - l) / 2)
        val h = max(1, (b - t) / 2)
        if (w >= r - l || h >= b - t) return null
        val piece = Bitmap.createBitmap(bmp, l, t, r - l, b - t)
        val scaled = Bitmap.createScaledBitmap(piece, w, h, true)
        if (piece !== bmp && piece !== scaled) piece.recycle()
        val fx = w.toFloat() / (r - l)
        val fy = h.toFloat() / (b - t)
        return scaled to SourceWindow((win.left - l) * fx, (win.top - t) * fy, (win.right - l) * fx, (win.bottom - t) * fy)
    }
}
