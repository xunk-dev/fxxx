package com.frameout.core.util

import com.frameout.core.R

object ClipStrings {
    private fun s(id: Int, vararg args: Any): String = StringSource.get(id, *args)
    fun saveClip() = s(R.string.clip_save)
    fun saveWait() = s(R.string.clip_save_wait)
    fun tooShort() = s(R.string.clip_too_short)
    fun webpLimit(seconds: Int) = s(R.string.clip_webp_limit, seconds)
}
