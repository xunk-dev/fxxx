package com.frameout.core.quality

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TextModelRulesTest {
    @Test
    fun missingDisabledOrUnconnectedPlayServicesAreReportedAsMissing() {
        listOf(1, 3, 9, 17).forEach { assertEquals(TextModelState.NoPlayServices, TextModelRules.fromStatusCode(it)) }
    }

    @Test
    fun anOutdatedPlayServicesIsDistinguished() {
        assertEquals(TextModelState.PlayServicesOutdated, TextModelRules.fromStatusCode(2))
    }

    @Test
    fun anyOtherStatusIsJustUnavailable() {
        listOf(7, 8, 13, 99).forEach { assertEquals(TextModelState.Unavailable, TextModelRules.fromStatusCode(it)) }
    }

    @Test
    fun onlyTheUnavailableMlKitCodeMeansTheModelIsStillDownloading() {
        assertEquals(TextModelState.Downloading, TextModelRules.fromMlKitCode(14))
        assertNull(TextModelRules.fromMlKitCode(13))
        assertNull(TextModelRules.fromMlKitCode(0))
    }

    @Test
    fun unknownAndReadyAreNotProblems() {
        assertFalse(TextModelState.Unknown.problem)
        assertFalse(TextModelState.Ready.problem)
        assertTrue(TextModelState.Downloading.problem)
        assertTrue(TextModelState.NoPlayServices.problem)
        assertTrue(TextModelState.PlayServicesOutdated.problem)
        assertTrue(TextModelState.Unavailable.problem)
    }
}
