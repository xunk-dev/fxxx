package com.frameout.engine.work

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.TranscodeStrings
import com.frameout.engine.R

internal object TranscodeNotification {
    private const val STEPS = 1000
    private const val MIN_FRACTION_FOR_ETA = 0.05f

    fun build(context: Context, progress: TranscodeNotice.Progress?): Notification {
        ExtractNotifications.ensureChannel(context)
        val known = progress != null && progress.fraction >= 0f
        val fraction = if (known) progress.fraction.coerceIn(0f, 1f) else 0f
        val builder = NotificationCompat.Builder(context, ExtractNotifications.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(NotificationTheme.color(context))
            .setContentTitle(title(fraction, known))
            .setContentText(details(progress, fraction, known))
            .setContentIntent(ExtractNotifications.launchIntent(context))
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setProgress(STEPS, (fraction * STEPS).toInt(), !known)
            .addAction(0, FrameStrings.cancel(), cancelIntent(context))
        progress?.takeIf { it.projectId >= 0 }?.let { ExtractNotifications.thumbnailOf(context, it.projectId) }?.let { builder.setLargeIcon(it) }
        return builder.build()
    }

    private fun title(fraction: Float, known: Boolean): String =
        if (known) listOf(TranscodeStrings.exporting(), "${(fraction * 100).toInt()}%").joinToString(FrameStrings.sep()) else TranscodeStrings.exporting()

    private fun details(progress: TranscodeNotice.Progress?, fraction: Float, known: Boolean): String {
        if (progress == null || !known) return FrameStrings.working()
        val parts = ArrayList<String>(2)
        if (progress.frameCount > 0) parts += FrameStrings.exportProgress((progress.frameCount * fraction).toInt(), progress.frameCount)
        if (fraction >= MIN_FRACTION_FOR_ETA && fraction < 1f) {
            val left = (progress.elapsedMs * (1f - fraction) / fraction).toLong()
            parts += FrameStrings.remaining(ExtractNotifications.formatTime(left))
        }
        return parts.joinToString(FrameStrings.sep()).ifEmpty { FrameStrings.working() }
    }

    private fun cancelIntent(context: Context): PendingIntent {
        val intent = Intent(TranscodeNotice.ACTION_CANCEL).setPackage(context.packageName)
        return PendingIntent.getBroadcast(context, TranscodeNotice.ACTION_CANCEL.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}
