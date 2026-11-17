package com.frameout.core.media

import android.content.Context
import androidx.paging.PagingSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

object MediaPrewarm {
    private const val START_DELAY_MS = 1_500L
    private const val FIRST_PAGE = 60
    private const val THUMBS = 60
    private const val PARALLEL = 8

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val started = AtomicBoolean(false)

    fun start(context: Context) {
        if (!started.compareAndSet(false, true)) return
        val app = context.applicationContext
        scope.launch {
            delay(START_DELAY_MS)
            if (!PermissionHelper.hasMediaAccess(app)) {
                started.set(false)
                return@launch
            }
            MediaChangeTracker.start(app)
            runCatching { MediaScanner(app).summary() }
            val result = runCatching {
                MediaStorePagingSource(app, null, null).load(PagingSource.LoadParams.Refresh<MediaKey>(null, FIRST_PAGE, false))
            }.getOrNull()
            val items = (result as? PagingSource.LoadResult.Page)?.data?.take(THUMBS).orEmpty()
            items.chunked(PARALLEL).forEach { batch ->
                coroutineScope { batch.forEach { item -> launch { runCatching { MediaThumbLoader.load(app, item, background = true) } } } }
            }
        }
    }
}
