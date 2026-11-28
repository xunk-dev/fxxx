package com.frameout.core.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProFreshnessGraceTest {
    private val grace = ProCatalog.GRACE_DAYS * DAY_MS

    private fun fresh(now: Long, verified: Long, highWater: Long) =
        ProFreshness.fresh(now, verified, highWater, grace, SKEW_MS)

    @Test
    fun graceWindowIsFourteenDaysAndSkewIsOneDay() {
        assertEquals(14, ProCatalog.GRACE_DAYS)
        assertEquals(DAY_MS, SKEW_MS)
    }

    @Test
    fun dayThirteenIsFreshAndDayFourteenIsNot() {
        val verified = 100 * DAY_MS
        assertTrue(fresh(verified + 13 * DAY_MS, verified, verified + 13 * DAY_MS))
        assertFalse(fresh(verified + 14 * DAY_MS, verified, verified + 14 * DAY_MS))
        assertFalse(fresh(verified + 15 * DAY_MS, verified, verified + 15 * DAY_MS))
    }

    @Test
    fun graceBoundaryIsExclusiveToTheMillisecond() {
        val verified = 100 * DAY_MS
        assertTrue(fresh(verified + grace - 1, verified, verified + grace - 1))
        assertFalse(fresh(verified + grace, verified, verified + grace))
        assertFalse(fresh(verified + grace + 1, verified, verified + grace + 1))
    }

    @Test
    fun missingOrNegativeVerificationIsNeverFresh() {
        assertFalse(fresh(10 * DAY_MS, 0L, 0L))
        assertFalse(fresh(10 * DAY_MS, -1L, 0L))
        assertFalse(fresh(10 * DAY_MS, Long.MIN_VALUE, 0L))
    }

    @Test
    fun stampJustInsideTheSkewIsAcceptedAndJustOutsideIsForged() {
        val now = 50 * DAY_MS
        assertTrue(fresh(now, now + SKEW_MS, now))
        assertFalse(fresh(now, now + SKEW_MS + 1, now))
    }

    @Test
    fun highWaterToleratesExactlyTheSkew() {
        val highWater = 50 * DAY_MS
        val verified = 49 * DAY_MS
        assertTrue(fresh(highWater - SKEW_MS, verified, highWater))
        assertFalse(fresh(highWater - SKEW_MS - 1, verified, highWater))
    }

    @Test
    fun highWaterOfZeroNeverBlocks() {
        assertTrue(fresh(10 * DAY_MS, 9 * DAY_MS, 0L))
    }

    @Test
    fun rollingBackAfterTheHighWaterAdvancedIsRejected() {
        val verified = 100 * DAY_MS
        val highWater = verified + 13 * DAY_MS
        assertFalse(fresh(verified + 5 * DAY_MS, verified, highWater))
    }

    @Test
    fun jumpingForwardAndBackKeepsTheStateStale() {
        val verified = 5 * DAY_MS
        val highWaterAfterJump = 40 * DAY_MS
        assertFalse(fresh(10 * DAY_MS, verified, highWaterAfterJump))
    }

    @Test
    fun aNewVerificationResetsBothStampsAndRestoresFreshness() {
        val staleHighWater = 40 * DAY_MS
        val now = 10 * DAY_MS
        assertFalse(fresh(now, 5 * DAY_MS, staleHighWater))
        assertTrue(fresh(now, now, now))
    }

    @Test
    fun expiredGraceStaysExpiredWhateverTheHighWater() {
        assertFalse(fresh(200 * DAY_MS, 5 * DAY_MS, 0L))
        assertFalse(fresh(200 * DAY_MS, 5 * DAY_MS, 200 * DAY_MS))
    }

    @Test
    fun clockSetForwardPastTheGraceExpiresProWithoutAHighWaterHelp() {
        val verified = 5 * DAY_MS
        assertFalse(fresh(verified + 20 * DAY_MS, verified, verified))
    }
}
