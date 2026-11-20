package com.frameout.engine.player

import android.content.Context
import android.media.MediaExtractor
import android.net.Uri
import com.frameout.codecpack.FormatKind
import com.frameout.codecpack.FormatProbe
import com.frameout.codecpack.VideoTrackSelector
import com.frameout.core.util.logged

object FramePtsIndex {
    private const val CHECK_EVERY = 2048

    fun build(context: Context, uri: Uri, isActive: () -> Boolean): LongArray? = runCatching {
        if (FormatProbe.probe(context, uri).kind == FormatKind.NeedsFfmpeg) return null
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, uri, null)
            val track = VideoTrackSelector.select(extractor)
            if (track < 0) return null
            extractor.selectTrack(track)
            var buffer = LongArray(4096)
            var size = 0
            while (true) {
                val pts = extractor.sampleTime
                if (pts < 0L) break
                if (size == buffer.size) buffer = buffer.copyOf(size * 2)
                buffer[size++] = pts
                if (size % CHECK_EVERY == 0 && !isActive()) return null
                if (!extractor.advance()) break
            }
            if (size == 0) null else buffer.copyOf(size).also { it.sort() }
        } finally {
            extractor.release()
        }
    }.logged("FramePtsIndex").getOrNull()

    fun lowerBound(index: LongArray, valueUs: Long): Int {
        var lo = 0
        var hi = index.size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (index[mid] < valueUs) lo = mid + 1 else hi = mid
        }
        return lo
    }

    fun upperBound(index: LongArray, valueUs: Long): Int {
        var lo = 0
        var hi = index.size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (index[mid] <= valueUs) lo = mid + 1 else hi = mid
        }
        return lo
    }

    fun stepUs(index: LongArray, currentMs: Long, forward: Boolean, count: Int): Long? {
        val frames = count.coerceAtLeast(1)
        return if (forward) {
            val after = upperBound(index, ExactPts.limitUs(currentMs))
            if (after >= index.size) null else index[minOf(index.size - 1, after + frames - 1)]
        } else {
            val before = lowerBound(index, currentMs.coerceAtLeast(0L) * 1000L)
            if (before == 0) null else index[maxOf(0, before - frames)]
        }
    }

    fun step(index: LongArray, currentMs: Long, forward: Boolean, count: Int): Long? =
        stepUs(index, currentMs, forward, count)?.let { it / 1000L }

    fun displayedUs(index: LongArray, positionMs: Long): Long {
        if (index.isEmpty()) return 0L
        val shown = (upperBound(index, ExactPts.limitUs(positionMs)) - 1).coerceAtLeast(0)
        return index[shown]
    }

    fun window(index: LongArray, positionMs: Long, back: Int, fwd: Int): Pair<Long, Long>? {
        if (index.isEmpty()) return null
        val shown = (upperBound(index, ExactPts.limitUs(positionMs)) - 1).coerceAtLeast(0)
        return index[maxOf(0, shown - back)] to index[minOf(index.size - 1, shown + fwd)]
    }
}
