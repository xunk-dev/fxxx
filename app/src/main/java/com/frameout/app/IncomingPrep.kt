package com.frameout.app

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

object IncomingPrep {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Any()
    private var running = 0
    private val jobs = mutableSetOf<Job>()
    private val recent = mutableMapOf<String, Long>()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    private val _result = MutableStateFlow<IncomingMedia.Stabilized?>(null)
    val result: StateFlow<IncomingMedia.Stabilized?> = _result

    private fun fresh(requested: List<Uri>): List<Uri> = synchronized(lock) {
        val now = System.currentTimeMillis()
        recent.values.removeAll { now - it > DUPLICATE_WINDOW_MS }
        requested.distinct().filter { uri ->
            val key = uri.toString()
            if (recent.containsKey(key)) false else { recent[key] = now; true }
        }
    }

    private fun merge(result: IncomingMedia.Stabilized) = synchronized(lock) {
        val previous = _result.value
        _result.value = if (previous == null) result
        else IncomingMedia.Stabilized(
            previous.ready + result.ready, previous.failed + result.failed, minOf(previous.at, result.at), previous.rejected + result.rejected,
            previous.stills + result.stills, previous.stillsSkipped + result.stillsSkipped, previous.stillsFailed + result.stillsFailed,
            previous.stillUris + result.stillUris
        )
    }

    private fun finished(job: Job, app: Context) = synchronized(lock) {
        jobs -= job
        running--
        _busy.value = running > 0
        if (running == 0) {
            com.frameout.engine.work.ExtractNotifications.clearCopyBlocked(app)
            com.frameout.engine.work.ImportNotice.setCancelHook(null)
        }
    }

    fun start(context: Context, requested: List<Uri>) {
        if (requested.isEmpty()) return
        val uris = fresh(requested)
        if (uris.isEmpty()) return
        val app = context.applicationContext
        synchronized(lock) { running++; _busy.value = true }
        com.frameout.engine.work.ImportNotice.setCancelHook { cancel() }
        runCatching { androidx.core.content.ContextCompat.startForegroundService(app, android.content.Intent(app, IncomingCopyService::class.java)) }
        val job = scope.launch {
            val done: IncomingMedia.Stabilized? = try {
                IncomingMedia.stabilize(app, uris)
            } catch (cancelled: CancellationException) {
                null
            } catch (e: Exception) {
                IncomingMedia.Stabilized(emptyList(), uris.size)
            }
            merge(done ?: return@launch)
        }
        synchronized(lock) { jobs += job }
        job.invokeOnCompletion { finished(job, app) }
    }

    fun cancel() {
        val active = synchronized(lock) { recent.clear(); jobs.toList() }
        active.forEach { it.cancel() }
    }

    fun take(): IncomingMedia.Stabilized? = synchronized(lock) {
        val pending = _result.value
        _result.value = null
        pending?.takeIf { System.currentTimeMillis() - it.at <= STALE_AFTER_MS }
    }

    private const val DUPLICATE_WINDOW_MS = 10_000L
    private const val STALE_AFTER_MS = 30L * 60L * 1000L
}
