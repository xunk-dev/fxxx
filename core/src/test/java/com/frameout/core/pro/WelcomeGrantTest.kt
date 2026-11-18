package com.frameout.core.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WelcomeGrantTest {
    private class FakeStore(var saved: String? = null, var failWrites: Boolean = false) : GrantStore {
        var writes = 0
        override fun read(): String? = saved
        override fun write(value: String): Boolean {
            writes++
            if (failWrites) return false
            saved = value
            return true
        }
    }

    @Test
    fun startsEmpty() {
        val grant = WelcomeGrant(FakeStore())
        assertNull(grant.key.value)
        assertFalse(grant.committed)
    }

    @Test
    fun commitStoresTheChosenColor() {
        val store = FakeStore()
        val grant = WelcomeGrant(store)
        assertTrue(grant.commit("gold"))
        assertEquals("gold", grant.key.value)
        assertEquals("gold", store.saved)
    }

    @Test
    fun secondCommitCannotChangeTheGrant() {
        val store = FakeStore()
        val grant = WelcomeGrant(store)
        grant.commit("gold")
        assertFalse(grant.commit("rose"))
        assertEquals("gold", grant.key.value)
        assertEquals(1, store.writes)
    }

    @Test
    fun grantSurvivesRestart() {
        val store = FakeStore()
        WelcomeGrant(store).commit("violet")
        val again = WelcomeGrant(store)
        assertEquals("violet", again.key.value)
        assertFalse(again.commit("gold"))
    }

    @Test
    fun systemColorCannotBeGranted() {
        val grant = WelcomeGrant(FakeStore())
        assertTrue(grant.commit("dynamic"))
        assertEquals(WelcomeGrant.FALLBACK, grant.key.value)
    }

    @Test
    fun unknownKeyFallsBack() {
        val grant = WelcomeGrant(FakeStore())
        grant.commit("not-a-color")
        assertEquals(WelcomeGrant.FALLBACK, grant.key.value)
    }

    @Test
    fun tamperedStoredValueIsIgnored() {
        val grant = WelcomeGrant(FakeStore(saved = "dynamic"))
        assertNull(grant.key.value)
        assertFalse(grant.committed)
    }

    @Test
    fun failedWriteKeepsTheGrantForThisSessionOnly() {
        val store = FakeStore(failWrites = true)
        val grant = WelcomeGrant(store)
        assertFalse(grant.commit("gold"))
        assertEquals("gold", grant.key.value)
        assertNull(store.saved)
        assertNull(WelcomeGrant(store).key.value)
    }

    @Test
    fun untrustedStoreKeepsGrantInMemoryOnly() {
        val store = FakeStore(saved = "gold")
        val grant = WelcomeGrant(store, persistent = false)
        assertNull(grant.key.value)
        assertTrue(grant.commit("rose"))
        assertEquals("rose", grant.key.value)
        assertEquals(0, store.writes)
    }

    @Test
    fun existingUserKeepsAPreviouslyFreeColor() {
        listOf("indigo", "ocean", "black").forEach { color ->
            val grant = WelcomeGrant(FakeStore())
            grant.adoptExisting(onboardingDone = true, current = color)
            assertEquals(color, grant.key.value)
        }
    }

    @Test
    fun existingUserOnAPaidColorDoesNotGetItForFree() {
        listOf("gold", "emerald", "dynamic", "rose").forEach { color ->
            val grant = WelcomeGrant(FakeStore())
            grant.adoptExisting(onboardingDone = true, current = color)
            assertEquals(WelcomeGrant.FALLBACK, grant.key.value)
        }
    }

    @Test
    fun newUserIsNotAdoptedBeforeOnboardingEnds() {
        val grant = WelcomeGrant(FakeStore())
        grant.adoptExisting(onboardingDone = false, current = "indigo")
        assertNull(grant.key.value)
    }

    @Test
    fun adoptingDoesNotOverwriteAnExistingGrant() {
        val grant = WelcomeGrant(FakeStore(saved = "gold"))
        grant.adoptExisting(onboardingDone = true, current = "indigo")
        assertEquals("gold", grant.key.value)
    }

    @Test
    fun spentGrantForcesTheFallbackColor() {
        val grant = WelcomeGrant(FakeStore())
        grant.commit("gold", spent = true)
        assertEquals(WelcomeGrant.FALLBACK, grant.key.value)
    }

    @Test
    fun revokeDowngradesToTheFallbackAndPersists() {
        val store = FakeStore()
        val grant = WelcomeGrant(store)
        grant.commit("gold")
        assertEquals("gold", grant.revoke())
        assertEquals(WelcomeGrant.FALLBACK, grant.key.value)
        assertEquals(WelcomeGrant.FALLBACK, store.saved)
        assertNull(grant.revoke())
    }

    @Test
    fun revokeWithoutAGrantDoesNothing() {
        assertNull(WelcomeGrant(FakeStore()).revoke())
    }

    @Test
    fun deferKeepsTheGrantOpenAcrossRestart() {
        val store = FakeStore()
        val grant = WelcomeGrant(store)
        assertTrue(grant.defer())
        assertTrue(grant.decided)
        assertFalse(grant.committed)
        val again = WelcomeGrant(store)
        assertTrue(again.open.value)
        assertNull(again.key.value)
    }

    @Test
    fun trialSurvivesRestartAndCountsAsSpent() {
        val store = FakeStore()
        val grant = WelcomeGrant(store)
        grant.defer()
        assertTrue(grant.beginTrial(spent = false))
        assertTrue(grant.spent)
        assertFalse(grant.committed)
        val again = WelcomeGrant(store)
        assertTrue(again.trial.value)
        assertTrue(again.open.value)
    }

    @Test
    fun settleLocksWhateverIsCurrentOnce() {
        val grant = WelcomeGrant(FakeStore())
        grant.defer()
        grant.beginTrial(false)
        assertEquals("emerald", grant.settle("emerald"))
        assertEquals("emerald", grant.key.value)
        assertFalse(grant.open.value)
        assertFalse(grant.trial.value)
        assertNull(grant.settle("gold"))
        assertFalse(grant.beginTrial(false))
    }

    @Test
    fun settleOnSystemColorFallsBack() {
        val grant = WelcomeGrant(FakeStore())
        grant.defer()
        grant.beginTrial(false)
        assertEquals(WelcomeGrant.FALLBACK, grant.settle("dynamic"))
    }

    @Test
    fun trialRefusedWhenSpentUntrustedOrNotOpen() {
        val spent = WelcomeGrant(FakeStore())
        spent.defer()
        assertFalse(spent.beginTrial(spent = true))
        assertFalse(WelcomeGrant(FakeStore()).beginTrial(false))
        val untrusted = WelcomeGrant(FakeStore(), persistent = false)
        untrusted.defer()
        assertFalse(untrusted.beginTrial(false))
    }

    @Test
    fun failedWriteKeepsTrialRunning() {
        val store = FakeStore()
        val grant = WelcomeGrant(store)
        grant.defer()
        grant.beginTrial(false)
        store.failWrites = true
        assertNull(grant.settle("gold"))
        assertTrue(grant.trial.value)
        assertNull(grant.key.value)
    }

    @Test
    fun exhaustDoesNotKillARunningTrial() {
        val grant = WelcomeGrant(FakeStore())
        grant.defer()
        grant.beginTrial(false)
        grant.exhaust()
        assertTrue(grant.trial.value)
        assertNull(grant.key.value)
    }

    @Test
    fun exhaustClosesAnOpenGrantToFallback() {
        val grant = WelcomeGrant(FakeStore())
        grant.defer()
        grant.exhaust()
        assertEquals(WelcomeGrant.FALLBACK, grant.key.value)
        assertFalse(grant.open.value)
    }

    @Test
    fun adoptExistingLeavesAnOpenGrantAlone() {
        val grant = WelcomeGrant(FakeStore())
        grant.defer()
        grant.adoptExisting(onboardingDone = true, current = "ocean")
        assertTrue(grant.open.value)
        assertNull(grant.key.value)
    }
}
