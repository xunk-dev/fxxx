package com.frameout.core.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrialLedgerTest {
    @Test
    fun roundTripsEveryStateAndWipeCount() {
        listOf(TrialLedger.NEW, TrialLedger.ARMED, TrialLedger.USED).forEach { s ->
            (0..3).forEach { w -> assertEquals(TrialSnapshot(s, w), TrialLedger.decode(TrialLedger.encode(s, w))) }
        }
    }

    @Test
    fun emptyVaultMeansFirstRun() {
        assertNull(TrialLedger.decode(ByteArray(0)))
    }

    @Test
    fun garbageOrTamperedBytesCountAsUsedAndMaxWipes() {
        listOf("hello".toByteArray(), "ft2:0:0:999".toByteArray(), "ft2:9:0:1".toByteArray(), byteArrayOf(0x7f, 0x00, 0x01)).forEach {
            assertEquals(TrialSnapshot(TrialLedger.USED, TrialRisk.MAX_WIPES), TrialLedger.decode(it))
        }
    }

    @Test
    fun editingUsedBackToNewIsRejected() {
        val forged = String(TrialLedger.encode(TrialLedger.USED, 1)).replaceFirst("ft2:2:", "ft2:0:")
        assertEquals(TrialLedger.USED, TrialLedger.decode(forged.toByteArray())!!.state)
    }

    @Test
    fun laterStateAlwaysWins() {
        assertEquals(TrialLedger.USED, TrialLedger.merge(TrialLedger.NEW, TrialLedger.USED))
        assertEquals(TrialLedger.USED, TrialLedger.merge(TrialLedger.USED, TrialLedger.NEW))
        assertEquals(TrialLedger.ARMED, TrialLedger.merge(TrialLedger.NEW, TrialLedger.ARMED))
        assertEquals(TrialLedger.NEW, TrialLedger.merge(TrialLedger.NEW, null))
    }

    @Test
    fun clearedAppDataStillSeesUsedTrial() {
        val remote = TrialLedger.decode(TrialLedger.encode(TrialLedger.USED))?.state
        assertEquals(TrialLedger.USED, TrialLedger.merge(TrialLedger.NEW, remote))
    }

    @Test
    fun repeatedWipesEmulatorsAndClockRollbackCloseTheTrial() {
        assertFalse(TrialRisk.blocked(false, false, 2))
        assertTrue(TrialRisk.blocked(false, false, TrialRisk.MAX_WIPES))
        assertTrue(TrialRisk.blocked(true, false, 0))
        assertTrue(TrialRisk.blocked(false, true, 0))
    }

    @Test
    fun emulatorDetectionSeparatesRealPhonesFromEmulators() {
        assertTrue(TrialRisk.looksLikeEmulator("generic/sdk_gphone64/emu:14", "ranchu", "sdk_gphone64_x86_64", "sdk_gphone64_x86_64"))
        assertFalse(TrialRisk.looksLikeEmulator("samsung/e1sxeea/e1s:14/UP1A/release-keys", "exynos2400", "SM-S921B", "e1sxeea"))
        assertFalse(TrialRisk.looksLikeEmulator("google/husky/husky:14/AP1A/release-keys", "husky", "Pixel 8 Pro", "husky"))
    }

    @Test
    fun grantedLedgerRoundTrips() {
        (0..2).forEach { st -> assertEquals(TrialSnapshot(st, 1, true), TrialLedger.decode(TrialLedger.encode(st, 1, true))) }
    }

    @Test
    fun legacyLedgerIsNotGranted() {
        assertEquals(false, TrialLedger.decode(TrialLedger.encode(1, 0))!!.granted)
    }

    @Test
    fun forgedGrantedLedgerIsRejected() {
        val forged = String(TrialLedger.encode(TrialLedger.USED, 1, true)).replaceFirst("ft3:2:", "ft3:0:")
        assertEquals(TrialSnapshot(TrialLedger.USED, TrialRisk.MAX_WIPES), TrialLedger.decode(forged.toByteArray()))
    }
}
