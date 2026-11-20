package com.frameout.engine.repair

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri

internal object SalvageScan {
    private const val OPEN_END_US = Long.MAX_VALUE / 4

    fun effectiveDrops(context: Context, source: Uri, requested: DropRanges): DropRanges {
        if (requested.isEmpty) return requested
        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(context, source, null)
            val video = (0 until extractor.trackCount).firstOrNull { isVideo(extractor.getTrackFormat(it)) }
                ?: return requested
            extractor.selectTrack(video)
            walk(extractor, requested)
        } catch (e: Exception) {
            requested
        } finally {
            runCatching { extractor.release() }
        }
    }

    private fun isVideo(format: MediaFormat): Boolean =
        format.getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true

    private fun walk(extractor: MediaExtractor, requested: DropRanges): DropRanges {
        val result = DropRanges()
        var dropStart = -1L
        while (true) {
            val time = extractor.sampleTime
            if (time < 0L) break
            val sync = extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0
            val inside = requested.contains(time)
            if (inside) dropStart = if (dropStart < 0L) time else minOf(dropStart, time)
            if (dropStart >= 0L && sync && !inside) {
                result.add(dropStart, time)
                dropStart = -1L
            }
            if (!extractor.advance()) break
        }
        if (dropStart >= 0L) result.add(dropStart, OPEN_END_US)
        return result
    }
}
