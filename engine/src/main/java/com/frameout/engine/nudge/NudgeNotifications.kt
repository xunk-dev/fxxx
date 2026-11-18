package com.frameout.engine.nudge

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.frameout.core.db.ProjectEntity
import com.frameout.core.nudge.NewVideo
import com.frameout.core.nudge.NewVideoScanner
import com.frameout.core.nudge.NudgePolicy
import com.frameout.core.nudge.NudgePrefs
import com.frameout.core.nudge.NudgeStrings
import com.frameout.core.nudge.VideoCover
import com.frameout.engine.R
import com.frameout.engine.work.ExtractNotifications
import com.frameout.engine.work.NotificationTheme

object NudgeNotifications {
    const val CHANNEL_NEW_VIDEO = "frameout_new_video"
    const val CHANNEL_REMINDERS = "frameout_reminders"
    private const val ID_NEW_VIDEO = 4210
    private const val ID_REMINDER = 4211
    private const val ID_PROBLEM = 4212
    private const val ID_PROJECT_BASE = 0x300000
    private const val NEW_VIDEO_TTL_MS = 24L * 60L * 60L * 1000L
    private const val REMINDER_TTL_MS = 48L * 60L * 60L * 1000L

    fun canPost(context: Context, channelId: String): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = context.getSystemService(NotificationManager::class.java).getNotificationChannel(channelId)
            if (channel != null && channel.importance == NotificationManager.IMPORTANCE_NONE) return false
        }
        return true
    }

    fun isNewVideoShowing(context: Context): Boolean = runCatching {
        context.getSystemService(NotificationManager::class.java).activeNotifications.any { it.id == ID_NEW_VIDEO }
    }.getOrDefault(false)

    fun showPending(context: Context): Int {
        val prefs = NudgePrefs.get(context)
        val now = System.currentTimeMillis()
        val videos = prefs.pendingIds()
            .mapNotNull { NewVideoScanner.byId(context, it) }
            .filter { now - it.addedSec * 1000L <= NudgePolicy.MAX_VIDEO_AGE_MS }
            .sortedByDescending { it.addedSec }
        prefs.setPending(videos.map { it.id })
        if (videos.isEmpty()) {
            cancelNewVideo(context)
            return 0
        }
        showNewVideos(context, videos)
        return videos.size
    }

    fun showNewVideos(context: Context, videos: List<NewVideo>) {
        val newest = videos.firstOrNull() ?: return
        ensureChannels(context)
        val single = NudgeStrings.videoLine(newest.name, newest.durationMs)
        val line = if (videos.size > 1) "$single  +${videos.size - 1}" else single
        val title = if (videos.size == 1) NudgeStrings.newVideoTitle() else NudgeStrings.newVideosTitle(videos.size)
        val builder = base(context, CHANNEL_NEW_VIDEO, title, line)
            .setOnlyAlertOnce(true)
            .setWhen(newest.addedSec * 1000L)
            .setShowWhen(true)
            .setTimeoutAfter(NEW_VIDEO_TTL_MS)
            .setContentIntent(openVideo(context, newest))
            .addAction(0, NudgeStrings.actionExtract(), extractAction(context, newest))
            .addAction(0, NudgeStrings.actionOpen(), openVideo(context, newest))
        withCover(context, builder, newest, line)
        post(context, ID_NEW_VIDEO, builder)
    }

    fun showWaitingReminder(context: Context, video: NewVideo) {
        ensureChannels(context)
        val clean = video.name.substringBeforeLast('.').ifBlank { video.name }
        val builder = base(context, CHANNEL_REMINDERS, NudgeStrings.waitingTitle(), NudgeStrings.waitingText(clean))
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setTimeoutAfter(REMINDER_TTL_MS)
            .setContentIntent(openVideo(context, video))
            .addAction(0, NudgeStrings.actionExtract(), extractAction(context, video))
            .addAction(0, NudgeStrings.actionStopReminders(), stopRemindersAction(context))
        withCover(context, builder, video, NudgeStrings.waitingText(clean))
        post(context, ID_REMINDER, builder)
    }

    fun showComebackReminder(context: Context, rotation: Int) {
        ensureChannels(context)
        val text = NudgeStrings.comebackText(rotation)
        val builder = base(context, CHANNEL_REMINDERS, NudgeStrings.comebackTitle(rotation), text)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setTimeoutAfter(REMINDER_TTL_MS)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openApp(context))
            .addAction(0, NudgeStrings.actionStopReminders(), stopRemindersAction(context))
        post(context, ID_REMINDER, builder)
    }

    fun showStalledReminder(context: Context, project: ProjectEntity) {
        ensureChannels(context)
        val text = NudgeStrings.stalledText(project.name.ifBlank { project.sourceName })
        val builder = base(context, CHANNEL_REMINDERS, NudgeStrings.stalledTitle(), text)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setTimeoutAfter(REMINDER_TTL_MS)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openProject(context, project.id))
            .addAction(0, NudgeStrings.actionOpen(), openProject(context, project.id))
            .addAction(0, NudgeStrings.actionStopReminders(), stopRemindersAction(context))
        post(context, ID_REMINDER, builder)
    }

    fun showRecapReminder(context: Context, frames: Int) {
        ensureChannels(context)
        val text = NudgeStrings.recapText(frames)
        val builder = base(context, CHANNEL_REMINDERS, NudgeStrings.recapTitle(), text)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setTimeoutAfter(REMINDER_TTL_MS)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openApp(context))
            .addAction(0, NudgeStrings.actionStopReminders(), stopRemindersAction(context))
        post(context, ID_REMINDER, builder)
    }

    fun showNeedsReview(context: Context, uri: Uri, videoId: Long) {
        ensureChannels(context)
        val builder = base(context, CHANNEL_NEW_VIDEO, NudgeStrings.reviewTitle(), NudgeStrings.reviewText())
            .setStyle(NotificationCompat.BigTextStyle().bigText(NudgeStrings.reviewText()))
            .setContentIntent(openVideo(context, uri, videoId))
        post(context, ID_NEW_VIDEO, builder)
    }

    fun showStartFailed(context: Context, uri: Uri, videoId: Long) {
        ensureChannels(context)
        val builder = base(context, CHANNEL_NEW_VIDEO, NudgeStrings.failedTitle(), NudgeStrings.failedText())
            .setContentIntent(openVideo(context, uri, videoId))
        post(context, ID_PROBLEM, builder)
    }

    fun cancelNewVideo(context: Context) {
        NotificationManagerCompat.from(context).cancel(ID_NEW_VIDEO)
    }

    fun cancelReminder(context: Context) {
        NotificationManagerCompat.from(context).cancel(ID_REMINDER)
    }

    private fun base(context: Context, channel: String, title: String, text: String): NotificationCompat.Builder =
        NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(NotificationTheme.color(context))
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

    private fun withCover(context: Context, builder: NotificationCompat.Builder, video: NewVideo, summary: String) {
        val cover: Bitmap? = VideoCover.wide(context, video.uri, video.durationMs)
        if (cover == null) {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(summary))
            return
        }
        builder.setLargeIcon(VideoCover.squareIcon(cover))
        builder.setStyle(
            NotificationCompat.BigPictureStyle()
                .bigPicture(cover)
                .bigLargeIcon(null as Bitmap?)
                .setSummaryText(summary)
        )
    }

    private fun post(context: Context, id: Int, builder: NotificationCompat.Builder) {
        try {
            NotificationManagerCompat.from(context).notify(id, builder.build())
        } catch (_: SecurityException) {
        }
    }

    private fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_NEW_VIDEO, NudgeStrings.channelNewVideo(), NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = NudgeStrings.channelNewVideoDesc() }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_REMINDERS, NudgeStrings.channelReminders(), NotificationManager.IMPORTANCE_LOW)
                .apply { description = NudgeStrings.channelRemindersDesc() }
        )
    }

    private fun launchBase(context: Context): Intent? =
        context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

    private fun openApp(context: Context): PendingIntent? {
        val intent = launchBase(context) ?: return null
        return PendingIntent.getActivity(context, ID_REMINDER, intent, FLAGS)
    }

    private fun openProject(context: Context, projectId: Long): PendingIntent? {
        val intent = launchBase(context) ?: return null
        intent.putExtra(ExtractNotifications.EXTRA_OPEN_PROJECT, projectId)
        intent.putExtra(ExtractNotifications.EXTRA_OPEN_TOKEN, com.frameout.core.util.IntentToken.get(context))
        return PendingIntent.getActivity(context, ID_PROJECT_BASE + (projectId and 0xFFFFF).toInt(), intent, FLAGS)
    }

    private fun openVideo(context: Context, video: NewVideo): PendingIntent? = openVideo(context, video.uri, video.id)

    private fun openVideo(context: Context, uri: Uri, videoId: Long): PendingIntent? {
        val intent = launchBase(context) ?: return null
        intent.action = Intent.ACTION_VIEW
        intent.setDataAndType(uri, "video/*")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return PendingIntent.getActivity(context, (videoId and 0x7FFFFFF).toInt(), intent, FLAGS)
    }

    private fun extractAction(context: Context, video: NewVideo): PendingIntent {
        val intent = Intent(NudgeIntents.ACTION_EXTRACT_NEW)
            .setPackage(context.packageName)
            .putExtra(NudgeIntents.EXTRA_VIDEO_URI, video.uri.toString())
            .putExtra(NudgeIntents.EXTRA_VIDEO_ID, video.id)
            .putExtra(NudgeIntents.EXTRA_VIDEO_DURATION, video.durationMs)
        return PendingIntent.getBroadcast(context, (video.id and 0x7FFFFFF).toInt(), intent, FLAGS)
    }

    private fun stopRemindersAction(context: Context): PendingIntent {
        val intent = Intent(NudgeIntents.ACTION_STOP_REMINDERS).setPackage(context.packageName)
        return PendingIntent.getBroadcast(context, ID_REMINDER, intent, FLAGS)
    }

    private const val FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
}
