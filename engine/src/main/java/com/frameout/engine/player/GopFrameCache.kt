package com.frameout.engine.player

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.frameout.core.util.logged
import com.frameout.engine.decode.FrameDecoder

internal class GopFrameCache(private val context: Context, private val uri: Uri) {
    private val lock = Any()
    private val frames = LinkedHashMap<Long, Bitmap>()
    private var bytes = 0L
    @Volatile private var coveredFromUs = Long.MAX_VALUE
    @Volatile private var coveredToUs = Long.MIN_VALUE
    @Volatile private var warmingMs = NO_POSITION

    fun get(ptsUs: Long): Bitmap? = synchronized(lock) { frames[ptsUs] }

    fun isWarmingAt(positionMs: Long): Boolean = warmingMs == positionMs

    fun covers(displayedUs: Long): Boolean {
        val from = coveredFromUs
        val to = coveredToUs
        if (to < from) return false
        return displayedUs in (from + (to - from) / EDGE_DIVISOR)..to
    }

    fun clear() {
        synchronized(lock) {
            frames.clear()
            bytes = 0L
        }
        coveredFromUs = Long.MAX_VALUE
        coveredToUs = Long.MIN_VALUE
        warmingMs = NO_POSITION
    }

    fun warm(index: LongArray, positionMs: Long, isActive: () -> Boolean) {
        val (startUs, endUs) = FramePtsIndex.window(index, positionMs, BACK_FRAMES, FORWARD_FRAMES) ?: return
        clear()
        warmingMs = positionMs
        val decoder = FrameDecoder(context)
        try {
            decoder.open(uri)
            decoder.seekToKeyframe(startUs)
            while (isActive()) {
                val raw = decoder.nextOutput() ?: break
                if (raw.ptsUs > endUs) {
                    decoder.drop(raw)
                    break
                }
                if (raw.ptsUs < startUs) {
                    decoder.drop(raw)
                    continue
                }
                val frame = decoder.convert(raw) ?: continue
                store(frame.first, shrink(frame.second))
            }
            if (isActive()) {
                coveredFromUs = startUs
                coveredToUs = endUs
            }
        } catch (e: OutOfMemoryError) {
            clear()
        } catch (e: Exception) {
            runCatching { throw e }.logged("GopFrameCache")
            clear()
        } finally {
            runCatching { decoder.close() }
        }
    }

    private fun store(ptsUs: Long, bitmap: Bitmap) {
        synchronized(lock) {
            frames.put(ptsUs, bitmap)?.let { bytes -= it.allocationByteCount }
            bytes += bitmap.allocationByteCount
            val iterator = frames.entries.iterator()
            while (bytes > BUDGET_BYTES && iterator.hasNext()) {
                val eldest = iterator.next()
                bytes -= eldest.value.allocationByteCount
                iterator.remove()
            }
        }
    }

    private fun shrink(bitmap: Bitmap): Bitmap {
        val (w, h) = HdrPreviewPolicy.scaledSize(bitmap.width, bitmap.height, MAX_EDGE)
        return if (w == bitmap.width && h == bitmap.height) bitmap else Bitmap.createScaledBitmap(bitmap, w, h, true)
    }

    companion object {
        const val BACK_FRAMES = 40
        const val FORWARD_FRAMES = 12
        private const val MAX_EDGE = 640
        private const val BUDGET_BYTES = 56L * 1024 * 1024
        private const val EDGE_DIVISOR = 4L
    }
}
