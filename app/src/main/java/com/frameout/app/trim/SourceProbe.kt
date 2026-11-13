package com.frameout.app.trim

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class SourceProbe(val timeline: FrameTimeline, val fps: Float) {
    companion object {
        private const val MAX_SAMPLES = 1_500_000
        private const val TIMEOUT_MS = 12_000L
        private const val CHECK_MASK = 1023
        private val cache = object : LinkedHashMap<String, SourceProbe>(8, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, SourceProbe>?) = size > 4
        }

        suspend fun load(context: Context, uri: Uri): SourceProbe? {
            val key = uri.toString()
            synchronized(cache) { cache[key] }?.let { return it }
            val probe = withTimeoutOrNull(TIMEOUT_MS + 1_000L) { withContext(Dispatchers.IO) { scan(context, uri) } }
            if (probe != null) synchronized(cache) { cache[key] = probe }
            return probe
        }

        private fun scan(context: Context, uri: Uri): SourceProbe? {
            val extractor = MediaExtractor()
            return try {
                extractor.setDataSource(context, uri, null)
                val track = (0 until extractor.trackCount).firstOrNull {
                    extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true
                } ?: return null
                val fps = readFps(extractor.getTrackFormat(track))
                extractor.selectTrack(track)
                readTimeline(extractor)?.let { SourceProbe(it, fps) }
            } catch (e: Exception) {
                null
            } finally {
                runCatching { extractor.release() }
            }
        }

        private fun readFps(format: MediaFormat): Float {
            if (!format.containsKey(MediaFormat.KEY_FRAME_RATE)) return 0f
            return runCatching { format.getInteger(MediaFormat.KEY_FRAME_RATE).toFloat() }
                .recoverCatching { format.getFloat(MediaFormat.KEY_FRAME_RATE) }
                .getOrDefault(0f)
        }

        private fun readTimeline(extractor: MediaExtractor): FrameTimeline? {
            val builder = FrameTimeline.Builder(MAX_SAMPLES)
            val deadline = SystemClock.elapsedRealtime() + TIMEOUT_MS
            var visited = 0
            while (true) {
                val time = extractor.sampleTime
                if (time < 0L) break
                val sync = extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0
                if (!builder.add(time, sync)) return null
                if (++visited and CHECK_MASK == 0 && SystemClock.elapsedRealtime() > deadline) return null
                if (!extractor.advance()) break
            }
            return builder.build()
        }
    }
}

@Composable
internal fun rememberSourceProbe(uri: Uri): SourceProbe? {
    val context = LocalContext.current.applicationContext
    val probe by produceState<SourceProbe?>(null, uri) { value = SourceProbe.load(context, uri) }
    return probe
}

@Composable
internal fun TrimEnv.frameEstimate(stored: String): Int {
    val probe = rememberSourceProbe(source)
    val range = clamped
    val duration = player.durationMs
    return remember(probe, stored, duration, range) { estimateFrames(duration, range, stored, probe) }
}
