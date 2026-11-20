package com.frameout.core.quality

import android.os.SystemClock
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class DetectorTimeout(what: String = "detector") : Exception("$what timed out")

class DetectorCancelled : Exception("detector task was cancelled")

internal const val DETECTOR_TIMEOUT_MS = 12_000L

internal class ScanDeadline(val untilMs: Long, private val now: () -> Long = { SystemClock.elapsedRealtime() }) : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<ScanDeadline> {
        fun after(budgetMs: Long, now: () -> Long = { SystemClock.elapsedRealtime() }): ScanDeadline = ScanDeadline(now() + budgetMs, now)
    }

    fun remainingMs(): Long = untilMs - now()

    fun expired(): Boolean = remainingMs() <= 0L
}

internal suspend fun scanBudgetLeft(): Long? = coroutineContext[ScanDeadline]?.remainingMs()

private class Held<T>(val value: T)

internal suspend fun <T> Task<T>.await(timeoutMs: Long = DETECTOR_TIMEOUT_MS): T {
    val left = scanBudgetLeft()
    if (left != null && left <= 0L) throw DetectorTimeout("scan budget")
    val limit = if (left == null) timeoutMs else minOf(timeoutMs, left)
    return withContext(NonCancellable) {
        val held = withTimeoutOrNull(limit) {
            suspendCancellableCoroutine<Held<T>> { cont ->
                addOnSuccessListener { if (cont.isActive) cont.resume(Held(it)) }
                addOnFailureListener { if (cont.isActive) cont.resumeWithException(it) }
                addOnCanceledListener { if (cont.isActive) cont.resumeWithException(DetectorCancelled()) }
            }
        }
        (held ?: throw DetectorTimeout()).value
    }
}
