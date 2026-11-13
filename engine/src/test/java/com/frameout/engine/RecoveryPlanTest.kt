package com.frameout.engine

import com.frameout.engine.repair.RecoveryPlan
import com.frameout.engine.repair.RecoveryStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecoveryPlanTest {
    @Test fun skipsGrowWithEachAttempt() {
        val targets = (0..5).map { (RecoveryPlan.next(it, 10_000, 600_000, false) as RecoveryStep.SkipTo).positionMs }
        assertEquals(listOf(10_001L, 10_750L, 12_000L, 15_000L, 20_000L, 30_000L), targets)
    }

    @Test fun ladderEndsInSalvageOnce() {
        assertEquals(RecoveryStep.Salvage, RecoveryPlan.next(6, 10_000, 600_000, false))
        assertEquals(RecoveryStep.GiveUp, RecoveryPlan.next(6, 10_000, 600_000, true))
    }

    @Test fun errorNearTheEndSalvagesInsteadOfSkipping() {
        assertEquals(RecoveryStep.Salvage, RecoveryPlan.next(0, 59_500, 60_000, false))
        assertEquals(RecoveryStep.Salvage, RecoveryPlan.next(5, 45_000, 60_000, false))
    }

    @Test fun unknownDurationStillSkips() =
        assertTrue(RecoveryPlan.next(0, 5_000, 0, false) is RecoveryStep.SkipTo)

    @Test fun onlyDataErrorsAreRecoverable() {
        assertTrue(RecoveryPlan.isRecoverable(4003))
        assertTrue(RecoveryPlan.isRecoverable(3001))
        assertTrue(RecoveryPlan.isRecoverable(2008))
        assertFalse(RecoveryPlan.isRecoverable(2005))
        assertFalse(RecoveryPlan.isRecoverable(2006))
        assertFalse(RecoveryPlan.isRecoverable(6001))
    }

    @Test fun progressResetsAttempts() {
        assertTrue(RecoveryPlan.resetsAttempts(14_000, 10_000))
        assertFalse(RecoveryPlan.resetsAttempts(11_000, 10_000))
        assertFalse(RecoveryPlan.resetsAttempts(50_000, -1))
    }
}
