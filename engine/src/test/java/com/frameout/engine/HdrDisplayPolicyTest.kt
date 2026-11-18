package com.frameout.engine

import com.frameout.engine.player.HdrDisplayPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HdrDisplayPolicyTest {
    @Test
    fun smallPreviewInsideUiStaysSdr() {
        assertFalse(HdrDisplayPolicy.useNativeSurface(35, true, 0.25f))
    }

    @Test
    fun screenFillingPlayerOnHdrDisplayGoesNative() {
        assertTrue(HdrDisplayPolicy.useNativeSurface(34, true, 0.85f))
    }

    @Test
    fun noHdrDisplayOrOldAndroidNeverGoesNative() {
        assertFalse(HdrDisplayPolicy.useNativeSurface(35, false, 1f))
        assertFalse(HdrDisplayPolicy.useNativeSurface(24, true, 1f))
    }

    @Test
    fun coverageHandlesUnmeasuredSizes() {
        assertEquals(0f, HdrDisplayPolicy.coverage(0L, 1000L), 0f)
        assertEquals(0f, HdrDisplayPolicy.coverage(1000L, 0L), 0f)
        assertEquals(0.5f, HdrDisplayPolicy.coverage(500L, 1000L), 0f)
    }

    @Test
    fun headroomIsCappedOnlyWhereSupported() {
        assertNull(HdrDisplayPolicy.headroomCap(34))
        assertEquals(HdrDisplayPolicy.CAPPED_HEADROOM, HdrDisplayPolicy.headroomCap(35)!!, 0f)
    }
}
