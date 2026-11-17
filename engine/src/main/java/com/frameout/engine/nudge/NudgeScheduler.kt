package com.frameout.engine.nudge

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.provider.MediaStore
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.frameout.core.media.PermissionHelper
import com.frameout.core.nudge.NudgePrefs
import java.util.concurrent.TimeUnit

object NudgeScheduler {
    private const val WATCH = "frameout_new_video_watch"
    private const val SWEEP = "frameout_nudge_sweep"
    private const val SYNC_EVERY_MS = 10L * 60L * 1000L

    @Volatile private var lastSync = 0L
    @Volatile private var started = false

    fun start(app: Application) {
        if (started) return
        started = true
        NudgePrefs.get(app).ensureBaseline(System.currentTimeMillis())
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                val now = System.currentTimeMillis()
                NudgePrefs.get(app).noteOpened(now)
                NudgeNotifications.cancelReminder(app)
                if (now - lastSync > SYNC_EVERY_MS) sync(app)
            }
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
        sync(app)
    }

    fun sync(context: Context) {
        lastSync = System.currentTimeMillis()
        val prefs = NudgePrefs.get(context)
        val wm = WorkManager.getInstance(context)
        if (prefs.newVideoEnabled && PermissionHelper.canReadVideos(context)) {
            armWatcher(context, append = false)
        } else {
            wm.cancelUniqueWork(WATCH)
        }
        if (prefs.newVideoEnabled || prefs.remindersEnabled) {
            val request = PeriodicWorkRequestBuilder<NudgeSweepWorker>(3, TimeUnit.HOURS, 1, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
                .build()
            wm.enqueueUniquePeriodicWork(SWEEP, ExistingPeriodicWorkPolicy.KEEP, request)
        } else {
            wm.cancelUniqueWork(SWEEP)
        }
    }

    internal fun armWatcher(context: Context, append: Boolean) {
        val constraints = Constraints.Builder()
            .addContentUriTrigger(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true)
            .setTriggerContentUpdateDelay(15, TimeUnit.SECONDS)
            .setTriggerContentMaxDelay(2, TimeUnit.MINUTES)
            .build()
        val request = OneTimeWorkRequestBuilder<NewVideoWatchWorker>().setConstraints(constraints).build()
        val policy = if (append) ExistingWorkPolicy.APPEND_OR_REPLACE else ExistingWorkPolicy.KEEP
        WorkManager.getInstance(context).enqueueUniqueWork(WATCH, policy, request)
    }
}
