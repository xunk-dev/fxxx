package com.frameout.core.nudge

import com.frameout.core.nudge.NudgePolicy.Reminder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NudgePolicyTest {
    private val day = NudgePolicy.DAY_MS
    private val now = 100L * day

    private fun remind(
        hour: Int = 12,
        idle: Long = 0L,
        sinceReminder: Long? = null,
        unanswered: Int = 0,
        waiting: Boolean = false,
        waitingFor: Long = 0L,
        waitingReminded: Boolean = false,
        stalled: Boolean = false,
        weeklyFrames: Int = 0,
        sinceRecap: Long? = null
    ) = NudgePolicy.nextReminder(
        nowMs = now,
        hourOfDay = hour,
        lastOpenMs = now - idle,
        lastReminderMs = sinceReminder?.let { now - it } ?: 0L,
        unanswered = unanswered,
        hasWaitingVideo = waiting,
        waitingSinceMs = now - waitingFor,
        waitingReminded = waitingReminded,
        hasStalledProject = stalled,
        weeklyFrames = weeklyFrames,
        lastRecapMs = sinceRecap?.let { now - it } ?: 0L
    )

    @Test fun quietHoursCoverTheNight() {
        assertTrue(NudgePolicy.isQuietHour(22))
        assertTrue(NudgePolicy.isQuietHour(3))
        assertTrue(NudgePolicy.isQuietHour(7))
        assertFalse(NudgePolicy.isQuietHour(8))
        assertFalse(NudgePolicy.isQuietHour(21))
    }

    @Test fun messengerFoldersAndOwnFolderAreBlocked() {
        assertTrue(NudgePolicy.isBlockedPath("Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Video/"))
        assertTrue(NudgePolicy.isBlockedPath("Movies/Telegram/"))
        assertTrue(NudgePolicy.isBlockedPath("Pictures/FrameOut/"))
        assertFalse(NudgePolicy.isBlockedPath("DCIM/Camera/"))
        assertFalse(NudgePolicy.isBlockedPath("Movies/Screen recordings/"))
    }

    @Test fun dailyCapIsThree() {
        assertTrue(NudgePolicy.canShowNewVideo(2))
        assertFalse(NudgePolicy.canShowNewVideo(3))
    }

    @Test fun freshInstallIsNeverNagged() {
        assertEquals(Reminder.None, remind(idle = 0L))
        assertEquals(Reminder.None, remind(idle = 2 * day))
    }

    @Test fun comebackAfterThreeIdleDays() {
        assertEquals(Reminder.Comeback, remind(idle = 3 * day))
    }

    @Test fun neverAtNight() {
        assertEquals(Reminder.None, remind(hour = 23, idle = 10 * day))
        assertEquals(Reminder.None, remind(hour = 5, idle = 10 * day))
    }

    @Test fun backsOffWhenIgnored() {
        assertEquals(Reminder.None, remind(idle = 5 * day, sinceReminder = 2 * day, unanswered = 1))
        assertEquals(Reminder.None, remind(idle = 9 * day, sinceReminder = 6 * day, unanswered = 1))
        assertEquals(Reminder.Comeback, remind(idle = 10 * day, sinceReminder = 7 * day, unanswered = 1))
        assertEquals(Reminder.None, remind(idle = 20 * day, sinceReminder = 13 * day, unanswered = 2))
        assertEquals(Reminder.Comeback, remind(idle = 30 * day, sinceReminder = 14 * day, unanswered = 2))
    }

    @Test fun stopsAfterThreeIgnoredReminders() {
        assertEquals(Reminder.None, remind(idle = 90 * day, sinceReminder = 30 * day, unanswered = 3))
    }

    @Test fun atMostOneReminderPerDay() {
        assertEquals(Reminder.None, remind(idle = 10 * day, sinceReminder = 5 * 60 * 60 * 1000L))
    }

    @Test fun waitingVideoReminderComesAfterTwentyHoursOnlyOnce() {
        val hours20 = 20L * 60 * 60 * 1000
        assertEquals(Reminder.None, remind(waiting = true, waitingFor = hours20 - 1))
        assertEquals(Reminder.WaitingVideo, remind(waiting = true, waitingFor = hours20))
        assertEquals(Reminder.None, remind(waiting = true, waitingFor = hours20, waitingReminded = true))
    }

    @Test fun staleWaitingVideoIsDropped() {
        assertEquals(Reminder.None, remind(waiting = true, waitingFor = 4 * day))
    }

    @Test fun stalledProjectBeatsTheGenericComeback() {
        assertEquals(Reminder.StalledProject, remind(idle = 4 * day, stalled = true))
        assertEquals(Reminder.StalledProject, remind(idle = 1 * day, stalled = true))
    }

    @Test fun waitingVideoStillBeatsStalledProject() {
        val hours20 = 20L * 60 * 60 * 1000
        assertEquals(Reminder.WaitingVideo, remind(waiting = true, waitingFor = hours20, stalled = true))
    }

    @Test fun stalledProjectRespectsNightBackOffAndDailyLimit() {
        assertEquals(Reminder.None, remind(hour = 23, stalled = true))
        assertEquals(Reminder.None, remind(stalled = true, unanswered = 3))
        assertEquals(Reminder.None, remind(stalled = true, sinceReminder = 5 * 60 * 60 * 1000L))
    }

    @Test fun weeklyRecapNeedsRealActivityAndAFullWeek() {
        assertEquals(Reminder.WeeklyRecap, remind(weeklyFrames = NudgePolicy.RECAP_MIN_FRAMES, sinceRecap = 7 * day))
        assertEquals(Reminder.None, remind(weeklyFrames = NudgePolicy.RECAP_MIN_FRAMES - 1, sinceRecap = 7 * day))
        assertEquals(Reminder.None, remind(weeklyFrames = 500, sinceRecap = 7 * day - 1))
    }

    @Test fun weeklyRecapNeverFiresWithoutABaseline() {
        assertEquals(Reminder.None, remind(weeklyFrames = 500, sinceRecap = null))
    }

    @Test fun weeklyRecapBeatsComebackButNotStalledProject() {
        assertEquals(Reminder.WeeklyRecap, remind(idle = 5 * day, weeklyFrames = 40, sinceRecap = 8 * day))
        assertEquals(Reminder.StalledProject, remind(idle = 5 * day, weeklyFrames = 40, sinceRecap = 8 * day, stalled = true))
    }

    @Test fun weeklyRecapRespectsNightAndIgnoredBackOff() {
        assertEquals(Reminder.None, remind(hour = 2, weeklyFrames = 40, sinceRecap = 8 * day))
        assertEquals(Reminder.None, remind(weeklyFrames = 40, sinceRecap = 8 * day, unanswered = 3))
    }
}
