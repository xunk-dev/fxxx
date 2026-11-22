package com.frameout.core.media

import java.util.concurrent.atomic.AtomicInteger

internal object ProbeStats {
    val fileReads = AtomicInteger()
    val cacheHits = AtomicInteger()
    val metadataQueries = AtomicInteger()

    fun reset() {
        fileReads.set(0)
        cacheHits.set(0)
        metadataQueries.set(0)
    }

    fun describe(): String = "fileReads=${fileReads.get()} cacheHits=${cacheHits.get()} metaQueries=${metadataQueries.get()}"
}
