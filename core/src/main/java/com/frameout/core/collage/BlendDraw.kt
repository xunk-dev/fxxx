package com.frameout.core.collage

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader

private const val PLACEHOLDER = 0xFF2A2A2A.toInt()
private const val RAMP_CLEAR = 0x00000000
private const val RAMP_FULL = 0xFF000000.toInt()

internal object BlendDraw {
    fun draw(canvas: Canvas, look: CellBlend, src: Bitmap?, config: CollageConfig, k: Float, paint: Paint, index: Int) {
        val b = look.bounds
        val dest = RectF(b.left, b.top, b.right, b.bottom)
        if (src == null) {
            placeholder(canvas, look, dest, paint)
            return
        }
        val layer = if (look.ramps.any) canvas.saveLayer(dest, null) else canvas.save()
        try {
            paintImage(canvas, look, src, dest, config.crops.getOrNull(index) ?: CellCrop(), paint)
            if (look.ramps.any) applyRamps(canvas, dest, look.ramps)
        } finally {
            canvas.restoreToCount(layer)
            src.recycle()
        }
        CollageRenderer.stamp(canvas, dest, config.timestamps.getOrNull(index), k, paint, look.padX, look.padY)
    }

    fun path(points: FloatArray): Path = Path().apply {
        moveTo(points[0], points[1])
        var i = 2
        while (i < points.size) {
            lineTo(points[i], points[i + 1])
            i += 2
        }
        close()
    }

    private fun placeholder(canvas: Canvas, look: CellBlend, dest: RectF, paint: Paint) {
        paint.color = PLACEHOLDER
        val poly = look.polygon
        if (poly != null) canvas.drawPath(path(poly), paint) else canvas.drawRect(dest, paint)
    }

    private fun paintImage(canvas: Canvas, look: CellBlend, src: Bitmap, dest: RectF, crop: CellCrop, paint: Paint) {
        val save = canvas.save()
        look.polygon?.let { canvas.clipPath(path(it)) }
        val geometry = CropGeometry(src.width.toFloat(), src.height.toFloat(), dest.width(), dest.height())
        CollageScale.draw(canvas, src, geometry.window(crop), dest, paint)
        canvas.restoreToCount(save)
    }

    private fun applyRamps(canvas: Canvas, dest: RectF, ramps: Ramps) {
        val paint = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN) }
        if (ramps.left > 0f) ramp(canvas, dest, paint, dest.left, 0f, dest.left + ramps.left, 0f)
        if (ramps.top > 0f) ramp(canvas, dest, paint, 0f, dest.top, 0f, dest.top + ramps.top)
        if (ramps.right > 0f) ramp(canvas, dest, paint, dest.right, 0f, dest.right - ramps.right, 0f)
        if (ramps.bottom > 0f) ramp(canvas, dest, paint, 0f, dest.bottom, 0f, dest.bottom - ramps.bottom)
    }

    private fun ramp(canvas: Canvas, dest: RectF, paint: Paint, x0: Float, y0: Float, x1: Float, y1: Float) {
        paint.shader = LinearGradient(x0, y0, x1, y1, RAMP_CLEAR, RAMP_FULL, Shader.TileMode.CLAMP)
        canvas.drawRect(dest, paint)
    }
}
