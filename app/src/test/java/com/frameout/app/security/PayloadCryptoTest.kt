package com.frameout.app.security

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PayloadCryptoTest {
    private val fingerprint = hex("53a6d464bb615003eab4706fff8283344677b025df34f2bbaf2b233a84613956")
    private val key = hex("33b1be7076452abde1397b46778eebc57f38ac03aa6649302de920a58208e896")
    private val plaintext = hex("00010274313fc00000000000000000000000000000c1480000000000003fa0000000000000000000004040000000000000000000003f40000000000000000000000000000000000000000000003f80000000000000")
    private val blob = hex(
        "46585045000102030405060708090a0b064c82ce0e64029b4237f17077a9e26574362485fa2447ebc53d0b1a72aa953496b602fec0a917a94f859ee05bff188c12a056e1796c924e7ee0ee986f84440171f92b5fa61029d5e3ca8aaf588506ae2d8abaffd37976e1431f8a3bc391d62a5d30c0c5d5"
    )

    private fun hex(text: String): ByteArray = ByteArray(text.length / 2) { text.substring(it * 2, it * 2 + 2).toInt(16).toByte() }

    private fun seal(payload: ByteArray, fingerprint: ByteArray): ByteArray {
        val iv = ByteArray(PayloadCrypto.IV_BYTES).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(PayloadCrypto.deriveKey(fingerprint), "AES"), GCMParameterSpec(128, iv))
        cipher.updateAAD(PayloadCrypto.MAGIC_SEALED)
        return PayloadCrypto.MAGIC_SEALED + iv + cipher.doFinal(payload)
    }

    @Test
    fun derivedKeyMatchesFixedVector() {
        assertArrayEquals(key, PayloadCrypto.deriveKey(fingerprint))
    }

    @Test
    fun correctFingerprintOpensFixedBlob() {
        assertArrayEquals(plaintext, PayloadCrypto.open(blob, fingerprint))
    }

    @Test
    fun differentFingerprintFailsWithoutCrashing() {
        val other = fingerprint.copyOf().also { it[0] = (it[0].toInt() xor 1).toByte() }
        assertNull(PayloadCrypto.open(blob, other))
        assertNull(PayloadCrypto.open(blob, ByteArray(32)))
    }

    @Test
    fun randomFingerprintsNeverOpenTheBlob() {
        val random = SecureRandom()
        repeat(50) { assertNull(PayloadCrypto.open(blob, ByteArray(32).also { random.nextBytes(it) })) }
    }

    @Test
    fun wrongFingerprintLengthIsRejected() {
        assertNull(PayloadCrypto.open(blob, ByteArray(0)))
        assertNull(PayloadCrypto.open(blob, fingerprint.copyOf(31)))
        assertNull(PayloadCrypto.open(blob, fingerprint + byteArrayOf(0)))
    }

    @Test
    fun anyChangedByteIsRejected() {
        for (index in blob.indices) {
            val changed = blob.copyOf().also { it[index] = (it[index].toInt() xor 0x80).toByte() }
            assertNull("byte $index", PayloadCrypto.open(changed, fingerprint))
        }
    }

    @Test
    fun truncatedOrEmptyBlobIsRejected() {
        assertNull(PayloadCrypto.open(ByteArray(0), fingerprint))
        assertNull(PayloadCrypto.open(blob.copyOf(20), fingerprint))
        assertNull(PayloadCrypto.open(blob.copyOf(blob.size - 1), fingerprint))
    }

    @Test
    fun plainBlobIsNeverAcceptedAsSealed() {
        assertNull(PayloadCrypto.open(PayloadCrypto.MAGIC_PLAIN + plaintext, fingerprint))
        assertNull(PayloadCrypto.openPlain(blob))
        assertArrayEquals(plaintext, PayloadCrypto.openPlain(PayloadCrypto.MAGIC_PLAIN + plaintext))
        assertNull(PayloadCrypto.openPlain(ByteArray(2)))
    }

    @Test
    fun randomRoundTripOpensOnlyWithItsOwnFingerprint() {
        val random = SecureRandom()
        repeat(20) {
            val own = ByteArray(32).also { random.nextBytes(it) }
            val other = ByteArray(32).also { random.nextBytes(it) }
            val payload = ByteArray(1 + random.nextInt(2000)).also { random.nextBytes(it) }
            val sealed = seal(payload, own)
            assertArrayEquals(payload, PayloadCrypto.open(sealed, own))
            assertNull(PayloadCrypto.open(sealed, other))
        }
    }

    @Test
    fun fixedPlaintextParsesIntoItsMatrix() {
        val parsed = ProPayloadCodec.parse(plaintext)
        assertNotNull(parsed)
        assertEquals(setOf("t1"), parsed!!.keys)
        val expected = floatArrayOf(1.5f, 0f, 0f, 0f, -12.5f, 0f, 1.25f, 0f, 0f, 3f, 0f, 0f, 0.75f, 0f, 0f, 0f, 0f, 0f, 1f, 0f)
        assertArrayEquals(expected, parsed.getValue("t1"), 0f)
    }

    @Test
    fun codecRejectsMalformedInput() {
        assertNull(ProPayloadCodec.parse(ByteArray(0)))
        assertNull(ProPayloadCodec.parse(plaintext.copyOf(plaintext.size - 1)))
        assertNull(ProPayloadCodec.parse(plaintext + byteArrayOf(0)))
        val nan = plaintext.copyOf().also { it[5] = 0x7f; it[6] = 0xc0.toByte(); it[7] = 0; it[8] = 0 }
        assertNull(ProPayloadCodec.parse(nan))
        val tooMany = plaintext.copyOf().also { it[0] = 0x7f }
        assertNull(ProPayloadCodec.parse(tooMany))
    }
}
