package com.frameout.engine.repair

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import com.frameout.core.util.logFailure
import java.io.File
import java.nio.ByteBuffer

internal class SalvageStats(val kept: Int, val droppedUs: Long, val lastPtsUs: Long, val declaredUs: Long) {
    val truncated: Boolean get() = declaredUs > 0L && lastPtsUs + droppedUs < declaredUs * TRUNCATION_RATIO / 100
    private companion object { const val TRUNCATION_RATIO = 95L }
}

internal class SalvageRemuxer(private val context: Context) {
    private class Layout(val map: Map<Int, Int>, val video: Int, val declaredUs: Long, var buffer: ByteBuffer)

    private class Cursor(var kept: Int = 0, var lastUs: Long = 0L, var seenSync: Boolean = false)

    fun run(source: Uri, out: File, requested: DropRanges): SalvageStats? {
        val drops = SalvageScan.effectiveDrops(context, source, requested)
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        return try {
            extractor.setDataSource(context, source, null)
            muxer = MediaMuxer(out.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val layout = layoutOf(extractor, muxer) ?: return null
            muxer.start()
            val cursor = pump(extractor, muxer, layout, drops)
            if (cursor.kept == 0) return null
            muxer.stop()
            SalvageStats(cursor.kept, drops.totalUs.coerceAtMost(layout.declaredUs), cursor.lastUs, layout.declaredUs)
        } catch (e: Throwable) {
            logFailure("SalvageRemuxer", e)
            null
        } finally {
            runCatching { muxer?.release() }
            runCatching { extractor.release() }
        }
    }

    private fun layoutOf(extractor: MediaExtractor, muxer: MediaMuxer): Layout? {
        val map = linkedMapOf<Int, Int>()
        var video = -1
        var rotation = 0
        var declared = 0L
        var maxBytes = DEFAULT_BUFFER
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
            if (!mime.startsWith("video/") && !mime.startsWith("audio/")) continue
            if (mime.startsWith("video/") && video < 0) {
                video = i
                if (format.containsKey(MediaFormat.KEY_ROTATION)) rotation = format.getInteger(MediaFormat.KEY_ROTATION)
            }
            if (format.containsKey(MediaFormat.KEY_DURATION)) declared = maxOf(declared, format.getLong(MediaFormat.KEY_DURATION))
            if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) maxBytes = maxOf(maxBytes, format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE))
            map[i] = muxer.addTrack(format)
        }
        if (video < 0) return null
        muxer.setOrientationHint(((rotation % 360) + 360) % 360)
        return Layout(map, video, declared, ByteBuffer.allocate(maxBytes.coerceAtMost(MAX_BUFFER)))
    }

    private fun pump(extractor: MediaExtractor, muxer: MediaMuxer, layout: Layout, drops: DropRanges): Cursor {
        val info = MediaCodec.BufferInfo()
        val cursor = Cursor()
        layout.map.keys.forEach(extractor::selectTrack)
        try {
            while (true) {
                val track = extractor.sampleTrackIndex
                val time = extractor.sampleTime
                if (track < 0 || time < 0L) break
                if (wanted(layout, cursor, drops, track, time, extractor) && !emit(extractor, muxer, layout, cursor, info, track, time, drops)) break
                if (!extractor.advance()) break
            }
        } catch (e: Exception) {
            logFailure("SalvageRemuxer", e)
        }
        return cursor
    }

    private fun wanted(layout: Layout, cursor: Cursor, drops: DropRanges, track: Int, time: Long, extractor: MediaExtractor): Boolean {
        if (track !in layout.map || drops.contains(time)) return false
        if (track != layout.video) return true
        val sync = extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0
        if (!cursor.seenSync && !sync) return false
        cursor.seenSync = true
        return true
    }

    private fun emit(
        extractor: MediaExtractor,
        muxer: MediaMuxer,
        layout: Layout,
        cursor: Cursor,
        info: MediaCodec.BufferInfo,
        track: Int,
        time: Long,
        drops: DropRanges
    ): Boolean {
        val size = extractor.sampleSize
        if (size < 0L) return false
        if (size == 0L || size > MAX_BUFFER) return true
        if (size > layout.buffer.capacity()) layout.buffer = ByteBuffer.allocate(size.toInt())
        layout.buffer.clear()
        val read = extractor.readSampleData(layout.buffer, 0)
        if (read < 0) return false
        val sync = extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0
        info.set(0, read, (time - drops.shiftAt(time)).coerceAtLeast(0L), if (sync) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0)
        muxer.writeSampleData(layout.map.getValue(track), layout.buffer, info)
        cursor.kept++
        cursor.lastUs = maxOf(cursor.lastUs, info.presentationTimeUs)
        return true
    }

    private companion object {
        const val DEFAULT_BUFFER = 1024 * 1024
        const val MAX_BUFFER = 64 * 1024 * 1024
    }
}
