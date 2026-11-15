package com.frameout.core.quality

import com.frameout.core.db.FrameTagDao

object TagSmoother {
    const val WINDOW = 4
    const val MIN_CONTEXT = 3
    const val MIN_SUPPORT = 0.2f
    const val KEEP_CONFIDENCE = 0.85f
    private const val CHUNK = 400

    fun smoothable(tag: String): Boolean =
        tag.startsWith(TagRules.LABEL) || tag.startsWith(TagRules.OBJECT) || TagFamilies.isFamily(tag)

    fun weakTags(frames: List<Map<String, Float>>): List<Set<String>> = frames.indices.map { i ->
        val from = maxOf(0, i - WINDOW)
        val to = minOf(frames.lastIndex, i + WINDOW)
        val neighbours = to - from
        if (neighbours < MIN_CONTEXT) emptySet() else frames[i].filter { (tag, confidence) ->
            confidence < KEEP_CONFIDENCE && support(frames, from, to, i, tag).toFloat() / neighbours < MIN_SUPPORT
        }.keys
    }

    private fun support(frames: List<Map<String, Float>>, from: Int, to: Int, self: Int, tag: String): Int =
        (from..to).count { it != self && tag in frames[it] }

    suspend fun apply(dao: FrameTagDao, projectId: Long): Int {
        val order = dao.scannedFrameIds(projectId)
        if (order.size <= MIN_CONTEXT) return 0
        val rows = dao.allTags(projectId).filter { smoothable(it.tag) }.groupBy { it.frameId }
        val weak = weakTags(order.map { id -> rows[id].orEmpty().associate { it.tag to it.confidence } })
        val doomed = ArrayList<Long>()
        order.forEachIndexed { i, id ->
            if (weak[i].isNotEmpty()) rows[id].orEmpty().filter { it.tag in weak[i] }.forEach { doomed += it.id }
        }
        doomed.chunked(CHUNK).forEach { dao.deleteTagRows(it) }
        return doomed.size
    }
}
