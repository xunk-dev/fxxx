package com.frameout.core.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WelcomeFlowTest {
    @Test
    fun noColorPickedLeavesTheGiftUnopenedAndUnspent() {
        val store = MemoryStore()
        val grant = WelcomeGrant(store)
        grant.resolveOnboarding(null, spent = false)
        assertTrue(grant.open.value)
        assertFalse(grant.trial.value)
        assertNull(grant.key.value)
        assertFalse(grant.spent)
        assertEquals(WelcomeGrant.OPEN, store.saved)
    }

    @Test
    fun freeColorPickedDefersTheGiftTheSameWay() {
        val grant = WelcomeGrant(MemoryStore())
        grant.resolveOnboarding("black", spent = false)
        assertTrue(grant.open.value)
        assertNull(grant.key.value)
        assertFalse(grant.spent)
    }

    @Test
    fun paidColorPickedCommitsItAndSpendsTheGift() {
        val store = MemoryStore()
        val grant = WelcomeGrant(store)
        grant.resolveOnboarding("gold", spent = false)
        assertEquals("gold", grant.key.value)
        assertFalse(grant.open.value)
        assertTrue(grant.spent)
        assertEquals("gold", store.saved)
    }

    @Test
    fun paidColorPickedAfterTheGiftWasSpentFallsBack() {
        val grant = WelcomeGrant(MemoryStore())
        grant.resolveOnboarding("gold", spent = true)
        assertEquals(WelcomeGrant.FALLBACK, grant.key.value)
    }

    @Test
    fun onboardingCannotBeResolvedTwice() {
        val grant = WelcomeGrant(MemoryStore())
        grant.resolveOnboarding("gold", spent = false)
        grant.resolveOnboarding("rose", spent = false)
        assertEquals("gold", grant.key.value)
    }

    @Test
    fun trialStartsOnlyFromAnOpenGift() {
        val closed = WelcomeGrant(MemoryStore())
        assertFalse(closed.beginTrial(spent = false))
        val grant = WelcomeGrant(MemoryStore())
        grant.defer()
        assertTrue(grant.beginTrial(spent = false))
        assertTrue(grant.trial.value)
        assertTrue(grant.open.value)
        assertTrue(grant.spent)
    }

    @Test
    fun settlingKeepsTheLastPickedColorAndClosesTheGift() {
        val store = MemoryStore()
        val grant = WelcomeGrant(store)
        grant.defer()
        grant.beginTrial(spent = false)
        assertEquals("violet", grant.settle("violet"))
        assertEquals("violet", grant.key.value)
        assertFalse(grant.trial.value)
        assertFalse(grant.open.value)
        assertEquals("violet", store.saved)
    }

    @Test
    fun settlingWithTheSystemColorFallsBack() {
        val grant = WelcomeGrant(MemoryStore())
        grant.defer()
        grant.beginTrial(spent = false)
        assertEquals(WelcomeGrant.FALLBACK, grant.settle(ProCatalog.DYNAMIC_ACCENT))
    }

    @Test
    fun settlingWithoutATrialDoesNothing() {
        val grant = WelcomeGrant(MemoryStore())
        grant.defer()
        assertNull(grant.settle("gold"))
        assertNull(grant.key.value)
    }

    @Test
    fun pickingAfterTheTrialEndedCannotStartAnother() {
        val grant = WelcomeGrant(MemoryStore())
        grant.defer()
        grant.beginTrial(spent = false)
        grant.settle("rose")
        assertFalse(grant.beginTrial(spent = false))
        assertFalse(grant.commit("gold"))
        assertEquals("rose", grant.key.value)
    }

    @Test
    fun spentGiftNeverStartsATrial() {
        val grant = WelcomeGrant(MemoryStore())
        grant.defer()
        assertFalse(grant.beginTrial(spent = true))
        assertFalse(grant.trial.value)
    }

    @Test
    fun exhaustedGiftClosesWithTheFallbackAndRefusesATrial() {
        val grant = WelcomeGrant(MemoryStore())
        grant.defer()
        grant.exhaust()
        assertEquals(WelcomeGrant.FALLBACK, grant.key.value)
        assertFalse(grant.open.value)
        assertFalse(grant.beginTrial(spent = false))
    }

    @Test
    fun trialCannotStartWhenTheStateCannotBePersisted() {
        val store = MemoryStore()
        val grant = WelcomeGrant(store)
        grant.defer()
        store.failWrites = true
        assertFalse(grant.beginTrial(spent = false))
        assertFalse(grant.trial.value)
    }

    @Test
    fun reopeningWithAnOpenGiftKeepsItOpen() {
        val store = MemoryStore()
        WelcomeGrant(store).defer()
        val again = WelcomeGrant(store)
        assertTrue(again.open.value)
        assertFalse(again.trial.value)
        assertNull(again.key.value)
    }

    @Test
    fun reopeningMidTrialResumesItAndItCanStillSettle() {
        val store = MemoryStore()
        val first = WelcomeGrant(store)
        first.defer()
        first.beginTrial(spent = false)
        val again = WelcomeGrant(store)
        assertTrue(again.trial.value)
        assertTrue(again.open.value)
        assertEquals("emerald", again.settle("emerald"))
    }

    @Test
    fun reopeningAfterSettlingKeepsTheColorAndNoMoreTrials() {
        val store = MemoryStore()
        val first = WelcomeGrant(store)
        first.defer()
        first.beginTrial(spent = false)
        first.settle("lime")
        val again = WelcomeGrant(store)
        assertEquals("lime", again.key.value)
        assertFalse(again.open.value)
        assertFalse(again.beginTrial(spent = false))
    }

    @Test
    fun untrustedStoreNeverRestoresAGift() {
        val store = MemoryStore(saved = WelcomeGrant.OPEN)
        val grant = WelcomeGrant(store, persistent = false)
        assertFalse(grant.open.value)
        assertFalse(grant.beginTrial(spent = false))
    }
}
