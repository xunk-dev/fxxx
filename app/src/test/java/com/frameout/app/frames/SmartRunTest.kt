package com.frameout.app.frames

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartRunTest {
    private val result = setOf(1L, 2L) to "ok"

    @Test
    fun resetInvalidatesTheRunningAnalysisAndItsPendingFilter() {
        val run = SmartRun()
        run.pending = SmartFilter.People
        val token = run.seq
        run.reset()
        assertTrue(run.seq != token)
        assertNull(run.pending)
    }

    @Test
    fun aLaterResetAlwaysDiffersFromEarlierTokens() {
        val run = SmartRun()
        val tokens = (0 until 5).map { run.reset(); run.seq }
        assertEquals(tokens.size, tokens.toSet().size)
    }

    @Test
    fun cachedResultIsFoundByProjectFilterAndFrameCount() {
        val run = SmartRun()
        run.remember(SmartKey(7L, SmartFilter.People, 120), result)
        assertNotNull(run.cache[SmartKey(7L, SmartFilter.People, 120)])
        assertNull(run.cache[SmartKey(7L, SmartFilter.People, 119)])
        assertNull(run.cache[SmartKey(7L, SmartFilter.NoPeople, 120)])
        assertNull(run.cache[SmartKey(8L, SmartFilter.People, 120)])
    }

    @Test
    fun theCacheStaysSmall() {
        val run = SmartRun()
        for (i in 0 until 40) run.remember(SmartKey(i.toLong(), SmartFilter.Duplicates, 10), result)
        assertTrue(run.cache.size <= 12)
        assertNotNull(run.cache[SmartKey(39L, SmartFilter.Duplicates, 10)])
    }
}
