package com.frameout.engine.decode

import android.media.MediaFormat
import com.frameout.core.util.DiagLog

internal object HdrTrust {
    @Volatile private var reported = false

    fun isHdrTransfer(transfer: Int): Boolean =
        transfer == MediaFormat.COLOR_TRANSFER_ST2084 || transfer == MediaFormat.COLOR_TRANSFER_HLG

    fun accepts(transfer: Int, evidence: Evidence): Boolean {
        if (!isHdrTransfer(transfer)) return true
        if (!evidence.hasInput) return true
        return evidence.inputTransfer == transfer || evidence.staticInfo || evidence.hdrProfile || evidence.tenBit || evidence.ictcp
    }

    fun vet(transfer: Int, evidence: Evidence): Int {
        if (accepts(transfer, evidence)) return transfer
        if (!reported) {
            reported = true
            DiagLog.append("ColorParams", "ignored an HDR transfer ($transfer) reported only by the decoder output; the stream does not declare HDR, treating it as SDR")
        }
        return MediaFormat.COLOR_TRANSFER_SDR_VIDEO
    }

    class Evidence(
        val hasInput: Boolean,
        val inputTransfer: Int,
        val staticInfo: Boolean,
        val hdrProfile: Boolean,
        val tenBit: Boolean,
        val ictcp: Boolean
    )
}
