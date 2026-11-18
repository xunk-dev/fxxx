package com.frameout.app.security

import com.frameout.core.edit.ColorOps
import com.frameout.core.edit.FilterPreset
import com.frameout.core.edit.FilterPresets
import com.frameout.core.edit.ProFilterSlot
import com.frameout.core.pro.ProCatalog
import com.frameout.core.pro.ProTier
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProFilterPayloadTest {
    private fun duotone(sr: Float, sg: Float, sb: Float, hr: Float, hg: Float, hb: Float): FloatArray =
        ColorOps.chain(ColorOps.saturation(0f), ColorOps.channels((hr - sr) / 255f, (hg - sg) / 255f, (hb - sb) / 255f, sr, sg, sb))

    private val reference: List<FilterPreset> = listOf(
            FilterPreset("tealorange", ColorOps.chain(ColorOps.channels(1.15f, 1f, 0.88f, -10f, 6f, 14f), ColorOps.saturation(1.25f), ColorOps.contrast(1.12f))),
            FilterPreset("golden", ColorOps.chain(ColorOps.channels(1.2f, 1.06f, 0.78f, 10f, 6f, -6f), ColorOps.saturation(1.18f), ColorOps.contrast(1.06f))),
            FilterPreset("arctic", ColorOps.chain(ColorOps.channels(0.88f, 1f, 1.18f, -4f, 2f, 14f), ColorOps.saturation(0.9f), ColorOps.contrast(1.08f, 0.03f))),
            FilterPreset("neon", ColorOps.chain(ColorOps.saturation(1.7f), ColorOps.channels(1.1f, 0.92f, 1.2f, 0f, -4f, 8f), ColorOps.contrast(1.22f))),
            FilterPreset("emerald", ColorOps.chain(ColorOps.channels(0.9f, 1.1f, 0.95f, -4f, 6f, 0f), ColorOps.saturation(1.15f), ColorOps.contrast(1.1f))),
            FilterPreset("infrared", floatArrayOf(
                0.2f, 0.2f, 0.8f, 0f, 0f,
                0.5f, 0.5f, 0f, 0f, 0f,
                0.9f, 0.1f, 0f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )),
            FilterPreset("duo_ocean", duotone(8f, 18f, 60f, 120f, 230f, 255f)),
            FilterPreset("duo_sunset", duotone(54f, 12f, 84f, 255f, 170f, 90f)),
            FilterPreset("duo_rose", duotone(70f, 16f, 48f, 255f, 196f, 206f)),
            FilterPreset("portra", ColorOps.chain(ColorOps.saturation(0.92f), ColorOps.channels(1.05f, 1.01f, 0.95f, 8f, 5f, 4f), ColorOps.contrast(0.92f, 0.03f))),
            FilterPreset("velvia", ColorOps.chain(ColorOps.saturation(1.5f), ColorOps.channels(1f, 1.04f, 1.02f), ColorOps.contrast(1.2f, -0.03f))),
            FilterPreset("ektar", ColorOps.chain(ColorOps.saturation(1.35f), ColorOps.channels(1.08f, 1f, 0.94f, 4f, 0f, 0f), ColorOps.contrast(1.14f))),
            FilterPreset("cinestill", ColorOps.chain(ColorOps.channels(1.04f, 1.0f, 1.1f, -4f, 4f, 16f), ColorOps.saturation(1.1f), ColorOps.contrast(1.1f, -0.02f))),
            FilterPreset("bleach", ColorOps.chain(ColorOps.saturation(0.45f), ColorOps.contrast(1.4f, -0.04f))),
            FilterPreset("cross", ColorOps.chain(ColorOps.channels(1.15f, 1.02f, 0.82f, -8f, 8f, 22f), ColorOps.saturation(1.3f), ColorOps.contrast(1.18f))),
            FilterPreset("moody", ColorOps.chain(ColorOps.saturation(0.7f), ColorOps.channels(0.94f, 1f, 1.06f, -4f, 2f, 8f), ColorOps.contrast(1.25f, -0.06f))),
            FilterPreset("matcha", ColorOps.chain(ColorOps.channels(0.94f, 1.06f, 0.92f, 4f, 8f, 4f), ColorOps.saturation(0.85f), ColorOps.contrast(0.88f, 0.04f))),
            FilterPreset("pastel", ColorOps.chain(ColorOps.saturation(0.78f), ColorOps.channels(1.02f, 1f, 1.04f, 14f, 10f, 14f), ColorOps.contrast(0.82f, 0.05f))),
            FilterPreset("trix", ColorOps.chain(ColorOps.saturation(0f), ColorOps.contrast(1.5f, -0.07f), ColorOps.channels(1.02f, 1f, 0.98f)))
    )

    private val rows: List<List<String>> = File("payload/pro_filters.txt").readLines()
        .map { it.trim() }.filter { it.isNotEmpty() }.map { it.split(Regex("\\s+")) }

    private fun encoded(): ByteArray {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { out ->
            out.writeShort(rows.size)
            for (row in rows) {
                val id = row.first().toByteArray(Charsets.UTF_8)
                out.writeByte(id.size)
                out.write(id)
                row.drop(1).forEach { out.writeFloat(it.toFloat()) }
            }
        }
        return bytes.toByteArray()
    }

    private fun sealed(payload: ByteArray, fingerprint: ByteArray): ByteArray {
        val iv = ByteArray(PayloadCrypto.IV_BYTES).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(PayloadCrypto.deriveKey(fingerprint), "AES"), GCMParameterSpec(128, iv))
        cipher.updateAAD(PayloadCrypto.MAGIC_SEALED)
        return PayloadCrypto.MAGIC_SEALED + iv + cipher.doFinal(payload)
    }

    private fun proIds(): List<String> = FilterPresets.All.map { it.id }.filter { ProCatalog.filterTier(it) == ProTier.Pro }

    @After
    fun resetSlot() {
        ProFilterSlot.install { emptyMap() }
    }

    @Test
    fun payloadFileMatchesTheOriginalDefinitionsExactly() {
        assertEquals(reference.map { it.id }, rows.map { it.first() })
        reference.forEachIndexed { index, preset ->
            val values = FloatArray(20) { rows[index][it + 1].toFloat() }
            assertArrayEquals(preset.id, preset.matrix, values, 0f)
        }
    }

    @Test
    fun payloadCoversEveryProOnlyFilterAndNothingElse() {
        assertEquals(proIds().sorted(), rows.map { it.first() }.sorted())
    }

    @Test
    fun sealedPayloadRestoresEveryProFilter() {
        val fingerprint = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val blob = sealed(encoded(), fingerprint)
        val table = ProPayloadCodec.parse(PayloadCrypto.open(blob, fingerprint)!!)
        assertNotNull(table)
        ProFilterSlot.install { table!! }
        reference.forEach { assertArrayEquals(it.id, it.matrix, FilterPresets.matrix(it.id, 1f), 1e-6f) }
    }

    @Test
    fun wrongFingerprintLeavesProFiltersAsIdentity() {
        val blob = sealed(encoded(), ByteArray(32) { 1 })
        val table = PayloadCrypto.open(blob, ByteArray(32) { 2 })?.let(ProPayloadCodec::parse).orEmpty()
        ProFilterSlot.install { table }
        proIds().forEach { assertArrayEquals(it, ColorOps.identity(), FilterPresets.matrix(it, 1f), 0f) }
    }

    @Test
    fun freeAndAdFiltersDoNotDependOnThePayload() {
        ProFilterSlot.install { emptyMap() }
        val independent = FilterPresets.All.filter { ProCatalog.filterTier(it.id) != ProTier.Pro && !it.isNone }
        assertTrue(independent.isNotEmpty())
        independent.forEach { assertTrue(it.id, !FilterPresets.matrix(it.id, 1f).contentEquals(ColorOps.identity())) }
    }

    @Test
    fun brokenLoaderFallsBackToIdentityWithoutCrashing() {
        ProFilterSlot.install { error("boom") }
        proIds().forEach { assertArrayEquals(it, ColorOps.identity(), FilterPresets.matrix(it, 1f), 0f) }
    }
}
