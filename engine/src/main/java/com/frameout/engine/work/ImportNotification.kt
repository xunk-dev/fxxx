package com.frameout.engine.work

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.text.format.Formatter
import androidx.core.app.NotificationCompat
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.TranscodeStrings
import com.frameout.engine.R

internal object ImportNotification {
    private const val STEPS = 1000
    private const val MIN_FRACTION_FOR_ETA = 0.05f

    fun build(context: Context, snap: ImportNotice.Snapshot?): Notification {
        ExtractNotifications.ensureChannel(context)
        val known = snap != null && snap.total > 0L
        val fraction = if (known) (snap.done.toFloat() / snap.total).coerceIn(0f, 1f) else 0f
        val title = if (known) listOf(TranscodeStrings.importing(), "${(fraction * 100).toInt()}%").joinToString(FrameStrings.sep()) else TranscodeStrings.importing()
        return NotificationCompat.Builder(context, ExtractNotifications.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(NotificationTheme.color(context))
            .setContentTitle(title)
            .setContentText(details(context, snap, fraction, known))
            .setContentIntent(ExtractNotifications.launchIntent(context))
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setProgress(STEPS, (fraction * STEPS).toInt(), !known)
            .addAction(0, FrameStrings.cancel(), cancelIntent(context))
            .build()
    }

    private fun details(context: Context, snap: ImportNotice.Snapshot?, fraction: Float, known: Boolean): String {
        if (snap == null || !known) return FrameStrings.working()
        val parts = ArrayList<String>(2)
        parts += "${Formatter.formatFileSize(context, snap.done)} / ${Formatter.formatFileSize(context, snap.total)}"
        if (fraction >= MIN_FRACTION_FOR_ETA && fraction < 1f) {
            parts += FrameStrings.remaining(ExtractNotifications.formatTime((snap.elapsedMs * (1f - fraction) / fraction).toLong()))
        }
        return parts.joinToString(FrameStrings.sep())
    }

    private fun cancelIntent(context: Context): PendingIntent {
        val intent = Intent(ImportNotice.ACTION_CANCEL).setPackage(context.packageName)
        return PendingIntent.getBroadcast(context, ImportNotice.ACTION_CANCEL.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}
