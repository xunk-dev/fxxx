package com.frameout.app.trim

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import com.frameout.core.quality.Sharpness
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import com.frameout.core.util.safeCoerceIn

internal object SuggestedMoments {
    private const val READY_SHARE = 0.9f
    private const val MIN_SLOTS = 6

    fun countFor(slots: Int): Int = (slots / 8).coerceIn(3, 6)

    fun gapFor(slots: Int): Int = (slots / 12).coerceAtLeast(2)

    fun ready(loaded: Int, slots: Int): Boolean = slots >= MIN_SLOTS && loaded >= slots * READY_SHARE

    fun pick(scores: List<Double?>, count: Int, minGap: Int): List<Int> {
        val picked = ArrayList<Int>()
        val order = scores.indices.filter { scores[it] != null && scores[it]!! > 0.0 }.sortedByDescending { scores[it]!! }
        for (slot in order) {
            if (picked.size >= count) break
            if (picked.none { kotlin.math.abs(it - slot) < minGap }) picked += slot
        }
        return picked.sorted()
    }

    fun timeOf(slot: Int, slots: Int, durationMs: Long): Long =
        ((slot + 0.5) / slots.coerceAtLeast(1) * durationMs).toLong().safeCoerceIn(0L, durationMs)
}

@Composable
internal fun rememberSuggestedMoments(thumbs: List<Bitmap?>, durationMs: Long): List<Long> {
    val loaded = thumbs.count { it != null }
    val slots = thumbs.size
    val moments by produceState(emptyList<Long>(), loaded, slots, durationMs) {
        if (durationMs <= 0L || !SuggestedMoments.ready(loaded, slots)) {
            value = emptyList()
            return@produceState
        }
        delay(150L)
        val snapshot = thumbs.toList()
        value = withContext(Dispatchers.Default) {
            val scores = snapshot.map { bitmap ->
                bitmap?.takeIf { !it.isRecycled }?.let { runCatching { Sharpness.score(Sharpness.lumaOf(it), it.width, it.height) }.getOrNull() }
            }
            SuggestedMoments.pick(scores, SuggestedMoments.countFor(slots), SuggestedMoments.gapFor(slots))
                .map { SuggestedMoments.timeOf(it, slots, durationMs) }
        }
    }
    return moments
}
