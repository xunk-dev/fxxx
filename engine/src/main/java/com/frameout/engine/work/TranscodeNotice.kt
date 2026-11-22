package com.frameout.engine.work

import android.app.ActivityManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.TranscodeStrings
import com.frameout.engine.R

object TranscodeNotice {
    const val ACTION_CANCEL = "com.frameout.TRANSCODE_CANCEL"
    private const val READY_ID = 4104
    private const val BYTES_PER_MB = 1048576.0

    @Volatile private var cancelHook: (() -> Unit)? = null

    class Progress(val projectId: Long, val frameCount: Int, val fraction: Float, val elapsedMs: Long)

    fun setCancelHook(hook: (() -> Unit)?) {
        cancelHook = hook
    }

    fun cancel() {
        cancelHook?.invoke()
    }

    fun post(context: Context, progress: Progress) {
        runCatching {
            context.getSystemService(NotificationManager::class.java).notify(ExtractNotifications.TRANSCODE_ID, TranscodeNotification.build(context, progress))
        }
    }

    fun clear(context: Context) {
        runCatching { context.getSystemService(NotificationManager::class.java).cancel(ExtractNotifications.TRANSCODE_ID) }
    }

    fun ready(context: Context, uri: Uri, mime: String, sizeBytes: Long) {
        if (inForeground()) return
        ExtractNotifications.ensureChannel(context)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val view = Intent(Intent.ACTION_VIEW).setDataAndType(uri, mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        val send = Intent(Intent.ACTION_SEND).setType(mime).putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val chooser = Intent.createChooser(send, FrameStrings.share()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val notification = NotificationCompat.Builder(context, ExtractNotifications.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(NotificationTheme.color(context))
            .setContentTitle(TranscodeStrings.ready())
            .setContentText(FrameStrings.exportedMb("%.1f".format(sizeBytes / BYTES_PER_MB)))
            .setContentIntent(PendingIntent.getActivity(context, 0, view, flags))
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .addAction(0, FrameStrings.share(), PendingIntent.getActivity(context, 1, chooser, flags))
            .build()
        runCatching { context.getSystemService(NotificationManager::class.java).notify(READY_ID, notification) }
    }

    private fun inForeground(): Boolean {
        val info = ActivityManager.RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(info)
        return info.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
    }
}
