package com.frameout.core.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class FanoutTest {
    @Test
    fun keepsTheOrderOfTheInput() = runBlocking {
        val out = Fanout.map((1..20).toList(), 4) { n ->
            delay((21 - n).toLong())
            n * 2
        }
        assertEquals((1..20).map { it * 2 }, out)
    }

    @Test
    fun neverRunsMoreThanTheWidthAtOnce() = runBlocking {
        val running = AtomicInteger()
        val peak = AtomicInteger()
        Fanout.forEach((1..24).toList(), 3) {
            val now = running.incrementAndGet()
            peak.updateAndGet { maxOf(it, now) }
            delay(5)
            running.decrementAndGet()
        }
        assertTrue(peak.get() in 2..3)
        assertEquals(0, running.get())
    }

    @Test
    fun smallListsAndWidthOneStaySequential() = runBlocking {
        val order = ArrayList<Int>()
        Fanout.forEach(listOf(1, 2, 3), 1) { order.add(it) }
        Fanout.forEach(listOf(4), 4) { order.add(it) }
        assertEquals(listOf(1, 2, 3, 4), order)
    }

    @Test
    fun aFailureCancelsTheRest() {
        val started = AtomicInteger()
        val result = runCatching {
            runBlocking {
                Fanout.map((1..50).toList(), 2) { n ->
                    started.incrementAndGet()
                    if (n == 3) error("boom")
                    delay(20)
                    n
                }
            }
        }
        assertTrue(result.isFailure)
        assertTrue(started.get() < 50)
    }
}
