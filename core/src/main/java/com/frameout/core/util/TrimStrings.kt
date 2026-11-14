package com.frameout.core.util

import com.frameout.core.R

object TrimStrings {
    private fun s(id: Int, vararg args: Any): String = StringSource.get(id, *args)
    fun extractFrames() = s(R.string.trim_extract_frames)
    fun planTitle() = s(R.string.trim_plan_title)
    fun planChange() = s(R.string.trim_plan_change)
    fun panelHandle() = s(R.string.trim_panel_handle)
    fun speedHint() = s(R.string.trim_speed_hint)
    fun scrubHint() = s(R.string.trim_scrub_hint)
    fun batchPrepareTitle() = s(R.string.batch_prepare_title)
    fun batchSettingsTitle() = s(R.string.batch_settings_title)
    fun batchSettingsHint() = s(R.string.batch_settings_hint)
    fun batchVideosTitle(count: Int) = s(R.string.batch_videos_title, count)
    fun batchStart() = s(R.string.batch_start)
    fun batchEdit() = s(R.string.batch_edit)
    fun batchRemove() = s(R.string.batch_remove)
    fun batchApply() = s(R.string.batch_apply)
    fun batchTapHint() = s(R.string.batch_tap_hint)
    fun batchVideos(count: Int) = StringSource.plural(R.plurals.batch_videos, count, count)
    fun planFrames(count: Int) = StringSource.plural(R.plurals.plan_frames, count, count)
    fun planSize(mb: String) = s(R.string.trim_plan_size, mb)
    fun planSizeGb(gb: String) = s(R.string.trim_plan_size_gb, gb)
    fun planSizeTb(tb: String) = s(R.string.trim_plan_size_tb, tb)
    fun planEveryN(n: Int) = s(R.string.trim_plan_every_n, n)
    fun planEveryTime(value: String) = s(R.string.trim_plan_every_time, value)
    fun planFixed(count: Int) = s(R.string.trim_plan_fixed, count)
    fun planKeyframes() = s(R.string.trim_plan_keyframes)
    fun planScene() = s(R.string.trim_plan_scene)
    fun planSlides() = s(R.string.trim_plan_slides)
    fun planContent(value: String) = s(R.string.trim_plan_content, value)
    fun planAll() = s(R.string.trim_plan_all)
    fun optionsTitle() = s(R.string.trim_options_title)
    fun optionsMode() = s(R.string.trim_options_mode)
    fun gallerySave() = s(R.string.trim_gallery_save)
    fun galleryHint() = s(R.string.trim_gallery_hint)
    fun galleryShort() = s(R.string.trim_gallery_short)
    fun galleryHelp() = s(R.string.trim_gallery_help)
    fun galleryWarn(count: Int) = s(R.string.trim_gallery_warn, count)
    fun filtersTitle() = s(R.string.trim_filters_title)
    fun peopleOnlyTitle() = s(R.string.setting_people_only)
    fun peopleOnlyHelp() = s(R.string.help_people_only)
    fun peopleSlow() = s(R.string.trim_people_slow)
    fun peopleIgnored() = s(R.string.trim_people_ignored)
    fun filtersCountHint() = s(R.string.trim_filters_count_hint)
    fun skipSummary(blurry: Int, duplicate: Int, noPeople: Int, noMatch: Int): String = listOfNotNull(
        blurry.takeIf { it > 0 }?.let { s(R.string.skip_blurry_n, it) },
        duplicate.takeIf { it > 0 }?.let { s(R.string.skip_dup_n, it) },
        noPeople.takeIf { it > 0 }?.let { s(R.string.skip_nopeople_n, it) },
        noMatch.takeIf { it > 0 }?.let { s(R.string.skip_nomatch_n, it) }
    ).joinToString(FrameStrings.sep())
    fun peopleModeFaces() = s(R.string.people_mode_faces)
    fun peopleModeBoth() = s(R.string.people_mode_both)
    fun outFrames() = s(R.string.trim_out_frames)
    fun outVideo() = s(R.string.trim_out_video)
    fun outLive() = s(R.string.trim_out_live)
    fun outGif() = s(R.string.trim_out_gif)
    fun outWebp() = s(R.string.trim_out_webp)
    fun outputTitle() = s(R.string.trim_output_title)
    fun upTo(seconds: Int) = s(R.string.trim_up_to, seconds)
    fun outDesc(kind: String) = when (kind) {
        "video" -> s(R.string.trim_out_video_desc)
        "live" -> s(R.string.trim_out_live_desc)
        "gif" -> s(R.string.trim_out_gif_desc)
        "webp" -> s(R.string.trim_out_webp_desc)
        else -> s(R.string.trim_out_frames_desc)
    }
    fun limitHint(seconds: Int) = s(R.string.trim_limit_hint, seconds).map { c ->
        when (c) {
            in '\u0660'..'\u0669' -> '0' + (c - '\u0660')
            in '\u06F0'..'\u06F9' -> '0' + (c - '\u06F0')
            else -> c
        }
    }.joinToString("")
    fun saveAs(label: String) = s(R.string.trim_save_as, label)
    fun wholeVideo() = s(R.string.trim_whole_video)
    fun snapshot() = s(R.string.trim_snapshot)
    fun rangeLength() = s(R.string.trim_range_length)
    fun presetSeconds(seconds: Int) = s(R.string.trim_preset_seconds, seconds)
    fun nudgeEarlier() = s(R.string.trim_nudge_earlier)
    fun nudgeLater() = s(R.string.trim_nudge_later)
}
