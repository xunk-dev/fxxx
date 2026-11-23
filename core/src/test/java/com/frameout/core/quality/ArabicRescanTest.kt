package com.frameout.core.quality

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArabicRescanTest {
    private class MemoryStore(var ids: Set<Long> = emptySet()) : PendingStore {
        override fun load(): Set<Long> = ids
        override fun save(ids: Set<Long>) { this.ids = ids }
    }

    @Test
    fun aScanStartedWithoutTheModelIsMarked() {
        assertTrue(ArabicRescan.shouldMark(readyAtStart = false))
        assertFalse(ArabicRescan.shouldMark(readyAtStart = true))
    }

    @Test
    fun nothingIsDueUntilTheModelIsReady() {
        assertEquals(emptySet<Long>(), ArabicRescan.due(false, setOf(1L, 2L)))
        assertEquals(setOf(1L, 2L), ArabicRescan.due(true, setOf(1L, 2L)))
    }

    @Test
    fun beginScanMarksOnlyWhenTheModelIsMissing() {
        val store = MemoryStore()
        val rescan = ArabicRescan(store)
        rescan.beginScan(7, ready = true)
        assertEquals(emptySet<Long>(), store.ids)
        rescan.beginScan(7, ready = false)
        rescan.beginScan(9, ready = false)
        assertEquals(setOf(7L, 9L), store.ids)
    }

    @Test
    fun takeKeepsTheMarksWhileNotReadyAndConsumesThemOnce() {
        val store = MemoryStore(setOf(3L, 4L))
        val rescan = ArabicRescan(store)
        assertEquals(emptySet<Long>(), rescan.take(false))
        assertEquals(setOf(3L, 4L), store.ids)
        assertEquals(setOf(3L, 4L), rescan.take(true))
        assertEquals(emptySet<Long>(), store.ids)
        assertEquals(emptySet<Long>(), rescan.take(true))
    }

    @Test
    fun releaseClearsEveryDueProjectOnce() = runBlocking {
        val store = MemoryStore(setOf(1L, 2L))
        val cleared = ArrayList<Long>()
        val first = ArabicRescan(store).release(true) { cleared += it }
        val second = ArabicRescan(store).release(true) { cleared += it }
        assertEquals(2, first)
        assertEquals(0, second)
        assertEquals(listOf(1L, 2L), cleared.sorted())
    }

    @Test
    fun aFailedClearIsKeptForTheNextTime() = runBlocking {
        val store = MemoryStore(setOf(1L, 2L))
        val done = ArabicRescan(store).release(true) { if (it == 1L) throw IllegalStateException("db") }
        assertEquals(1, done)
        assertEquals(setOf(1L), store.ids)
    }

    @Test
    fun aCancelledClearIsKeptAndRethrown() {
        val store = MemoryStore(setOf(5L))
        var thrown = false
        try {
            runBlocking { ArabicRescan(store).release(true) { throw CancellationException("stop") } }
        } catch (e: CancellationException) {
            thrown = true
        }
        assertTrue(thrown)
        assertEquals(setOf(5L), store.ids)
    }

    @Test
    fun theCodecRoundTripsAndIgnoresGarbage() {
        assertEquals("1,5,12", PendingCodec.encode(setOf(12L, 1L, 5L)))
        assertEquals(setOf(1L, 5L, 12L), PendingCodec.decode("1,5,12"))
        assertEquals(setOf(4L), PendingCodec.decode(" 4 ,x,,"))
        assertEquals(emptySet<Long>(), PendingCodec.decode(""))
    }
}
