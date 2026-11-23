package com.frameout.app.security

import android.content.Context
import com.frameout.app.BuildConfig
import com.frameout.core.pro.Entitlements
import com.frameout.core.util.DiagLog
import com.google.android.gms.tasks.Task
import com.google.firebase.functions.FirebaseFunctions
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

enum class ServerVerdict { Genuine, Rejected, Unavailable }

object IntegrityVerifier {
    private const val REGION = "us-central1"

    val enabled: Boolean get() = BuildConfig.PLAY_BILLING && BuildConfig.INTEGRITY_CLOUD_PROJECT > 0L

    suspend fun check(context: Context): ServerVerdict {
        if (!enabled) return ServerVerdict.Unavailable
        return try {
            val functions = FirebaseFunctions.getInstance(REGION)
            val issued = functions.getHttpsCallable("issueIntegrityNonce").call().await().data as? Map<*, *>
            val nonce = issued?.get("nonce") as? String ?: return ServerVerdict.Unavailable
            val token = PlayIntegrityGuard.requestIntegrityToken(context, nonce)
            val reply = functions.getHttpsCallable("verifyIntegrityToken")
                .call(mapOf("token" to token, "nonce" to nonce)).await().data as? Map<*, *>
            when (reply?.get("isGenuine")) {
                true -> ServerVerdict.Genuine
                false -> ServerVerdict.Rejected
                else -> ServerVerdict.Unavailable
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ServerVerdict.Unavailable
        }
    }

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(it) }
        addOnFailureListener { cont.resumeWithException(it) }
        addOnCanceledListener { cont.cancel() }
    }
}

object ServerIntegrity {
    private const val RETRY_MS = 5 * 60_000L
    private const val RECHECK_MS = 6 * 60 * 60_000L

    fun install(context: Context, entitlements: Entitlements, scope: CoroutineScope) {
        if (!IntegrityVerifier.enabled) return
        val app = context.applicationContext
        scope.launch(Dispatchers.IO) {
            delay(Random.nextLong(30_000L, 90_000L))
            var rejections = 0
            while (isActive) {
                when (IntegrityVerifier.check(app)) {
                    ServerVerdict.Genuine -> rejections = 0
                    ServerVerdict.Rejected -> {
                        rejections++
                        DiagLog.append("integrity", "server=rejected n=$rejections")
                        if (rejections >= 2) entitlements.applyIntegrity(tampered = false, hooked = true)
                    }
                    ServerVerdict.Unavailable -> Unit
                }
                delay(if (rejections == 1) RETRY_MS else RECHECK_MS)
            }
        }
    }
}
