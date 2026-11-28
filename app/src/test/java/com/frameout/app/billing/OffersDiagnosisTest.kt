package com.frameout.app.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OffersDiagnosisTest {
    @Test
    fun networkErrorsAreReportedAsNoConnection() {
        assertEquals(OffersFailure.NoConnection, OffersDiagnosis.classify("NetworkError", false, 0, 0))
        assertEquals(OffersFailure.NoConnection, OffersDiagnosis.classify("OfflineConnectionError", false, 0, 0))
    }

    @Test
    fun anyOtherSdkErrorIsAStoreFailure() {
        listOf("StoreProblemError", "ConfigurationError", "UnknownError", "InvalidCredentialsError")
            .forEach { assertEquals(it, OffersFailure.StoreFailed, OffersDiagnosis.classify(it, false, 0, 0)) }
    }

    @Test
    fun missingCurrentOfferingIsReported() {
        assertEquals(OffersFailure.NoOffering, OffersDiagnosis.classify(null, false, 0, 0))
    }

    @Test
    fun currentOfferingWithoutPackagesIsReported() {
        assertEquals(OffersFailure.NoOffering, OffersDiagnosis.classify(null, true, 0, 0))
    }

    @Test
    fun currentOfferingWithUnrecognizedPackagesIsReported() {
        assertEquals(OffersFailure.NoOffering, OffersDiagnosis.classify(null, true, 2, 0))
    }

    @Test
    fun usablePackagesAreNotAFailure() {
        assertNull(OffersDiagnosis.classify(null, true, 3, 3))
        assertNull(OffersDiagnosis.classify(null, true, 3, 1))
    }

    @Test
    fun noCurrentOfferingDetailListsWhatTheDashboardReturned() {
        val text = OffersDiagnosis.describeEmpty(listOf("beta", "default"), null, emptyList(), 0)
        assertTrue(text, text.contains("no current offering"))
        assertTrue(text, text.contains("beta") && text.contains("default"))
    }

    @Test
    fun emptyOfferingDetailNamesTheOffering() {
        val text = OffersDiagnosis.describeEmpty(listOf("default"), "default", emptyList(), 0)
        assertTrue(text, text.contains("'default'") && text.contains("no purchasable packages"))
    }

    @Test
    fun unmatchedPackagesDetailListsThem() {
        val text = OffersDiagnosis.describeEmpty(listOf("default"), "default", listOf("x:CUSTOM:foo"), 0)
        assertTrue(text, text.contains("x:CUSTOM:foo"))
    }

    @Test
    fun errorDetailKeepsTheRealCode() {
        assertEquals("code=StoreProblemError message=boom", OffersDiagnosis.describeError("StoreProblemError", "boom"))
    }
}
