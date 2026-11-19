package com.frameout.app.security

import android.content.Context
import com.frameout.app.BuildConfig
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object PlayIntegrityGuard {

    suspend fun requestIntegrityToken(context: Context, serverNonce: String): String {
        val project = BuildConfig.INTEGRITY_CLOUD_PROJECT
        check(project > 0L) { "integrityCloudProject is not configured" }
        require(serverNonce.length in 16..500) { "nonce must come from the server (16-500 URL-safe base64 chars)" }
        val request = IntegrityTokenRequest.builder()
            .setCloudProjectNumber(project)
            .setNonce(serverNonce)
            .build()
        return suspendCancellableCoroutine { cont ->
            IntegrityManagerFactory.create(context).requestIntegrityToken(request)
                .addOnSuccessListener { response -> cont.resume(response.token()) }
                .addOnFailureListener { e -> cont.resumeWithException(e) }
        }
    }
}
