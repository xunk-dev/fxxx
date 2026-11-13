package com.frameout.app.frames

import com.frameout.engine.extract.GateFlags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartAccessTest {
    private val since = 1_000L
    private val stampedPro = GateFlags.encode(false, false, false, true, granted = true)
    private val freeExtraction = GateFlags.encode(false, false, false, true)
    private val trialProject = GateFlags.encode(true, true, false, true, granted = true, trial = true)

    private fun decide(pro: Boolean, armed: Boolean, flags: Int, createdAt: Long = 2_000L, enforced: Boolean = true) =
        SmartAccess.decide(enforced, pro, armed, flags, createdAt, since)

    @Test fun proOnAProExtractionIsAllowed() = assertEquals(SmartVerdict.Allowed, decide(true, false, stampedPro))

    @Test fun upgradingLaterDoesNotUnlockAFreeExtraction() =
        assertEquals(SmartVerdict.NeedsReextract, decide(true, false, freeExtraction))

    @Test fun oldProjectsAreGrandfatheredForPro() = assertEquals(SmartVerdict.Allowed, decide(true, false, freeExtraction, createdAt = 500L))

    @Test fun freeUserNeedsTheTrialOrThePaywall() {
        assertEquals(SmartVerdict.NeedsGate, decide(false, false, freeExtraction))
        assertEquals(SmartVerdict.SpendTrial, decide(false, true, freeExtraction))
    }

    @Test fun oldProjectsAreNeverGrandfatheredForFreeUsers() =
        assertEquals(SmartVerdict.NeedsGate, decide(false, false, freeExtraction, createdAt = 500L))

    @Test fun theProjectThatSpentTheTrialKeepsItsTools() = assertEquals(SmartVerdict.Allowed, decide(false, false, trialProject))

    @Test fun unenforcedBuildsAreOpen() = assertEquals(SmartVerdict.Allowed, decide(false, false, 0, enforced = false))

    @Test fun flagsRoundTrip() {
        assertTrue(GateFlags.granted(stampedPro)); assertFalse(GateFlags.trial(stampedPro))
        assertTrue(GateFlags.trial(trialProject)); assertTrue(GateFlags.blurry(trialProject))
        assertFalse(GateFlags.granted(freeExtraction))
        assertTrue(GateFlags.granted(GateFlags.with(freeExtraction, granted = true, trial = true)))
    }
}
