package com.frameout.core.media

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking

internal object ParallelProbe {
    private const val THREADS = 8
    private const val MIN_PARALLEL = 4

    private val dispatcher = Dispatchers.IO.limitedParallelism(THREADS)

    suspend fun <T, R> map(items: List<T>, transform: (T) -> R): List<R> {
        if (items.size < MIN_PARALLEL) return items.map(transform)
        return coroutineScope { items.map { async(dispatcher) { transform(it) } }.awaitAll() }
    }

    fun <T, R> mapBlocking(items: List<T>, transform: (T) -> R): List<R> {
        if (items.size < MIN_PARALLEL) return items.map(transform)
        return runBlocking(dispatcher) { items.map { async { transform(it) } }.awaitAll() }
    }
}
