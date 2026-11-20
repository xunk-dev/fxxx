package com.frameout.app.push

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.work.WorkManager
import com.google.firebase.messaging.FirebaseMessaging

object Push {
    const val CHANNEL = "announcements"
    private const val TOPIC = "all"

    fun init(ctx: Context) {
        cleanupLegacy(ctx)
        runCatching { FirebaseMessaging.getInstance().subscribeToTopic(TOPIC) }
    }

    private fun cleanupLegacy(ctx: Context) {
        runCatching { WorkManager.getInstance(ctx).cancelUniqueWork("remote_content") }
        runCatching { NotificationManagerCompat.from(ctx).deleteNotificationChannel("remote_notices") }
        runCatching {
            ctx.getSharedPreferences("remote_content", Context.MODE_PRIVATE).edit().remove("shown").apply()
        }
    }
}
