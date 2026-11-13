package com.frameout.engine

import com.frameout.engine.repair.BoxScan
import com.frameout.engine.repair.ContainerHealth
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BoxScanTest {
    private fun box(type: String, payload: Int): ByteArray {
        val out = ByteBuffer.allocate(8 + payload)
        out.putInt(8 + payload)
        out.put(type.toByteArray(Charsets.ISO_8859_1))
        return out.array()
    }

    private fun file(vararg parts: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        parts.forEach(out::write)
        return out.toByteArray()
    }

    private fun health(bytes: ByteArray) = BoxScan.scan(bytes).health

    @Test fun completeFileWithIndexFirstIsHealthy() =
        assertEquals(ContainerHealth.Healthy, health(file(box("ftyp", 8), box("moov", 40), box("mdat", 200))))

    @Test fun completeFileWithIndexLastIsHealthy() =
        assertEquals(ContainerHealth.Healthy, health(file(box("ftyp", 8), box("mdat", 200), box("moov", 40))))

    @Test fun cutMediaDataWithIndexFirstIsTruncated() {
        val whole = file(box("ftyp", 8), box("moov", 40), box("mdat", 200))
        val report = BoxScan.scan(whole.copyOf(whole.size - 90))
        assertEquals(ContainerHealth.TruncatedData, report.health)
        assertTrue(report.needsRepair)
        assertTrue(report.recoverable)
    }

    @Test fun cutMediaDataWithoutIndexIsMissingIndex() {
        val whole = file(box("ftyp", 8), box("mdat", 200), box("moov", 40))
        val report = BoxScan.scan(whole.copyOf(whole.size - 100))
        assertEquals(ContainerHealth.MissingIndex, report.health)
        assertFalse(report.recoverable)
        assertFalse(report.needsRepair)
    }

    @Test fun cutIndexIsMissingIndex() {
        val whole = file(box("ftyp", 8), box("mdat", 200), box("moov", 40))
        assertEquals(ContainerHealth.MissingIndex, health(whole.copyOf(whole.size - 10)))
    }

    @Test fun cutLastFragmentIsTruncated() {
        val whole = file(box("ftyp", 8), box("moov", 40), box("moof", 16), box("mdat", 100), box("moof", 16), box("mdat", 100))
        val report = BoxScan.scan(whole.copyOf(whole.size - 30))
        assertEquals(ContainerHealth.TruncatedData, report.health)
        assertTrue(report.fragmented)
    }

    @Test fun trailingJunkIsDamaged() =
        assertEquals(ContainerHealth.Damaged, health(file(box("ftyp", 8), box("moov", 40), box("mdat", 200), byteArrayOf(1, 2, 3))))

    @Test fun garbageBoxTypeMidFileIsDamaged() {
        val bad = ByteBuffer.allocate(16).putInt(16).put(byteArrayOf(0, 1, 2, 3)).array()
        assertEquals(ContainerHealth.Damaged, health(file(box("ftyp", 8), box("moov", 40), bad, box("mdat", 100))))
    }

    @Test fun matroskaAndUnknownFormatsAreLeftAlone() {
        assertEquals(ContainerHealth.NotIsoBmff, health(byteArrayOf(0x1A, 0x45, 0xDF.toByte(), 0xA3.toByte(), 1, 2, 3, 4, 5, 6)))
        assertEquals(ContainerHealth.NotIsoBmff, health(ByteArray(64) { 0x47 }))
    }

    @Test fun tinyInputIsNotIsoBmff() = assertEquals(ContainerHealth.NotIsoBmff, health(byteArrayOf(1, 2, 3)))

    @Test fun mdatWithZeroSizeRunsToEndOfFile() {
        val open = ByteBuffer.allocate(8 + 50).putInt(0).put("mdat".toByteArray()).array()
        assertEquals(ContainerHealth.Healthy, health(file(box("ftyp", 8), box("moov", 40), open)))
    }
}
