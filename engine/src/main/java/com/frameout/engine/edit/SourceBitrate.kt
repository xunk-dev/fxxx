package com.frameout.engine.edit

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import com.frameout.core.util.logFailure

internal object SourceBitrate {
    private const val HEADROOM_PCT = 120L
    private const val MIN_BPS = 1_000_000L
    private const val MAX_BPS = 200_000_000L
    private const val ASSUMED_AUDIO_BPS = 160_000L

    fun target(sourceBps: Long): Int = (sourceBps * HEADROOM_PCT / 100L).coerceIn(MIN_BPS, MAX_BPS).toInt()

    fun estimate(sizeBytes: Long, durationUs: Long, audioBps: Long): Long {
        if (sizeBytes <= 0L || durationUs <= 0L) return 0L
        val total = sizeBytes * 8L * 1_000_000L / durationUs
        return (total - audioBps.coerceAtLeast(0L)).coerceAtLeast(0L)
    }

    fun videoBps(context: Context, uri: Uri): Long? {
        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(context, uri, null)
            probe(context, uri, extractor)
        } catch (e: Throwable) {
            logFailure("SourceBitrate", e)
            null
        } finally {
            runCatching { extractor.release() }
        }
    }

    private fun probe(context: Context, uri: Uri, extractor: MediaExtractor): Long? {
        var video: MediaFormat? = null
        var audioBps = 0L
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
            if (mime.startsWith("video/") && video == null) video = format
            if (mime.startsWith("audio/")) audioBps += format.bitrateOrZero().let { if (it > 0L) it else ASSUMED_AUDIO_BPS }
        }
        val track = video ?: return null
        val declared = track.bitrateOrZero()
        val size = runCatching { context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } }.getOrNull() ?: 0L
        val durationUs = if (track.containsKey(MediaFormat.KEY_DURATION)) track.getLong(MediaFormat.KEY_DURATION) else 0L
        return maxOf(declared, estimate(size, durationUs, audioBps)).takeIf { it > 0L }
    }

    private fun MediaFormat.bitrateOrZero(): Long =
        if (containsKey(MediaFormat.KEY_BIT_RATE)) getInteger(MediaFormat.KEY_BIT_RATE).toLong().coerceAtLeast(0L) else 0L
}
