package com.frameout.core.util

import com.frameout.core.R

object DetailStrings {
    private fun s(id: Int): String = StringSource.get(id)
    fun details() = s(R.string.project_details)
    fun date() = s(R.string.detail_date)
    fun duration() = s(R.string.detail_duration)
    fun quality() = s(R.string.detail_quality)
    fun codec() = s(R.string.detail_codec)
    fun fps() = s(R.string.detail_fps)
    fun status() = s(R.string.detail_status)
}
