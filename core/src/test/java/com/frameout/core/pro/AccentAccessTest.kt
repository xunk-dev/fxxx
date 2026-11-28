package com.frameout.core.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccentAccessTest {
    private val paid = ProCatalog.accentKeys - ProCatalog.freeAccents

    private fun lock(
        key: String,
        enforced: Boolean = true,
        isPro: Boolean = false,
        welcomeKey: String? = null,
        trial: Boolean = false,
        ads: Boolean = true,
        rewarded: Boolean = false
    ) = AccentAccess.lock(key, enforced, isPro, welcomeKey, trial, ads, rewarded)

    @Test
    fun noColorPickedKeepsEveryPaidColorLocked() {
        paid.forEach { assertTrue(it, lock(it) != AccentLock.Open) }
        assertEquals(AccentLock.Open, lock("black"))
    }

    @Test
    fun noColorPickedWithoutAdsLocksPaidColorsAsPro() {
        paid.forEach { assertEquals(it, AccentLock.Pro, lock(it, ads = false)) }
    }

    @Test
    fun freeColorPickedKeepsPaidColorsLocked() {
        val grant = WelcomeGrant(MemoryStore())
        grant.resolveOnboarding("black", spent = false)
        assertTrue(grant.open.value)
        paid.forEach { assertTrue(it, lock(it, welcomeKey = grant.key.value, trial = grant.trial.value) != AccentLock.Open) }
    }

    @Test
    fun proOnlyColorsStayProAndOthersAskForAnAd() {
        ProCatalog.proOnlyAccents.forEach { assertEquals(it, AccentLock.Pro, lock(it)) }
        assertEquals(AccentLock.Ad, lock("emerald"))
    }

    @Test
    fun runningTrialOpensEveryPickableColorButNotTheSystemColor() {
        paid.forEach { assertEquals(it, AccentLock.Open, lock(it, trial = true)) }
        assertEquals(AccentLock.Pro, lock(ProCatalog.DYNAMIC_ACCENT, trial = true))
    }

    @Test
    fun afterTheTrialOnlyTheKeptColorIsOpen() {
        assertEquals(AccentLock.Open, lock("gold", welcomeKey = "gold"))
        (paid - "gold").forEach { assertTrue(it, lock(it, welcomeKey = "gold") != AccentLock.Open) }
    }

    @Test
    fun proAndFreeBuildsSeeEverythingOpen() {
        paid.forEach {
            assertEquals(AccentLock.Open, lock(it, isPro = true))
            assertEquals(AccentLock.Open, lock(it, enforced = false))
        }
    }

    @Test
    fun rewardedAdOpensOnlyAdTierColors() {
        assertEquals(AccentLock.Open, lock("emerald", rewarded = true))
        assertEquals(AccentLock.Pro, lock("gold", rewarded = true))
    }

    @Test
    fun welcomeCanBeClaimedOnlyWhileOpenAndForPickableColors() {
        assertTrue(AccentAccess.canClaimWelcome("gold", true, false, true))
        assertFalse(AccentAccess.canClaimWelcome("gold", true, false, false))
        assertFalse(AccentAccess.canClaimWelcome("gold", true, true, true))
        assertFalse(AccentAccess.canClaimWelcome("gold", false, false, true))
        assertFalse(AccentAccess.canClaimWelcome(ProCatalog.DYNAMIC_ACCENT, true, false, true))
    }
}
