package com.frameout.core.nudge

import android.content.Context
import android.content.SharedPreferences

class NudgePrefs private constructor(private val sp: SharedPreferences) {

    var newVideoEnabled: Boolean
        get() = sp.getBoolean(K_NEW_VIDEO, true)
        set(value) = sp.edit().putBoolean(K_NEW_VIDEO, value).apply()

    var remindersEnabled: Boolean
        get() = sp.getBoolean(K_REMINDERS, true)
        set(value) = sp.edit().putBoolean(K_REMINDERS, value).apply()

    var watermarkSec: Long
        get() = sp.getLong(K_WATERMARK, -1L)
        set(value) = sp.edit().putLong(K_WATERMARK, value).apply()

    fun notifiedIds(): Set<Long> =
        sp.getString(K_NOTIFIED, "").orEmpty().split(',').mapNotNull { it.toLongOrNull() }.toSet()

    fun rememberNotified(ids: Collection<Long>) {
        val merged = (ids + sp.getString(K_NOTIFIED, "").orEmpty().split(',').mapNotNull { it.toLongOrNull() })
            .distinct().take(MAX_REMEMBERED)
        sp.edit().putString(K_NOTIFIED, merged.joinToString(",")).apply()
    }

    fun newVideoShownOn(dayKey: Int): Int =
        if (sp.getInt(K_DAY_KEY, -1) == dayKey) sp.getInt(K_DAY_COUNT, 0) else 0

    fun noteNewVideoShown(dayKey: Int) {
        val count = newVideoShownOn(dayKey) + 1
        sp.edit().putInt(K_DAY_KEY, dayKey).putInt(K_DAY_COUNT, count).apply()
    }

    var lastOpenMs: Long
        get() = sp.getLong(K_LAST_OPEN, 0L)
        private set(value) = sp.edit().putLong(K_LAST_OPEN, value).apply()

    var lastReminderMs: Long
        get() = sp.getLong(K_LAST_REMINDER, 0L)
        private set(value) = sp.edit().putLong(K_LAST_REMINDER, value).apply()

    var unansweredStreak: Int
        get() = sp.getInt(K_STREAK, 0)
        private set(value) = sp.edit().putInt(K_STREAK, value).apply()

    var reminderRotation: Int
        get() = sp.getInt(K_ROTATION, 0)
        private set(value) = sp.edit().putInt(K_ROTATION, value).apply()

    var waitingVideoId: Long
        get() = sp.getLong(K_WAIT_ID, -1L)
        private set(value) = sp.edit().putLong(K_WAIT_ID, value).apply()

    var waitingSinceMs: Long
        get() = sp.getLong(K_WAIT_SINCE, 0L)
        private set(value) = sp.edit().putLong(K_WAIT_SINCE, value).apply()

    var waitingReminded: Boolean
        get() = sp.getBoolean(K_WAIT_REMINDED, false)
        private set(value) = sp.edit().putBoolean(K_WAIT_REMINDED, value).apply()

    fun pendingIds(): List<Long> =
        sp.getString(K_PENDING, "").orEmpty().split(',').mapNotNull { it.toLongOrNull() }

    fun addPending(ids: Collection<Long>) = setPending(ids + pendingIds())

    fun setPending(ids: Collection<Long>) {
        sp.edit().putString(K_PENDING, ids.distinct().take(MAX_PENDING).joinToString(",")).apply()
    }

    fun removePending(id: Long) = setPending(pendingIds().filter { it != id })

    fun clearPending() {
        sp.edit().putString(K_PENDING, "").apply()
    }

    var lastRecapMs: Long
        get() = sp.getLong(K_LAST_RECAP, 0L)
        private set(value) = sp.edit().putLong(K_LAST_RECAP, value).apply()

    fun noteRecapShown(nowMs: Long) {
        lastRecapMs = nowMs
    }

    var stalledRemindedId: Long
        get() = sp.getLong(K_STALLED_ID, -1L)
        private set(value) = sp.edit().putLong(K_STALLED_ID, value).apply()

    fun markStalledReminded(projectId: Long) {
        stalledRemindedId = projectId
    }

    fun ensureBaseline(nowMs: Long) {
        if (lastOpenMs == 0L) lastOpenMs = nowMs
        if (lastRecapMs == 0L) lastRecapMs = nowMs
    }

    fun noteOpened(nowMs: Long) {
        sp.edit()
            .putLong(K_LAST_OPEN, nowMs)
            .putInt(K_STREAK, 0)
            .putLong(K_WAIT_ID, -1L)
            .putBoolean(K_WAIT_REMINDED, false)
            .putString(K_PENDING, "")
            .apply()
    }

    fun noteReminderShown(nowMs: Long) {
        lastReminderMs = nowMs
        unansweredStreak += 1
        reminderRotation += 1
    }

    fun setWaitingVideo(id: Long, sinceMs: Long) {
        waitingVideoId = id
        waitingSinceMs = sinceMs
        waitingReminded = false
    }

    fun updateWaitingVideo(id: Long, nowMs: Long, keepClock: Boolean) {
        if (keepClock && waitingSinceMs > 0L && waitingVideoId >= 0L) {
            waitingVideoId = id
        } else {
            setWaitingVideo(id, nowMs)
        }
    }

    fun markWaitingReminded() {
        waitingReminded = true
    }

    fun clearWaitingVideo() {
        waitingVideoId = -1L
        waitingReminded = false
    }

    companion object {
        private const val FILE = "frameout_nudge"
        private const val MAX_REMEMBERED = 40
        private const val MAX_PENDING = 20
        private const val K_PENDING = "pending_video_ids"
        private const val K_LAST_RECAP = "last_recap_ms"
        private const val K_STALLED_ID = "stalled_reminded_id"
        private const val K_NEW_VIDEO = "new_video_enabled"
        private const val K_REMINDERS = "reminders_enabled"
        private const val K_WATERMARK = "watermark_sec"
        private const val K_NOTIFIED = "notified_ids"
        private const val K_DAY_KEY = "day_key"
        private const val K_DAY_COUNT = "day_count"
        private const val K_LAST_OPEN = "last_open_ms"
        private const val K_LAST_REMINDER = "last_reminder_ms"
        private const val K_STREAK = "unanswered_streak"
        private const val K_ROTATION = "reminder_rotation"
        private const val K_WAIT_ID = "waiting_video_id"
        private const val K_WAIT_SINCE = "waiting_since_ms"
        private const val K_WAIT_REMINDED = "waiting_reminded"

        @Volatile private var instance: NudgePrefs? = null

        fun get(context: Context): NudgePrefs = instance ?: synchronized(this) {
            instance ?: NudgePrefs(
                context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            ).also { instance = it }
        }
    }
}
