package com.frameout.app.viewer

import android.graphics.Bitmap
import com.frameout.core.db.FrameEntity
import com.frameout.core.db.FrameScanEntity
import com.frameout.core.db.FrameTagDao
import com.frameout.core.db.FrameTagEntity
import com.frameout.core.edit.DetectionMapper
import com.frameout.core.edit.FrameEdit
import com.frameout.core.quality.FrameTagger
import com.frameout.core.quality.NormBox
import com.frameout.core.quality.NormBoxCodec
import com.frameout.core.quality.SceneReasoner
import com.frameout.core.quality.TagFamilies
import com.frameout.core.quality.TagHit
import com.frameout.core.quality.TagRules
import kotlin.math.roundToInt

internal object ViewerDetections {
    private val QUIET = setOf(TagRules.GROUP, TagRules.GOOD_FACE)

    private const val WINDOW = 4
    const val LEGACY_BOX_CAP = 8
    const val LIVE_SIDE = 1600

    class Stored(val detections: List<Detection>, val complete: Boolean)

    class Live(val detections: List<Detection>, val failedAll: Boolean)

    suspend fun load(dao: FrameTagDao, frame: FrameEntity, edit: FrameEdit, ignoreStored: Boolean = false): Stored {
        if (ignoreStored) return Stored(emptyList(), false)
        val rows = runCatching { dao.boxedTagsForFrame(frame.id) }.getOrDefault(emptyList()).filterNot { TagFamilies.isFamily(it.tag) }
        val scanned = runCatching { dao.scanCount(frame.id) > 0 }.getOrDefault(false)
        val hits = rows.map { TagHit(it.tag, it.confidence, NormBoxCodec.decode(it.boxes)) }
        val around = runCatching { dao.tagsAround(frame.projectId, frame.id, frame.index - WINDOW, frame.index + WINDOW) }
            .getOrDefault(emptyList())
            .filterNot { TagFamilies.isFamily(it.tag) }
            .groupBy { it.frameId }.values
            .map { rows2 -> rows2.map { TagHit(it.tag, it.confidence, NormBoxCodec.decode(it.boxes)) } }
        return Stored(detectionsFrom(hits, around, edit), scanned && !truncated(hits))
    }

    fun truncated(hits: List<TagHit>): Boolean = hits.any { it.boxes.size == LEGACY_BOX_CAP }

    fun detectionsFrom(hits: List<TagHit>, around: List<List<TagHit>>, edit: FrameEdit?): List<Detection> {
        val refined = SceneReasoner.refine(hits, around).filter { it.tag !in QUIET && it.boxes.isNotEmpty() }
        return group(refined.flatMap { hit ->
            hit.boxes.mapNotNull { box -> if (edit == null) box else DetectionMapper.map(box, edit) }.map { it to hit.tag }
        })
    }

    suspend fun scan(source: Bitmap, store: (suspend (List<TagHit>) -> Unit)? = null): Live {
        val work = workingCopy(source) ?: return Live(emptyList(), true)
        try {
            val outcome = FrameTagger.tagDetailed("viewer-live", work)
            if (store != null && outcome.failed.isEmpty()) store(outcome.hits)
            return Live(detectionsFrom(outcome.hits, emptyList(), null), outcome.failedDetectors >= FrameTagger.DETECTORS)
        } finally {
            if (!work.isRecycled) work.recycle()
        }
    }

    suspend fun scanAndStore(dao: FrameTagDao, frame: FrameEntity, source: Bitmap): Live = scan(source) { hits ->
        val tags = hits.map { FrameTagEntity(frameId = frame.id, projectId = frame.projectId, tag = it.tag, confidence = it.confidence, boxes = NormBoxCodec.encode(it.boxes)) }
        runCatching { dao.commitScan(listOf(frame.id), tags, listOf(FrameScanEntity(frame.id, frame.projectId))) }
    }

    private fun workingCopy(source: Bitmap): Bitmap? = try {
        if (source.isRecycled) null else {
            val side = maxOf(source.width, source.height)
            val factor = if (side > LIVE_SIDE) LIVE_SIDE.toFloat() / side else 1f
            if (factor < 1f) {
                Bitmap.createScaledBitmap(source, (source.width * factor).roundToInt().coerceAtLeast(1), (source.height * factor).roundToInt().coerceAtLeast(1), true)
            } else {
                source.copy(Bitmap.Config.ARGB_8888, false)
            }
        }
    } catch (e: IllegalStateException) {
        null
    } catch (e: OutOfMemoryError) {
        null
    }

    fun group(pairs: List<Pair<NormBox, String>>): List<Detection> =
        pairs.groupBy { (box, _) -> listOf(box.l, box.t, box.r, box.b).map { (it * 100).roundToInt() } }
            .values
            .map { same -> Detection(same.first().first, same.map { it.second }.distinct()) }
            .sortedByDescending { it.box.width * it.box.height }
}
