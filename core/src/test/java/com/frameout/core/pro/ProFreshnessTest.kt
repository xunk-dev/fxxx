package com.frameout.core.pro

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProFreshnessTest {
    private val day = 86_400_000L
    private val grace = 14 * day
    private val skew = day

    private fun fresh(now: Long, verified: Long, highWater: Long) = ProFreshness.fresh(now, verified, highWater, grace, skew)

    @Test
    fun recentVerificationIsFresh() {
        assertTrue(fresh(now = 10 * day, verified = 5 * day, highWater = 10 * day))
    }

    @Test
    fun expiredGraceIsNotFresh() {
        assertFalse(fresh(now = 30 * day, verified = 5 * day, highWater = 30 * day))
    }

    @Test
    fun neverVerifiedIsNotFresh() {
        assertFalse(fresh(now = 10 * day, verified = 0L, highWater = 0L))
    }

    @Test
    fun rollingTheClockBackCannotRevive() {
        assertFalse(fresh(now = 6 * day, verified = 5 * day, highWater = 25 * day))
    }

    @Test
    fun smallClockCorrectionsAreTolerated() {
        assertTrue(fresh(now = 10 * day - day / 2, verified = 5 * day, highWater = 10 * day))
    }

    @Test
    fun stampFromTheFutureIsForged() {
        assertFalse(fresh(now = 10 * day, verified = 400 * day, highWater = 10 * day))
    }
}
