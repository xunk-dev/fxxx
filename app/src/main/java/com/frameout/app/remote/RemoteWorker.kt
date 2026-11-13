package com.frameout.app.remote

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import java.util.concurrent.TimeUnit

class RemoteWorker(private val ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val m = RemoteSync.sync(ctx) ?: return Result.success()
        val store = RemoteStore(ctx)
        if (!NotificationManagerCompat.from(ctx).areNotificationsEnabled()) return Result.success()
        m.promos.filter { it.isActive() && !store.shown(it.id) }.forEach { show(it); store.markShown(it.id) }
        return Result.success()
    }

    private fun show(n: Promo) {
        val nm = NotificationManagerCompat.from(ctx)
        nm.createNotificationChannel(
            NotificationChannelCompat.Builder(CH, NotificationManagerCompat.IMPORTANCE_DEFAULT).setName(com.frameout.core.util.FrameStrings.announcementsChannel()).build()
        )
        val intent = n.link?.let { Intent(Intent.ACTION_VIEW, Uri.parse(it)) }
            ?: ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
        val pi = PendingIntent.getActivity(ctx, n.id.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val b = NotificationCompat.Builder(ctx, CH).setSmallIcon(com.frameout.engine.R.drawable.ic_notification)
            .setContentTitle(n.title).setContentText(n.body).setAutoCancel(true).setContentIntent(pi)
            .setStyle(NotificationCompat.BigTextStyle().bigText(n.body))
        if (n.media != null && n.sha != null) RemoteSync.media(ctx, n.media, n.sha)?.let { f ->
            BitmapFactory.decodeFile(f.path)?.let { b.setLargeIcon(it).setStyle(NotificationCompat.BigPictureStyle().bigPicture(it)) }
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        try {
            nm.notify(n.id.hashCode(), b.build())
        } catch (e: SecurityException) {
        }
    }

    companion object {
        private const val CH = "remote_notices"
        fun schedule(ctx: Context) {
            val req = PeriodicWorkRequestBuilder<RemoteWorker>(6, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build()
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork("remote_content", ExistingPeriodicWorkPolicy.KEEP, req)
        }
    }
}
