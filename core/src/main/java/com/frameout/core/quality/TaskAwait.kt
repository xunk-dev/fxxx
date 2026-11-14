package com.frameout.core.quality

import com.google.android.gms.tasks.Task
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class DetectorTimeout(what: String = "detector") : Exception("$what timed out")

internal const val DETECTOR_TIMEOUT_MS = 12_000L

private class Held<T>(val value: T)

internal suspend fun <T> Task<T>.await(timeoutMs: Long = DETECTOR_TIMEOUT_MS): T = withContext(NonCancellable) {
    val held = withTimeoutOrNull(timeoutMs) {
        suspendCancellableCoroutine<Held<T>> { cont ->
            addOnSuccessListener { if (cont.isActive) cont.resume(Held(it)) }
            addOnFailureListener { if (cont.isActive) cont.resumeWithException(it) }
            addOnCanceledListener { if (cont.isActive) cont.cancel() }
        }
    }
    (held ?: throw DetectorTimeout()).value
}
