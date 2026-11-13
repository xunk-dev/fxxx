package com.frameout.core.nudge

object NudgePolicy {
    const val DAY_MS = 24L * 60L * 60L * 1000L
    const val QUIET_START_HOUR = 22
    const val QUIET_END_HOUR = 8

    const val MAX_NEW_VIDEO_PER_DAY = 3
    const val MIN_VIDEO_MS = 2_000L
    const val MAX_VIDEO_AGE_MS = 3L * DAY_MS
    const val MAX_UNANSWERED_REMINDERS = 3

    const val ONE_TAP_FRAME_CAP = 900

    const val STALLED_MIN_AGE_MS = DAY_MS
    const val STALLED_MAX_AGE_MS = 14L * DAY_MS

    const val RECAP_WINDOW_MS = 7L * DAY_MS
    const val RECAP_MIN_FRAMES = 20

    private const val MIN_REMINDER_GAP_MS = 20L * 60L * 60L * 1000L
    private const val WAITING_AFTER_MS = 20L * 60L * 60L * 1000L

    private val BLOCKED_PATH_PARTS = listOf(
        "whatsapp", "telegram", "viber", "signal", "messenger",
        "wechat", "kakaotalk", "/line/", "snapchat", "/cache/", "/.thumbnails", "/.trash"
    )

    fun isQuietHour(hourOfDay: Int): Boolean =
        hourOfDay >= QUIET_START_HOUR || hourOfDay < QUIET_END_HOUR

    fun isBlockedPath(path: String): Boolean {
        val lower = path.lowercase()
        return "frameout" in lower || BLOCKED_PATH_PARTS.any { it in lower }
    }

    fun dayKey(year: Int, dayOfYear: Int): Int = year * 1000 + dayOfYear

    fun canShowNewVideo(shownToday: Int): Boolean = shownToday < MAX_NEW_VIDEO_PER_DAY

    enum class Reminder { None, WaitingVideo, StalledProject, WeeklyRecap, Comeback }

    fun comebackGapMs(unanswered: Int): Long = when (unanswered) {
        0 -> 3L * DAY_MS
        1 -> 7L * DAY_MS
        else -> 14L * DAY_MS
    }

    fun nextReminder(
        nowMs: Long,
        hourOfDay: Int,
        lastOpenMs: Long,
        lastReminderMs: Long,
        unanswered: Int,
        hasWaitingVideo: Boolean,
        waitingSinceMs: Long,
        waitingReminded: Boolean,
        hasStalledProject: Boolean = false,
        weeklyFrames: Int = 0,
        lastRecapMs: Long = 0L
    ): Reminder {
        if (isQuietHour(hourOfDay)) return Reminder.None
        if (unanswered >= MAX_UNANSWERED_REMINDERS) return Reminder.None
        if (lastReminderMs > 0L && nowMs - lastReminderMs < MIN_REMINDER_GAP_MS) return Reminder.None

        if (hasWaitingVideo && !waitingReminded) {
            val waited = nowMs - waitingSinceMs
            if (waited in WAITING_AFTER_MS..MAX_VIDEO_AGE_MS) return Reminder.WaitingVideo
        }

        if (hasStalledProject) return Reminder.StalledProject

        if (weeklyFrames >= RECAP_MIN_FRAMES && lastRecapMs > 0L && nowMs - lastRecapMs >= RECAP_WINDOW_MS) {
            return Reminder.WeeklyRecap
        }

        val idle = nowMs - lastOpenMs
        val sinceReminder = if (lastReminderMs > 0L) nowMs - lastReminderMs else Long.MAX_VALUE
        if (idle >= comebackGapMs(0) && (unanswered == 0 || sinceReminder >= comebackGapMs(unanswered))) {
            return Reminder.Comeback
        }
        return Reminder.None
    }
}
