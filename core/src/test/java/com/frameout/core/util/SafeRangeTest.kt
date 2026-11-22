package com.frameout.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeRangeTest {
    @Test fun invertedFloatRangeFromRoundingDoesNotThrow() {
        // The exact values from the production crash: min 2.9802322E-8, max 0.0
        val v = 0.5f.safeCoerceIn(2.9802322E-8f, 0f)
        assertTrue(v in 0f..1e-7f)
    }

    @Test fun normalRangeStillClamps() {
        assertEquals(1f, 5f.safeCoerceIn(0f, 1f), 0f)
        assertEquals(0f, (-5f).safeCoerceIn(0f, 1f), 0f)
        assertEquals(0.4f, 0.4f.safeCoerceIn(0f, 1f), 0f)
    }

    @Test fun nanIsHandled() {
        assertEquals(0f, Float.NaN.safeCoerceIn(0f, 1f), 0f)
        assertEquals(0.5f, 0.5f.safeCoerceIn(Float.NaN, 1f), 0f)
    }

    @Test fun intAndLongInvertedRanges() {
        assertEquals(5, 9.safeCoerceIn(5, 3))
        assertEquals(5L, 9L.safeCoerceIn(5L, 3L))
    }
}
