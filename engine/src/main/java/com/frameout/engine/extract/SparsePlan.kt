package com.frameout.engine.extract

import com.frameout.core.quality.content.ContentSpec
import com.frameout.engine.index.SampleInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

internal class SparseSelection(private val targets: LongArray, private val seekEachTime: Boolean) {
    val count: Int get() = targets.size

    fun sample(i: Int): SampleInfo = SampleInfo(i, targets[i], false, if (seekEachTime) ALWAYS_SEEK else 0, approximate = true)

    fun flow(afterIndex: Int): Flow<SampleInfo> = flow {
        for (i in (afterIndex + 1).coerceAtLeast(0) until targets.size) emit(sample(i))
    }

    companion object {
        const val ALWAYS_SEEK = Int.MAX_VALUE / 2
    }
}

internal object SparsePlan {
    const val MIN_STEP_FRAMES = 3
    const val FIXED_MIN_STEP_FRAMES = 300L
    private const val DECODE_THROUGH_MAX_US = 1_000_000L
    private const val MAX_TARGETS = 2_000_000L

    fun plan(mode: ExtractMode, startUs: Long, endUs: Long, trackDurationUs: Long, firstPtsUs: Long, trackFrames: Int): SparseSelection? {
        if (trackDurationUs <= 0L || trackFrames <= 0 || endUs <= startUs) return null
        val frameUs = (trackDurationUs / trackFrames).coerceAtLeast(1L)
        return when (mode) {
            is ExtractMode.FixedCount -> fixed(mode.count.coerceAtLeast(1), startUs, endUs, firstPtsUs, frameUs)
            is ExtractMode.EveryNMillis -> stepped(mode.millis.coerceAtLeast(1L) * 1000L, startUs, endUs, frameUs)
            is ExtractMode.Content -> stepped(ContentSpec.SAMPLE_STEP_MS * 1000L, startUs, endUs, frameUs)
            else -> null
        }
    }

    private fun fixed(count: Int, startUs: Long, endUs: Long, firstPtsUs: Long, frameUs: Long): SparseSelection? {
        val rangeFrames = (endUs - startUs) / frameUs
        if (count.toLong() * FIXED_MIN_STEP_FRAMES > rangeFrames) return null
        val first = maxOf(firstPtsUs, startUs)
        val last = maxOf(first, endUs - frameUs)
        val targets = LongArray(count) { if (count == 1) first else first + (last - first) * it / (count - 1) }
        return SparseSelection(targets, seekEachTime = true)
    }

    private fun stepped(stepUs: Long, startUs: Long, endUs: Long, frameUs: Long): SparseSelection? {
        if (stepUs < MIN_STEP_FRAMES * frameUs) return null
        val count = (endUs - startUs + stepUs - 1) / stepUs
        if (count <= 0L || count > MAX_TARGETS) return null
        val targets = LongArray(count.toInt()) { startUs + it * stepUs }
        return SparseSelection(targets, seekEachTime = stepUs > DECODE_THROUGH_MAX_US)
    }
}
