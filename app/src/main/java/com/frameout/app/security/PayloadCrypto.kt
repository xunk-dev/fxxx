package com.frameout.app.security

import java.security.GeneralSecurityException
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

internal object PayloadCrypto {
    const val FINGERPRINT_BYTES = 32
    const val IV_BYTES = 12
    const val TAG_BITS = 128
    private const val HMAC = "HmacSHA256"
    private const val CIPHER = "AES/GCM/NoPadding"
    private val INFO = "frameout.pro.payload.v1".toByteArray(Charsets.US_ASCII)

    val MAGIC_SEALED = byteArrayOf(0x46, 0x58, 0x50, 0x45)
    val MAGIC_PLAIN = byteArrayOf(0x46, 0x58, 0x50, 0x50)

    fun deriveKey(fingerprint: ByteArray): ByteArray {
        val mac = Mac.getInstance(HMAC)
        mac.init(SecretKeySpec(fingerprint, HMAC))
        mac.update(INFO)
        mac.update(1.toByte())
        return mac.doFinal()
    }

    fun open(blob: ByteArray, fingerprint: ByteArray): ByteArray? {
        if (fingerprint.size != FINGERPRINT_BYTES) return null
        val header = MAGIC_SEALED.size + IV_BYTES
        if (blob.size < header + TAG_BITS / 8 || !blob.startsWith(MAGIC_SEALED)) return null
        val key = deriveKey(fingerprint)
        return try {
            val cipher = Cipher.getInstance(CIPHER)
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, blob, MAGIC_SEALED.size, IV_BYTES))
            cipher.updateAAD(MAGIC_SEALED)
            cipher.doFinal(blob, header, blob.size - header)
        } catch (e: GeneralSecurityException) {
            null
        } finally {
            key.fill(0)
        }
    }

    fun openPlain(blob: ByteArray): ByteArray? =
        if (blob.startsWith(MAGIC_PLAIN)) blob.copyOfRange(MAGIC_PLAIN.size, blob.size) else null

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean =
        size >= prefix.size && prefix.indices.all { this[it] == prefix[it] }
}
