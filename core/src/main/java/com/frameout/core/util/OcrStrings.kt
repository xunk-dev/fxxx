package com.frameout.core.util

import com.frameout.core.R
import com.frameout.core.quality.TextModelState

object OcrStrings {
    private fun s(id: Int, vararg args: Any): String = StringSource.get(id, *args)
    fun araTitle() = s(R.string.ocr_ara_title)
    fun araDownloading(percent: Int) = s(R.string.ocr_ara_downloading, percent)
    fun araWaitingWifi() = s(R.string.ocr_ara_waiting_wifi)
    fun araConfirm() = s(R.string.ocr_ara_confirm)
    fun araFailed() = s(R.string.ocr_ara_failed)
    fun araUnavailable() = s(R.string.ocr_ara_unavailable)
    fun textTitle() = s(R.string.ocr_text_title)

    fun textMessage(state: TextModelState): String? = when (state) {
        TextModelState.NoPlayServices -> s(R.string.ocr_text_no_play)
        TextModelState.PlayServicesOutdated -> s(R.string.ocr_text_outdated)
        TextModelState.Downloading, TextModelState.Unavailable -> s(R.string.ocr_text_pending)
        TextModelState.Unknown, TextModelState.Ready -> null
    }
}
