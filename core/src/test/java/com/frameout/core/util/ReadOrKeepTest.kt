package com.frameout.core.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadOrKeepTest {
    @Test
    fun aSuccessfulReadReplacesThePreviousValue() {
        assertEquals(listOf(2), runBlocking { readOrKeep(listOf(1)) { listOf(2) } })
    }

    @Test
    fun aFailedReadKeepsThePreviousValue() {
        assertEquals(listOf(1), runBlocking { readOrKeep(listOf(1)) { error("db") } })
    }

    @Test
    fun cancellationIsNeverSwallowed() {
        val thrown = runBlocking { runCatching { readOrKeep(0) { throw CancellationException("stop") } }.exceptionOrNull() }
        assertTrue(thrown is CancellationException)
    }
}
