package com.frameout.engine.work

import android.app.NotificationManager
import android.content.Context
import android.os.SystemClock

object ImportNotice {
    const val ACTION_CANCEL = "com.frameout.IMPORT_CANCEL"
    const val NOTIFICATION_ID = 4105
    private const val INTERVAL_MS = 700L

    class Snapshot(val done: Long, val total: Long, val elapsedMs: Long)

    @Volatile private var cancelHook: (() -> Unit)? = null
    private val lock = Any()
    private var total = 0L
    private var done = 0L
    private var startedAt = 0L
    private var lastPost = 0L

    fun setCancelHook(hook: (() -> Unit)?) {
        cancelHook = hook
    }

    fun cancel() {
        cancelHook?.invoke()
    }

    fun begin(totalBytes: Long) = synchronized(lock) {
        total = totalBytes.coerceAtLeast(0L)
        done = 0L
        lastPost = 0L
        startedAt = SystemClock.elapsedRealtime()
    }

    fun advance(context: Context, bytes: Int) {
        val snap = synchronized(lock) {
            done += bytes
            val now = SystemClock.elapsedRealtime()
            if (now - lastPost < INTERVAL_MS) return
            lastPost = now
            Snapshot(done, total, now - startedAt)
        }
        runCatching {
            context.getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, ImportNotification.build(context, snap))
        }
    }
}
