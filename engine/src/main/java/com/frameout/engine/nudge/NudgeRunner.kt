package com.frameout.engine.nudge

import android.app.ActivityManager
import android.content.Context
import com.frameout.core.db.AppDatabase
import com.frameout.core.db.ProjectEntity
import com.frameout.core.media.PermissionHelper
import com.frameout.core.nudge.NewVideo
import com.frameout.core.nudge.NewVideoScanner
import com.frameout.core.nudge.NudgePolicy
import com.frameout.core.nudge.NudgePrefs
import com.frameout.core.prefs.AppPrefs
import com.frameout.core.util.LocaleHelper
import java.util.Calendar
import kotlin.coroutines.cancellation.CancellationException

internal object NudgeRunner {

    suspend fun checkNewVideos(context: Context) {
        val prefs = NudgePrefs.get(context)
        if (!prefs.newVideoEnabled || !PermissionHelper.canReadVideos(context)) return
        val now = System.currentTimeMillis()
        if (prefs.watermarkSec < 0L) {
            prefs.watermarkSec = now / 1000L
            return
        }
        val cal = Calendar.getInstance()
        if (NudgePolicy.isQuietHour(cal.get(Calendar.HOUR_OF_DAY))) return
        val fresh = scanFresh(context, prefs, now)
        if (fresh.isEmpty()) return
        val dayKey = NudgePolicy.dayKey(cal.get(Calendar.YEAR), cal.get(Calendar.DAY_OF_YEAR))
        prefs.rememberNotified(fresh.map { it.id })
        if (appVisible()) return
        if (!NudgeNotifications.canPost(context, NudgeNotifications.CHANNEL_NEW_VIDEO)) return
        notifyFresh(context, prefs, fresh, dayKey, now)
    }

    private suspend fun scanFresh(context: Context, prefs: NudgePrefs, now: Long): List<NewVideo> {
        val sources = safe { AppDatabase.get(context).projectDao().allSourceUris().toSet() } ?: emptySet()
        val result = NewVideoScanner.scan(context, prefs.watermarkSec, now, sources, prefs.notifiedIds())
        result.advanceTo?.let { if (it > prefs.watermarkSec) prefs.watermarkSec = it }
        return result.videos
    }

    private fun notifyFresh(context: Context, prefs: NudgePrefs, fresh: List<NewVideo>, dayKey: Int, now: Long) {
        val showing = NudgeNotifications.isNewVideoShowing(context)
        if (!showing) prefs.clearPending()
        if (!showing && !NudgePolicy.canShowNewVideo(prefs.newVideoShownOn(dayKey))) return
        applyLanguage(context)
        prefs.addPending(fresh.map { it.id })
        if (NudgeNotifications.showPending(context) == 0) return
        if (!showing) prefs.noteNewVideoShown(dayKey)
        prefs.updateWaitingVideo(fresh.first().id, now, keepClock = showing)
    }

    suspend fun maybeRemind(context: Context) {
        val prefs = NudgePrefs.get(context)
        if (!prefs.remindersEnabled) return
        if (!NudgeNotifications.canPost(context, NudgeNotifications.CHANNEL_REMINDERS)) return
        if (appVisible()) return
        val now = System.currentTimeMillis()
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val waitingId = prefs.waitingVideoId
        val waiting = if (waitingId >= 0L) NewVideoScanner.byId(context, waitingId) else null
        if (waitingId >= 0L && waiting == null) prefs.clearWaitingVideo()
        val dao = safe { AppDatabase.get(context).projectDao() }
        val stalled = dao?.let {
            safe { it.latestStalled(now - NudgePolicy.STALLED_MAX_AGE_MS, now - NudgePolicy.STALLED_MIN_AGE_MS) }
        }?.takeIf { it.id != prefs.stalledRemindedId }
        val weeklyFrames = dao?.let { safe { it.framesExtractedSince(now - NudgePolicy.RECAP_WINDOW_MS) } } ?: 0
        val decision = decide(prefs, now, hour, waiting != null, stalled != null, weeklyFrames)
        applyLanguage(context)
        show(context, prefs, decision, waiting, stalled, weeklyFrames, now)
    }

    private fun decide(
        prefs: NudgePrefs,
        now: Long,
        hour: Int,
        hasWaiting: Boolean,
        hasStalled: Boolean,
        weeklyFrames: Int
    ): NudgePolicy.Reminder = NudgePolicy.nextReminder(
        nowMs = now,
        hourOfDay = hour,
        lastOpenMs = prefs.lastOpenMs,
        lastReminderMs = prefs.lastReminderMs,
        unanswered = prefs.unansweredStreak,
        hasWaitingVideo = hasWaiting,
        waitingSinceMs = prefs.waitingSinceMs,
        waitingReminded = prefs.waitingReminded,
        hasStalledProject = hasStalled,
        weeklyFrames = weeklyFrames,
        lastRecapMs = prefs.lastRecapMs
    )

    private fun show(
        context: Context,
        prefs: NudgePrefs,
        decision: NudgePolicy.Reminder,
        waiting: NewVideo?,
        stalled: ProjectEntity?,
        weeklyFrames: Int,
        now: Long
    ) {
        when (decision) {
            NudgePolicy.Reminder.WaitingVideo -> {
                NudgeNotifications.showWaitingReminder(context, waiting ?: return)
                prefs.markWaitingReminded()
            }
            NudgePolicy.Reminder.StalledProject -> {
                val project = stalled ?: return
                NudgeNotifications.showStalledReminder(context, project)
                prefs.markStalledReminded(project.id)
            }
            NudgePolicy.Reminder.WeeklyRecap -> {
                NudgeNotifications.showRecapReminder(context, weeklyFrames)
                prefs.noteRecapShown(now)
            }
            NudgePolicy.Reminder.Comeback -> NudgeNotifications.showComebackReminder(context, prefs.reminderRotation)
            NudgePolicy.Reminder.None -> return
        }
        prefs.noteReminderShown(now)
    }

    private suspend fun <T> safe(block: suspend () -> T): T? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

    private fun applyLanguage(context: Context) {
        LocaleHelper.apply(context.applicationContext, AppPrefs.peekLanguage(context))
    }

    private fun appVisible(): Boolean {
        val info = ActivityManager.RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(info)
        return info.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
    }
}
