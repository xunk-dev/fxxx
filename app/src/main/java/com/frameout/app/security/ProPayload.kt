package com.frameout.app.security

import android.content.Context
import com.frameout.app.BuildConfig

internal object ProPayload {
    private const val ASSET = "pro_payload.bin"

    fun load(context: Context): Map<String, FloatArray> {
        val blob = runCatching { context.assets.open(ASSET).use { it.readBytes() } }.getOrNull() ?: return emptyMap()
        val payload = unwrap(context, blob) ?: return emptyMap()
        return ProPayloadCodec.parse(payload).orEmpty()
    }

    private fun unwrap(context: Context, blob: ByteArray): ByteArray? {
        if (BuildConfig.CERT_SHA256.isEmpty()) return PayloadCrypto.openPlain(blob)
        val fingerprint = NativeGuard.fingerprint(context.applicationInfo.sourceDir) ?: return null
        return try {
            PayloadCrypto.open(blob, fingerprint)
        } finally {
            fingerprint.fill(0)
        }
    }
}
