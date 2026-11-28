package com.frameout.core.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrialWindowTest {
    @Test
    fun budgetsStayAtTwentySecondsIdleAndTwoMinutesTotal() {
        assertEquals(20_000L, TRIAL_IDLE_MS)
        assertEquals(120_000L, TRIAL_CAP_MS)
    }

    @Test
    fun activeTrialIsNotExpired() {
        assertFalse(TrialWindow.expired(now = 10_000L, lastPick = 5_000L, started = 0L))
    }

    @Test
    fun idleBoundaryIsExclusive() {
        assertFalse(TrialWindow.expired(now = 20_000L, lastPick = 0L, started = 0L))
        assertTrue(TrialWindow.expired(now = 20_001L, lastPick = 0L, started = 0L))
    }

    @Test
    fun totalCapEndsATrialThatKeepsGettingPicks() {
        assertFalse(TrialWindow.expired(now = 120_000L, lastPick = 119_000L, started = 0L))
        assertTrue(TrialWindow.expired(now = 120_001L, lastPick = 120_000L, started = 0L))
    }

    @Test
    fun trialResumedAfterRestartExpiresAtOnce() {
        assertTrue(TrialWindow.expired(now = 500_000L, lastPick = 0L, started = 0L))
    }
}
