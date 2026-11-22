package com.frameout.core.util

import com.frameout.core.R

object UpdateStrings {
    private fun s(id: Int, vararg args: Any): String = StringSource.get(id, *args)
    fun badgeNew() = s(R.string.update_badge_new)
    fun title() = s(R.string.update_title)
    fun meta(size: String) = s(R.string.update_meta, size)
    fun notesTitle() = s(R.string.update_notes_title)
    fun notesBody() = s(R.string.update_notes_body)
    fun later() = s(R.string.update_later)
    fun now() = s(R.string.update_now)
}
