package com.frameout.core.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UltraHdrWriterTest {
    private fun jpeg(size: Int): ByteArray {
        val bytes = ByteArray(size) { 0x11 }
        bytes[0] = 0xFF.toByte(); bytes[1] = 0xD8.toByte()
        bytes[size - 2] = 0xFF.toByte(); bytes[size - 1] = 0xD9.toByte()
        return bytes
    }

    private fun int(b: ByteArray, at: Int) = ((b[at].toInt() and 255) shl 24) or ((b[at + 1].toInt() and 255) shl 16) or ((b[at + 2].toInt() and 255) shl 8) or (b[at + 3].toInt() and 255)

    private fun find(b: ByteArray, marker: Int): Int {
        var i = 2
        while (i + 3 < b.size) {
            if (b[i] != 0xFF.toByte()) return -1
            val m = b[i + 1].toInt() and 255
            if (m == marker) return i
            i += 2 + (((b[i + 2].toInt() and 255) shl 8) or (b[i + 3].toInt() and 255))
        }
        return -1
    }

    private val xml = UltraHdrWriter.gainXmp(floatArrayOf(1f, 1f, 1f), floatArrayOf(4f, 4f, 4f), 1f / 64f, 4f)

    @Test
    fun mpfPointsExactlyAtTheGainMapSoi() {
        val out = UltraHdrWriter.assemble(jpeg(500), jpeg(200), xml)
        val mpfAt = find(out, 0xE2)
        assertTrue(mpfAt > 0)
        val base = mpfAt + 8
        val entries = base + 50
        val primarySize = int(out, entries + 4)
        val gainSize = int(out, entries + 20)
        val gainOffset = int(out, entries + 24)
        assertEquals(out.size - gainSize, primarySize)
        assertEquals(primarySize, base + gainOffset)
        assertEquals(0xFF.toByte(), out[base + gainOffset])
        assertEquals(0xD8.toByte(), out[base + gainOffset + 1])
        assertEquals(0xD9.toByte(), out[out.size - 1])
    }

    @Test
    fun primaryXmpCarriesTheExactGainMapLength() {
        val out = UltraHdrWriter.assemble(jpeg(300), jpeg(120), xml)
        val mpfAt = find(out, 0xE2)
        val gainSize = int(out, mpfAt + 8 + 50 + 20)
        val text = String(out, Charsets.ISO_8859_1)
        assertTrue(text.contains("Item:Length=\"$gainSize\""))
        assertTrue(text.contains("hdrgm:Version=\"1.0\""))
    }

    @Test
    fun gainXmpUsesLog2AndDetectsPerChannelRanges() {
        assertTrue(xml.contains("hdrgm:GainMapMax=\"2\""))
        assertTrue(xml.contains("hdrgm:HDRCapacityMax=\"2\""))
        val mixed = UltraHdrWriter.gainXmp(floatArrayOf(1f, 1f, 1f), floatArrayOf(2f, 4f, 8f), 1f / 64f, 8f)
        assertTrue(mixed.contains("<hdrgm:GainMapMax><rdf:Seq>"))
        assertTrue(mixed.contains("<rdf:li>3</rdf:li>"))
    }

    @Test
    fun segmentsGoAfterAnExistingJfifHeader() {
        val base = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 0, 4, 1, 2, 0xFF.toByte(), 0xD9.toByte())
        assertEquals(8, JpegSegments.headerEnd(base))
        assertEquals(2, JpegSegments.headerEnd(jpeg(10)))
    }
}
