package com.frameout.core.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlin.coroutines.CoroutineContext

object Fanout {
    private const val MAX_IO = 6
    private const val MAX_VIDEO = 4

    fun ioWidth(): Int = DeviceLimits.thumbDecodes().coerceIn(2, MAX_IO)

    fun videoWidth(): Int = (DeviceLimits.decodes() + 1).coerceIn(2, MAX_VIDEO)

    suspend fun <T, R> map(
        items: List<T>,
        width: Int = ioWidth(),
        context: CoroutineContext = Dispatchers.IO,
        transform: suspend (T) -> R
    ): List<R> {
        if (items.size < 2 || width < 2) return items.map { transform(it) }
        val gate = Semaphore(width)
        return coroutineScope {
            items.map { item -> async(context) { gate.withPermit { transform(item) } } }.awaitAll()
        }
    }

    suspend fun <T> forEach(
        items: List<T>,
        width: Int = ioWidth(),
        context: CoroutineContext = Dispatchers.IO,
        block: suspend (T) -> Unit
    ) {
        map(items, width, context, block)
    }
}
