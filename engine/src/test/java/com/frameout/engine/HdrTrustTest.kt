package com.frameout.engine

import android.media.MediaFormat
import com.frameout.engine.decode.HdrTrust
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HdrTrustTest {
    private val pq = MediaFormat.COLOR_TRANSFER_ST2084
    private val hlg = MediaFormat.COLOR_TRANSFER_HLG

    private fun evidence(
        hasInput: Boolean = true,
        inputTransfer: Int = 0,
        staticInfo: Boolean = false,
        hdrProfile: Boolean = false,
        tenBit: Boolean = false,
        ictcp: Boolean = false
    ) = HdrTrust.Evidence(hasInput, inputTransfer, staticInfo, hdrProfile, tenBit, ictcp)

    @Test
    fun sdrTransfersAreAlwaysAccepted() {
        assertTrue(HdrTrust.accepts(MediaFormat.COLOR_TRANSFER_SDR_VIDEO, evidence()))
        assertTrue(HdrTrust.accepts(0, evidence()))
    }

    @Test
    fun decoderOnlyHdrClaimOnPlainStreamIsRejected() {
        assertFalse(HdrTrust.accepts(pq, evidence()))
        assertFalse(HdrTrust.accepts(hlg, evidence()))
    }

    @Test
    fun streamDeclaredOrTenBitHdrIsAccepted() {
        assertTrue(HdrTrust.accepts(pq, evidence(inputTransfer = pq)))
        assertTrue(HdrTrust.accepts(hlg, evidence(inputTransfer = hlg)))
        assertTrue(HdrTrust.accepts(pq, evidence(staticInfo = true)))
        assertTrue(HdrTrust.accepts(pq, evidence(hdrProfile = true)))
        assertTrue(HdrTrust.accepts(hlg, evidence(tenBit = true)))
        assertTrue(HdrTrust.accepts(pq, evidence(ictcp = true)))
    }

    @Test
    fun differentInputTransferDoesNotCorroborate() {
        assertFalse(HdrTrust.accepts(pq, evidence(inputTransfer = hlg)))
    }

    @Test
    fun singleFormatCallsKeepTheirTransfer() {
        assertTrue(HdrTrust.accepts(pq, evidence(hasInput = false)))
        assertEquals(pq, HdrTrust.vet(pq, evidence(hasInput = false)))
    }
}
