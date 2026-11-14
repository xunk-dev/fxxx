package com.frameout.core.nudge

import com.frameout.core.R
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.StringSource

object NudgeStrings {
    fun channelNewVideo() = StringSource.get(R.string.nudge_channel_new_video)
    fun channelNewVideoDesc() = StringSource.get(R.string.nudge_channel_new_video_desc)
    fun channelReminders() = StringSource.get(R.string.nudge_channel_reminders)
    fun channelRemindersDesc() = StringSource.get(R.string.nudge_channel_reminders_desc)

    fun newVideoTitle() = StringSource.get(R.string.nudge_new_video_title)
    fun newVideosTitle(count: Int) = StringSource.plural(R.plurals.nudge_new_videos_title, count, count)
    fun actionExtract() = StringSource.get(R.string.nudge_action_extract)
    fun actionOpen() = StringSource.get(R.string.nudge_action_open)
    fun actionStopReminders() = StringSource.get(R.string.nudge_action_stop_reminders)
    fun reviewTitle() = StringSource.get(R.string.nudge_review_title)
    fun reviewText() = StringSource.get(R.string.nudge_review_text)
    fun failedTitle() = StringSource.get(R.string.nudge_failed_title)
    fun failedText() = StringSource.get(R.string.nudge_failed_text)
    fun waitingTitle() = StringSource.get(R.string.nudge_waiting_title)
    fun waitingText(name: String) = StringSource.get(R.string.nudge_waiting_text, name)

    fun stalledTitle() = StringSource.get(R.string.nudge_stalled_title)
    fun stalledText(name: String) = StringSource.get(R.string.nudge_stalled_text, name)
    fun recapTitle() = StringSource.get(R.string.nudge_recap_title)
    fun recapText(frames: Int) = StringSource.get(R.string.nudge_recap_text, frames)

    private val comebackTitles = intArrayOf(R.string.nudge_comeback_title_0, R.string.nudge_comeback_title_1, R.string.nudge_comeback_title_2)
    private val comebackTexts = intArrayOf(R.string.nudge_comeback_text_0, R.string.nudge_comeback_text_1, R.string.nudge_comeback_text_2)
    fun comebackTitle(rotation: Int) = StringSource.get(comebackTitles[Math.floorMod(rotation, comebackTitles.size)])
    fun comebackText(rotation: Int) = StringSource.get(comebackTexts[Math.floorMod(rotation, comebackTexts.size)])

    fun settingNewVideoTitle() = StringSource.get(R.string.nudge_setting_new_video_title)
    fun settingNewVideoHint() = StringSource.get(R.string.nudge_setting_new_video_hint)
    fun settingNewVideoPartial() = StringSource.get(R.string.nudge_setting_new_video_partial)
    fun settingRemindersTitle() = StringSource.get(R.string.nudge_setting_reminders_title)
    fun settingRemindersHint() = StringSource.get(R.string.nudge_setting_reminders_hint)
    fun settingSystemOff() = StringSource.get(R.string.nudge_setting_system_off)

    fun videoLine(name: String, durationMs: Long): String {
        val total = (durationMs / 1000L).coerceAtLeast(0L)
        val h = total / 3600L
        val m = (total % 3600L) / 60L
        val s = total % 60L
        val length = when {
            h > 0 -> FrameStrings.durationHm(h, m)
            m > 0 -> FrameStrings.durationMs(m, s)
            else -> FrameStrings.durationS(s)
        }
        val clean = name.substringBeforeLast('.').ifBlank { name }
        return if (clean.isBlank()) length else "$clean${FrameStrings.sep()}$length"
    }
}
