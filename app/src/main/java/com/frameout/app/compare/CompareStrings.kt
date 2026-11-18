package com.frameout.app.compare

import com.frameout.app.R
import com.frameout.core.util.StringSource
import java.util.Locale

internal object CompareStrings {
    private fun s(id: Int, vararg args: Any): String = StringSource.get(id, *args)
    fun stack() = s(R.string.compare_stack)
    fun side() = s(R.string.compare_side)
    fun slider() = s(R.string.compare_slider)
    fun swap() = s(R.string.compare_swap)
    fun fit() = s(R.string.compare_fit)
    fun zoomIn() = s(R.string.compare_zoom_in)
    fun zoomOut() = s(R.string.compare_zoom_out)
    fun resetZoom() = s(R.string.compare_reset_zoom)
    fun hint() = s(R.string.compare_hint)
    fun loadFailed() = s(R.string.compare_load_failed)
    fun divider() = s(R.string.compare_divider)
    fun diff() = s(R.string.compare_diff)
    fun diffBusy() = s(R.string.compare_diff_busy)
    fun link() = s(R.string.compare_link)
    fun unlink() = s(R.string.compare_unlink)
    fun actualSize() = s(R.string.compare_actual_size)
    fun diffPercent(p: Float) = s(R.string.compare_diff_percent, String.format(Locale.US, "%.1f", p))
}
