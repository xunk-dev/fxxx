package com.frameout.core.quality

import android.graphics.Bitmap
import com.frameout.core.edit.MarkPoint
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

object FaceOutline {
    private const val CROP_PAD = 0.35f
    private const val MIN_CROP_SIDE = 320
    private const val MAX_CROP_SIDE = 1024
    private const val GROW = 1.1f
    private const val MIN_POINTS = 8
    private val gate = Semaphore(1)

    private val detector by lazy {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setMinFaceSize(0.1f)
                .build()
        )
    }

    suspend fun find(source: Bitmap, box: NormBox): List<MarkPoint>? = gate.withPermit {
        if (source.isRecycled) return@withPermit null
        val area = box.padded(CROP_PAD)
        val left = (area.l * source.width).roundToInt().coerceIn(0, source.width - 1)
        val top = (area.t * source.height).roundToInt().coerceIn(0, source.height - 1)
        val right = (area.r * source.width).roundToInt().coerceIn(left + 1, source.width)
        val bottom = (area.b * source.height).roundToInt().coerceIn(top + 1, source.height)
        val crop = try {
            Bitmap.createBitmap(source, left, top, right - left, bottom - top)
        } catch (e: IllegalArgumentException) {
            return@withPermit null
        } catch (e: OutOfMemoryError) {
            return@withPermit null
        }
        val work = fitted(crop)
        try {
            val outline = outlineOf(work) ?: return@withPermit null
            outline.map { MarkPoint(left / source.width + it.x * (right - left) / source.width, top / source.height + it.y * (bottom - top) / source.height) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            null
        } finally {
            if (work !== crop && !work.isRecycled) work.recycle()
            if (!crop.isRecycled && crop !== source) crop.recycle()
        }
    }

    private fun fitted(crop: Bitmap): Bitmap {
        val side = max(crop.width, crop.height)
        val target = side.coerceIn(MIN_CROP_SIDE, MAX_CROP_SIDE)
        val argb = if (crop.config == Bitmap.Config.ARGB_8888) crop else crop.copy(Bitmap.Config.ARGB_8888, false) ?: return crop
        if (target == side) return argb
        val k = target.toFloat() / side
        return Bitmap.createScaledBitmap(argb, (argb.width * k).roundToInt().coerceAtLeast(1), (argb.height * k).roundToInt().coerceAtLeast(1), true)
    }

    private suspend fun outlineOf(bitmap: Bitmap): List<MarkPoint>? {
        val faces = detector.process(InputImage.fromBitmap(bitmap, 0)).await()
        val face = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() } ?: return null
        val pts = face.getContour(FaceContour.FACE)?.points ?: return null
        if (pts.size < MIN_POINTS) return null
        val w = bitmap.width.toFloat()
        val h = bitmap.height.toFloat()
        val cx = pts.sumOf { it.x.toDouble() }.toFloat() / pts.size
        val cy = pts.sumOf { it.y.toDouble() }.toFloat() / pts.size
        return pts.map { MarkPoint(((cx + (it.x - cx) * GROW) / w).coerceIn(0f, 1f), ((cy + (it.y - cy) * GROW) / h).coerceIn(0f, 1f)) }
    }
}
