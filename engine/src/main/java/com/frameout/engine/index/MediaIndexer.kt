package com.frameout.engine.index

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.os.Build
import android.net.Uri
import androidx.room3.withWriteTransaction
import com.frameout.core.db.AppDatabase
import com.frameout.core.db.VideoIndexDao
import com.frameout.core.db.VideoIndexEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

class MediaIndexer(private val context: Context, private val db: AppDatabase = AppDatabase.get(context)) {
    private companion object {
        const val CHUNK = 2000
        const val DEFAULT_FPS = 30
        const val CHECK_MASK = 255
        const val PROGRESS_MASK = 1023
    }


    suspend fun probe(uri: Uri): SourceProbe? = withContext(Dispatchers.IO) {
        runCatching {
            val extractor = open(uri)
            try {
                val track = selectVideoTrack(extractor)
                if (track < 0) return@runCatching null
                extractor.selectTrack(track)
                val meta = readMeta(extractor, track)
                val first = extractor.sampleTime.coerceAtLeast(0L)
                val frames = meta.estimatedFrames.takeIf { it > 0 } ?: fallbackEstimate(uri, meta.durationUs)
                SourceProbe(meta.copy(frameCount = frames, estimatedFrames = frames), first)
            } finally {
                extractor.release()
            }
        }.getOrNull()
    }

    suspend fun indexToDatabase(uri: Uri, projectId: Long, dao: VideoIndexDao, isCancelled: () -> Boolean, isPaused: () -> Boolean, startMs: Long = 0L, endMs: Long = 0L, onProgress: (Int, Int) -> Unit = { _, _ -> }): VideoIndexMetadata = withContext(Dispatchers.IO) {
        val extractor = open(uri)
        try {
            runCatching { dao.clearIndex(-projectId) }
            val track = selectVideoTrack(extractor)
            if (track < 0) throw com.frameout.codecpack.MediaFailureException(com.frameout.codecpack.MediaFailure.NoVideoStream, "No video track")
            extractor.selectTrack(track)
            val meta = readMeta(extractor, track)
            val estimate = meta.estimatedFrames.takeIf { it > 0 } ?: fallbackEstimate(uri, meta.durationUs)
            val window = ScanWindow.of(startMs, endMs, meta.durationUs)
            val scanned = scanSamples(extractor, window?.scaleEstimate(estimate, meta.durationUs) ?: estimate, window, isCancelled, isPaused, onProgress)
            val scan = scanned.first
            db.withWriteTransaction {
                dao.clearIndex(projectId)
                writeOrdered(scan, projectId, dao)
            }
            meta.copy(frameCount = scan.size, estimatedFrames = estimate, window = scanned.second)
        } finally {
            extractor.release()
        }
    }

    suspend fun storedMetadata(uri: Uri, frameCount: Int): VideoIndexMetadata? = withContext(Dispatchers.IO) {
        runCatching {
            val extractor = open(uri)
            try {
                val track = selectVideoTrack(extractor)
                if (track < 0) null else readMeta(extractor, track).copy(frameCount = frameCount, estimatedFrames = frameCount)
            } finally {
                extractor.release()
            }
        }.getOrNull()
    }

    private suspend fun scanSamples(extractor: MediaExtractor, estimate: Int, requested: ScanWindow?, isCancelled: () -> Boolean, isPaused: () -> Boolean, onProgress: (Int, Int) -> Unit): Pair<SampleScan, ScanWindow?> {
        val scan = SampleScan()
        val window = requested?.takeIf { seekToWindow(extractor, it) }
        if (extractor.sampleTrackIndex < 0) return scan to window
        do {
            if (scan.size and CHECK_MASK == 0) {
                currentCoroutineContext().ensureActive()
                if (isCancelled()) throw CancellationException()
                while (isPaused() && !isCancelled()) { currentCoroutineContext().ensureActive(); delay(100) }
            }
            val sampleTime = extractor.sampleTime
            if (window != null && scan.size > 0 && window.isPastEnd(sampleTime)) break
            scan.add(sampleTime, extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0)
            if (scan.size == 1 || scan.size and PROGRESS_MASK == 0) onProgress(scan.size, estimate.coerceAtLeast(scan.size))
        } while (extractor.advance())
        onProgress(scan.size, scan.size)
        return scan to window
    }

    private fun seekToWindow(extractor: MediaExtractor, window: ScanWindow): Boolean {
        if (window.startUs <= 0L) return true
        extractor.seekTo(window.startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
        if (extractor.sampleTrackIndex >= 0 && extractor.sampleTime >= 0L) return true
        extractor.seekTo(0L, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
        return false
    }

    private suspend fun writeOrdered(scan: SampleScan, projectId: Long, dao: VideoIndexDao) {
        val batch = ArrayList<VideoIndexEntity>(CHUNK)
        scan.forEachDisplay { entity -> batch += entity.copy(projectId = projectId); if (batch.size == CHUNK) flush(batch, dao) }
        if (batch.isNotEmpty()) flush(batch, dao)
    }

    private suspend fun flush(batch: ArrayList<VideoIndexEntity>, dao: VideoIndexDao) {
        dao.insertAll(batch.toList())
        batch.clear()
    }

    private fun fallbackEstimate(uri: Uri, durationUs: Long): Int = runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val counted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT)?.toIntOrNull() else null
            val duration = if (durationUs > 0L) durationUs else (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L) * 1000L
            counted?.takeIf { it > 0 } ?: if (duration > 0L) (duration / 1_000_000.0 * DEFAULT_FPS).toInt() else 0
        } finally {
            retriever.release()
        }
    }.getOrDefault(0)

    private fun open(uri: Uri): MediaExtractor = com.frameout.engine.decode.SystemSource.open(context, uri)

    private fun readMeta(extractor: MediaExtractor, track: Int): VideoIndexMetadata {
        val format = extractor.getTrackFormat(track)
        val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
        val width = if (format.containsKey(MediaFormat.KEY_WIDTH)) format.getInteger(MediaFormat.KEY_WIDTH) else 0
        val height = if (format.containsKey(MediaFormat.KEY_HEIGHT)) format.getInteger(MediaFormat.KEY_HEIGHT) else 0
        val duration = if (format.containsKey(MediaFormat.KEY_DURATION)) format.getLong(MediaFormat.KEY_DURATION) else 0L
        val fps = if (format.containsKey(MediaFormat.KEY_FRAME_RATE)) runCatching { format.getInteger(MediaFormat.KEY_FRAME_RATE) }.getOrElse { runCatching { format.getFloat(MediaFormat.KEY_FRAME_RATE).toInt() }.getOrDefault(0) } else 0
        val estimate = if (duration > 0 && fps > 0) ((duration / 1_000_000.0) * fps).toInt() else 0
        return VideoIndexMetadata(duration, width, height, mime, estimate, estimate)
    }

    private fun selectVideoTrack(extractor: MediaExtractor): Int = com.frameout.codecpack.VideoTrackSelector.select(extractor)
}

data class SourceProbe(val meta: VideoIndexMetadata, val firstPtsUs: Long)

data class VideoIndexMetadata(
    val durationUs: Long,
    val width: Int,
    val height: Int,
    val mime: String,
    val frameCount: Int,
    val estimatedFrames: Int = frameCount,
    val window: ScanWindow? = null
)
