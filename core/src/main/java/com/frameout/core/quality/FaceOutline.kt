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
import com.frameout.core.util.safeCoerceIn

object FaceOutline {
    private const val CROP_PAD = 0.35f
    private const val MIN_CROP_SIDE = 320
    private const val MAX_CROP_SIDE = 1024
    private const val GROW = 1.1f
    private const val MIN_POINTS = 8
    private const val MIN_OVERLAP = 0.2f
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
        val left = (area.l * source.width).roundToInt().safeCoerceIn(0, source.width - 1)
        val top = (area.t * source.height).roundToInt().safeCoerceIn(0, source.height - 1)
        val right = (area.r * source.width).roundToInt().safeCoerceIn(left + 1, source.width)
        val bottom = (area.b * source.height).roundToInt().safeCoerceIn(top + 1, source.height)
        val crop = try {
            Bitmap.createBitmap(source, left, top, right - left, bottom - top)
        } catch (e: IllegalArgumentException) {
            return@withPermit null
        } catch (e: OutOfMemoryError) {
            return@withPermit null
        }
        val work = fitted(crop)
        try {
            val target = cropTarget(box, left, top, right - left, bottom - top, source.width, source.height)
            val outline = outlineOf(work, target) ?: return@withPermit null
            outline.map { cropToSource(it.x, it.y, left, top, right - left, bottom - top, source.width, source.height) }
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

    internal fun cropToSource(x: Float, y: Float, left: Int, top: Int, cropW: Int, cropH: Int, srcW: Int, srcH: Int): MarkPoint =
        MarkPoint((left + x * cropW) / srcW, (top + y * cropH) / srcH)

    internal fun cropTarget(box: NormBox, left: Int, top: Int, cropW: Int, cropH: Int, srcW: Int, srcH: Int): NormBox =
        NormBox(
            ((box.l * srcW - left) / cropW).coerceIn(0f, 1f),
            ((box.t * srcH - top) / cropH).coerceIn(0f, 1f),
            ((box.r * srcW - left) / cropW).coerceIn(0f, 1f),
            ((box.b * srcH - top) / cropH).coerceIn(0f, 1f)
        )

    internal fun overlap(a: NormBox, b: NormBox): Float {
        val w = minOf(a.r, b.r) - maxOf(a.l, b.l)
        val h = minOf(a.b, b.b) - maxOf(a.t, b.t)
        if (w <= 0f || h <= 0f) return 0f
        val inter = w * h
        val union = a.width * a.height + b.width * b.height - inter
        return if (union > 0f) inter / union else 0f
    }

    internal fun <T> pick(candidates: List<Pair<T, NormBox>>, target: NormBox): T? {
        val best = candidates.maxByOrNull { overlap(it.second, target) } ?: return null
        val face = best.second
        val cx = (face.l + face.r) / 2f
        val cy = (face.t + face.b) / 2f
        val inside = cx in target.l..target.r && cy in target.t..target.b
        return if (inside || overlap(face, target) >= MIN_OVERLAP) best.first else null
    }

    private suspend fun outlineOf(bitmap: Bitmap, target: NormBox): List<MarkPoint>? {
        val w = bitmap.width.toFloat()
        val h = bitmap.height.toFloat()
        val faces = detector.process(InputImage.fromBitmap(bitmap, 0)).await()
        val boxes = faces.map { f ->
            val b = f.boundingBox
            f to NormBox((b.left / w).coerceIn(0f, 1f), (b.top / h).coerceIn(0f, 1f), (b.right / w).coerceIn(0f, 1f), (b.bottom / h).coerceIn(0f, 1f))
        }
        val face = pick(boxes, target) ?: return null
        val pts = face.getContour(FaceContour.FACE)?.points ?: return null
        if (pts.size < MIN_POINTS) return null
        val cx = pts.sumOf { it.x.toDouble() }.toFloat() / pts.size
        val cy = pts.sumOf { it.y.toDouble() }.toFloat() / pts.size
        return pts.map { MarkPoint(((cx + (it.x - cx) * GROW) / w).coerceIn(0f, 1f), ((cy + (it.y - cy) * GROW) / h).coerceIn(0f, 1f)) }
    }
}
