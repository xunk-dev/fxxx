package com.frameout.core.quality

import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanThrottleTest {
    @Test
    fun interactiveScanIsCountedWhileItRunsAndReleasedAfter() = runBlocking {
        assertEquals(0, ScanThrottle.interactive.value)
        val seen = ScanThrottle.runInteractive { ScanThrottle.interactive.value }
        assertEquals(1, seen)
        assertEquals(0, ScanThrottle.interactive.value)
    }

    @Test
    fun aFailingInteractiveScanStillReleasesTheThrottle() {
        try {
            runBlocking { ScanThrottle.runInteractive<Unit> { throw IllegalStateException("boom") } }
        } catch (e: IllegalStateException) {
        }
        assertEquals(0, ScanThrottle.interactive.value)
    }

    @Test
    fun backgroundScansWaitForAnInteractiveScan() = runBlocking {
        var released = false
        var passed = false
        ScanThrottle.interactive.value = 1
        val waiter = async {
            ScanThrottle.awaitIdle()
            passed = true
            released
        }
        yield()
        assertFalse(passed)
        released = true
        ScanThrottle.interactive.value = 0
        assertTrue(waiter.await())
    }
}
