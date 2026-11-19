package com.frameout.app.billing

import com.frameout.core.pro.ProEditGuard
import com.frameout.core.pro.ProFeature
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LockGateOutputTest {
    private val report = ProEditGuard.LockReport(0, 1, null, linkedSetOf(ProFeature.AdvancedExport))

    @Test
    fun withoutALockTheActionRunsAtOnceWithNoNote() {
        val shown = mutableListOf<ProEditGuard.LockReport?>()
        var note: String? = "unset"
        LockGate { shown += it }.gateOutput(null, remove = { "x" }, run = { note = it })
        assertNull(note)
        assertTrue(shown.isEmpty())
    }

    @Test
    fun theActionWaitsForTheDecision() {
        val shown = mutableListOf<ProEditGuard.LockReport?>()
        var ran = 0
        LockGate { shown += it }.gateOutput(report, remove = { "x" }, run = { ran++ })
        assertEquals(0, ran)
        assertNotNull(shown.single())
    }

    @Test
    fun removingRevertsFirstThenContinuesWithItsNote() {
        val order = mutableListOf<String>()
        val shown = mutableListOf<ProEditGuard.LockReport?>()
        var note: String? = null
        val gate = LockGate { shown += it }
        gate.gateOutput(report, remove = { order += "revert"; "changed to video" }, run = { order += "run"; note = it })
        gate.removeAndSave()
        assertEquals(listOf("revert", "run"), order)
        assertEquals("changed to video", note)
        assertNull(shown.last())
    }

    @Test
    fun dismissingRunsNothingAndRevertsNothing() {
        var reverted = false
        var ran = false
        val shown = mutableListOf<ProEditGuard.LockReport?>()
        val gate = LockGate { shown += it }
        gate.gateOutput(report, remove = { reverted = true; "x" }, run = { ran = true })
        gate.dismiss()
        assertFalse(reverted)
        assertFalse(ran)
        assertNull(shown.last())
    }
}
