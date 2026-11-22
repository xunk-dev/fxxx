package com.frameout.core.quality

import android.graphics.Bitmap
import com.frameout.core.db.FrameDao
import com.frameout.core.db.FrameEntity
import com.frameout.core.util.ThumbCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

object FrameScan {
    private const val PAGE = 400

    suspend fun <T> mapThumbnails(dao: FrameDao, projectId: Long, accept: (FrameEntity) -> Boolean = { true }, block: suspend (FrameEntity, Bitmap) -> T): List<T> =
        withContext(Dispatchers.IO) {
            val out = ArrayList<T>()
            var offset = 0
            while (true) {
                val page = dao.pageActive(projectId, PAGE, offset)
                if (page.isEmpty()) break
                page.forEach { frame ->
                    ensureActive()
                    if (!accept(frame)) return@forEach
                    val bitmap = ThumbCache.decodePooled(frame.thumbnailPath ?: frame.fullPath) ?: return@forEach
                    try {
                        out += block(frame, bitmap)
                    } finally {
                        ThumbCache.release(bitmap)
                    }
                }
                offset += page.size
            }
            out
        }

    suspend fun <T : Any> mapThumbnailsParallel(
        dao: FrameDao,
        projectId: Long,
        total: Int,
        parallel: Int,
        onProgress: (Int, Int) -> Unit = { _, _ -> },
        block: (FrameEntity, Bitmap) -> T
    ): List<T> = withContext(Dispatchers.IO) {
        val out = ArrayList<T>()
        val permits = Semaphore(parallel.coerceAtLeast(1))
        val finished = java.util.concurrent.atomic.AtomicInteger()
        var offset = 0
        while (true) {
            val page = dao.pageActive(projectId, PAGE, offset)
            if (page.isEmpty()) break
            val results = coroutineScope {
                page.map { frame ->
                    async {
                        permits.withPermit {
                            ensureActive()
                            val bitmap = ThumbCache.decodePooled(frame.thumbnailPath ?: frame.fullPath)
                            val value = if (bitmap == null) null else try {
                                block(frame, bitmap)
                            } finally {
                                ThumbCache.release(bitmap)
                            }
                            onProgress(finished.incrementAndGet(), total)
                            value
                        }
                    }
                }.awaitAll()
            }
            out += results.filterNotNull()
            offset += page.size
        }
        out
    }
}
