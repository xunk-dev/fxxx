package com.frameout.app

import com.google.android.play.core.assetpacks.model.AssetPackErrorCode
import com.google.android.play.core.assetpacks.model.AssetPackStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AraPackStateTest {
    private fun state(status: Int, error: Int = AssetPackErrorCode.NO_ERROR, done: Long = 0L, total: Long = 0L) =
        PackStates.fromStatus(status, error, done, total)

    @Test
    fun completedMeansReady() {
        assertEquals(PackState.Ready, state(AssetPackStatus.COMPLETED, done = 10, total = 10))
    }

    @Test
    fun waitingForWifiAndConfirmationAreNoLongerPlainDownloads() {
        assertEquals(PackState.WaitingWifi, state(AssetPackStatus.WAITING_FOR_WIFI))
        assertEquals(PackState.NeedsConfirmation, state(AssetPackStatus.REQUIRES_USER_CONFIRMATION))
    }

    @Test
    fun progressIsCappedBelowOneHundredUntilCompleted() {
        assertEquals(PackState.Downloading(50), state(AssetPackStatus.DOWNLOADING, done = 50, total = 100))
        assertEquals(PackState.Downloading(99), state(AssetPackStatus.TRANSFERRING, done = 100, total = 100))
        assertEquals(PackState.Downloading(0), state(AssetPackStatus.PENDING))
    }

    @Test
    fun percentSurvivesUnknownAndNegativeSizes() {
        assertEquals(0, PackStates.percent(5, 0))
        assertEquals(0, PackStates.percent(-5, 100))
        assertEquals(99, PackStates.percent(500, 100))
    }

    @Test
    fun failuresKeepTheirCodeUnlessPlayItselfIsMissing() {
        assertEquals(PackState.Failed(AssetPackErrorCode.NETWORK_ERROR), state(AssetPackStatus.FAILED, AssetPackErrorCode.NETWORK_ERROR))
        assertEquals(PackState.Failed(AssetPackErrorCode.INSUFFICIENT_STORAGE), state(AssetPackStatus.CANCELED, AssetPackErrorCode.INSUFFICIENT_STORAGE))
        assertEquals(PackState.Unavailable, state(AssetPackStatus.FAILED, AssetPackErrorCode.PLAY_STORE_NOT_FOUND))
        assertEquals(PackState.Unavailable, PackStates.failure(AssetPackErrorCode.API_NOT_AVAILABLE))
        assertEquals(PackState.Unavailable, PackStates.failure(AssetPackErrorCode.APP_NOT_OWNED))
    }

    @Test
    fun onlyFailedAndUnavailableCanBeRetried() {
        assertEquals(PackAction.Retry, PackStates.action(PackState.Failed(1)))
        assertEquals(PackAction.Retry, PackStates.action(PackState.Unavailable))
        assertEquals(PackAction.Confirm, PackStates.action(PackState.NeedsConfirmation))
        listOf(PackState.Idle, PackState.Ready, PackState.WaitingWifi, PackState.Downloading(10)).forEach {
            assertEquals(PackAction.None, PackStates.action(it))
        }
    }

    @Test
    fun theSettingsRowIsHiddenWhenThereIsNothingToSay() {
        assertFalse(PackStates.visible(PackState.Idle))
        assertFalse(PackStates.visible(PackState.Ready))
        assertTrue(PackStates.visible(PackState.Downloading(3)))
        assertTrue(PackStates.visible(PackState.Failed(1)))
        assertTrue(PackStates.visible(PackState.Unavailable))
    }
}
