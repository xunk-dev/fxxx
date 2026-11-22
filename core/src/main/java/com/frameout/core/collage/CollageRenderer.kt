package com.frameout.core.collage

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Path
import java.io.File
import com.frameout.core.util.safeCoerceIn

object CollageRenderer {
    fun render(paths: List<String?>, config: CollageConfig): Bitmap {
        val template = config.template
        val items = paths.take(template.cells.size)
        val size = outputSize(config.outputWidth, template.aspect)
        val bmp = Bitmap.createBitmap(size.first, size.second, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(config.backgroundColor.toInt())
        drawCells(canvas, template, items, config, size.first, size.second)
        return bmp
    }

    fun outputSize(requestedWidth: Int, aspect: Float): Pair<Int, Int> =
        collageOutputSize(requestedWidth, aspect, collageMemoryPixels(Runtime.getRuntime().maxMemory()))

    private fun drawCells(canvas: Canvas, template: CollageTemplate, items: List<String?>, config: CollageConfig, outW: Int, outH: Int) {
        val k = collageUnit(outW.toFloat())
        val gap = config.spacingPx * k
        val radius = config.cornerRadiusPx * k
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val looks = if (config.blend == CollageBlend.None) null else BlendLayout.compute(template, config.blend, config.blendStrength, outW.toFloat(), outH.toFloat(), gap)
        template.cells.forEachIndexed { i, cell ->
            if (i >= items.size) return@forEachIndexed
            val look = looks?.getOrNull(i)
            if (look != null) {
                BlendDraw.draw(canvas, look, load(items[i]), config, k, paint, i)
            } else {
                val r = cell.toPx(outW.toFloat(), outH.toFloat(), gap).snapped()
                drawCell(canvas, RectF(r.left, r.top, r.right, r.bottom), load(items[i]), config, radius, k, paint, i)
            }
        }
    }

    private fun drawCell(canvas: Canvas, dest: RectF, src: Bitmap?, config: CollageConfig, radius: Float, k: Float, paint: Paint, index: Int) {
        if (src == null) {
            paint.color = 0xFF2A2A2A.toInt()
            canvas.drawRoundRect(dest, radius, radius, paint)
            return
        }
        val save = canvas.save()
        if (radius > 0f) {
            val path = Path().apply { addRoundRect(dest, radius, radius, Path.Direction.CW) }
            canvas.clipPath(path)
        }
        try {
            val geometry = CropGeometry(src.width.toFloat(), src.height.toFloat(), dest.width(), dest.height())
            val window = geometry.window(config.crops.getOrNull(index) ?: CellCrop())
            CollageScale.draw(canvas, src, window, dest, paint)
            stamp(canvas, dest, config.timestamps.getOrNull(index), k, paint)
        } finally {
            canvas.restoreToCount(save)
            src.recycle()
        }
    }

    internal fun stamp(canvas: Canvas, dest: RectF, text: String?, k: Float, paint: Paint, padX: Float = 0f, padY: Float = 0f) {
        if (text.isNullOrBlank()) return
        val size = (dest.width() * 0.07f).safeCoerceIn(22f * k, 44f * k)
        paint.textSize = size
        paint.isFakeBoldText = true
        val padW = size * 0.55f
        val padH = size * 0.32f
        val textW = paint.measureText(text)
        val margin = size * 0.6f
        val left = dest.left + padX + margin
        val bottom = dest.bottom - padY - margin
        val pill = RectF(left, bottom - size - padH * 2, left + textW + padW * 2, bottom)
        paint.color = 0x99000000.toInt()
        canvas.drawRoundRect(pill, pill.height() / 2f, pill.height() / 2f, paint)
        paint.color = 0xFFFFFFFF.toInt()
        canvas.drawText(text, pill.left + padW, pill.bottom - padH - size * 0.18f, paint)
        paint.isFakeBoldText = false
    }

    private fun load(path: String?): Bitmap? {
        if (path.isNullOrBlank()) return null
        val file = File(path)
        if (!file.exists()) return null
        var sample = sampleFor(file, COLLAGE_SOURCE_MAX_SIDE)
        repeat(4) {
            try {
                return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply {
                    inSampleSize = sample
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                })
            } catch (e: OutOfMemoryError) {
                sample *= 2
            }
        }
        return null
    }

    private fun sampleFor(file: File, maxSide: Int): Int {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        var sample = 1
        while (bounds.outWidth / sample > maxSide || bounds.outHeight / sample > maxSide) sample *= 2
        return sample
    }
}
