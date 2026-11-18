package com.frameout.core.util

import com.frameout.core.R

object SearchStrings {
    private fun s(id: Int, vararg args: Any): String = StringSource.get(id, *args)
    fun action() = s(R.string.search_image)
    fun actionShort() = s(R.string.search_image_short)
    fun failed() = s(R.string.search_failed)
    fun opening() = s(R.string.search_opening)
    fun webRetry() = s(R.string.search_web_retry)
    fun webSite() = s(R.string.search_web_site)
    fun webFallback() = s(R.string.search_web_fallback)
}
