package com.frameout.engine.player

import android.graphics.Bitmap
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class FramePrecapture(private val scope: CoroutineScope) {
    private class Slot(val atMs: Long) {
        val result = CompletableDeferred<Bitmap?>()
        var job: Job? = null
        var dead = false
    }

    private val lock = Any()
    private var current: Slot? = null

    fun request(positionMs: Long, grab: suspend () -> Bitmap?) {
        synchronized(lock) {
            if (current?.atMs == positionMs) return
            discardLocked()
            val slot = Slot(positionMs)
            current = slot
            slot.job = scope.launch(Dispatchers.IO) {
                var bitmap: Bitmap? = null
                try {
                    delay(SETTLE_MS)
                    bitmap = try { grab() } catch (e: OutOfMemoryError) { null }
                    if (bitmap != null && bitmap.allocationByteCount > MAX_BYTES) {
                        bitmap.recycle()
                        bitmap = null
                    }
                } finally {
                    synchronized(lock) {
                        if (slot.dead) {
                            bitmap?.recycle()
                            bitmap = null
                        }
                        slot.result.complete(bitmap)
                    }
                }
            }
        }
    }

    suspend fun take(positionMs: Long): Bitmap? {
        val slot = synchronized(lock) {
            val s = current ?: return null
            if (s.atMs != positionMs || s.dead) return null
            current = null
            s
        }
        val bitmap = slot.result.await()
        return bitmap?.takeIf { !it.isRecycled }
    }

    fun clear() = synchronized(lock) { discardLocked() }

    private fun discardLocked() {
        val slot = current ?: return
        current = null
        slot.dead = true
        slot.job?.cancel()
        if (slot.result.isCompleted) {
            @Suppress("OPT_IN_USAGE")
            runCatching { slot.result.getCompleted() }.getOrNull()?.let { if (!it.isRecycled) it.recycle() }
        }
    }

    private companion object {
        const val SETTLE_MS = 350L
        const val MAX_BYTES = 72 * 1024 * 1024
    }
}
