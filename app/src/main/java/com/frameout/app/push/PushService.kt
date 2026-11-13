package com.frameout.app.push

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.net.URL

class PushService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: return
        val body = message.notification?.body ?: message.data["body"].orEmpty()
        val link = message.data["link"]?.takeIf {
            it.startsWith("https://") && runCatching { URL(it).host == "play.google.com" }.getOrDefault(false)
        }
        show(title, body, link)
    }

    private fun show(title: String, body: String, link: String?) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val nm = NotificationManagerCompat.from(this)
        nm.createNotificationChannel(
            NotificationChannelCompat.Builder(Push.CHANNEL, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(com.frameout.core.util.FrameStrings.announcementsChannel()).build()
        )
        val intent = link?.let { Intent(Intent.ACTION_VIEW, Uri.parse(it)) }
            ?: packageManager.getLaunchIntentForPackage(packageName) ?: return
        val id = (System.currentTimeMillis() and 0x7fffffff).toInt()
        val pi = PendingIntent.getActivity(this, id, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(this, Push.CHANNEL)
            .setSmallIcon(com.frameout.engine.R.drawable.ic_notification)
            .setContentTitle(title).setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true).setContentIntent(pi).build()
        try { nm.notify(id, n) } catch (_: SecurityException) {}
    }
}
