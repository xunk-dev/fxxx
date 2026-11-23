package com.frameout.core.quality

import com.frameout.core.db.FrameTagDao
import com.frameout.core.prefs.SessionPrefs
import com.frameout.core.util.DetectorHealth
import kotlinx.coroutines.CancellationException

interface PendingStore {
    fun load(): Set<Long>
    fun save(ids: Set<Long>)
}

object PendingCodec {
    fun encode(ids: Set<Long>): String = ids.sorted().joinToString(",")

    fun decode(raw: String): Set<Long> = raw.split(',').mapNotNull { it.trim().toLongOrNull() }.toSet()
}

class PrefsPendingStore(private val prefs: SessionPrefs) : PendingStore {
    override fun load(): Set<Long> = PendingCodec.decode(prefs.arabicRescanRaw())

    override fun save(ids: Set<Long>) = prefs.setArabicRescanRaw(PendingCodec.encode(ids))
}

class ArabicRescan(private val store: PendingStore) {
    private val lock = Any()

    fun mark(projectId: Long) = synchronized(lock) { store.save(store.load() + projectId) }

    fun pending(): Set<Long> = synchronized(lock) { store.load() }

    fun take(ready: Boolean): Set<Long> = synchronized(lock) {
        val all = store.load()
        val due = due(ready, all)
        if (due.isNotEmpty()) store.save(all - due)
        due
    }

    suspend fun release(ready: Boolean, clear: suspend (Long) -> Unit): Int {
        var cleared = 0
        for (id in take(ready)) {
            try {
                clear(id)
                cleared++
            } catch (e: CancellationException) {
                mark(id)
                throw e
            } catch (e: Exception) {
                DetectorHealth.failed("arabic-rescan", e)
                mark(id)
            }
        }
        return cleared
    }

    suspend fun releaseTo(dao: FrameTagDao, ready: Boolean): Int =
        release(ready) { id -> dao.clearScansWithoutTag(id, TagRules.TEXT) }

    fun beginScan(projectId: Long, ready: Boolean) {
        if (shouldMark(ready)) mark(projectId)
    }

    companion object {
        fun shouldMark(readyAtStart: Boolean): Boolean = !readyAtStart

        fun due(ready: Boolean, pending: Set<Long>): Set<Long> = if (ready) pending else emptySet()
    }
}
