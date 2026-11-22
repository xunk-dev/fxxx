package com.frameout.core.export

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ZipNamesTest {
    @Test
    fun traversalAndAbsolutePathsAreFlattened() {
        assertEquals("passwd", ZipNames.safeFile("../../etc/passwd", "f"))
        assertEquals("evil.jpg", ZipNames.safeFile("..\\..\\evil.jpg", "f"))
        assertEquals("f", ZipNames.safeFile("..", "f"))
        assertEquals("f", ZipNames.safeFile("/", "f"))
        assertEquals("f", ZipNames.safeFile("   ", "f"))
    }

    @Test
    fun controlAndBidiCharactersAreRemoved() {
        val name = ZipNames.safeFile("photo\u202Egpj.exe\u0000", "f")
        assertFalse(name.any { it == '\u202E' || it == '\u0000' })
    }

    @Test
    fun windowsDeviceNamesAreDefused() {
        assertEquals("_CON.jpg", ZipNames.safeFile("CON.jpg", "f"))
        assertEquals("_nul", ZipNames.safeFile("nul", "f"))
    }

    @Test
    fun longNamesAreCappedKeepingExtension() {
        val name = ZipNames.safeFile("a".repeat(500) + ".jpg", "f")
        assertTrue(name.endsWith(".jpg"))
        assertTrue(name.toByteArray().size <= ZipNames.MAX_SEGMENT_BYTES)
    }

    @Test
    fun equivalentNamesCollapseToOneKey() {
        val set = ZipNameSet()
        val a = set.unique("Caf\u00E9.jpg")
        val b = set.unique("Cafe\u0301.jpg")
        val c = set.unique("CAF\u00C9.JPG")
        val d = set.unique("Caf\u00E9.jpg.")
        assertEquals(4, setOf(ZipNames.key(a), ZipNames.key(b), ZipNames.key(c), ZipNames.key(d)).size)
    }

    @Test
    fun suffixCandidateNeverCollidesWithARealName() {
        val set = ZipNameSet()
        val names = listOf("a.jpg", "a_2.jpg", "a.jpg", "a.jpg", "a_2.jpg").map { set.unique(it) }
        assertEquals(5, names.map { ZipNames.key(it) }.toSet().size)
    }

    @Test
    fun reservedNamesAreHonored() {
        val set = ZipNameSet(listOf("metadata.csv"))
        assertEquals("METADATA_2.CSV", set.unique("METADATA.CSV"))
    }

    @Test
    fun zipSessionRejectsUnsafeEntryNames() {
        val bad = listOf("../x", "/abs", "a/../b", "a\\b", "dir/", "", "a/./b", "x\u0000y")
        for (name in bad) {
            try {
                ZipSession.use(ByteArrayOutputStream()) { ZipSession.putBytes(it, name, ByteArray(1)) }
                fail("accepted $name")
            } catch (_: IllegalArgumentException) {
            }
        }
    }

    @Test
    fun zipSessionAcceptsDatasetPaths() {
        val out = ByteArrayOutputStream()
        ZipSession.use(out) {
            ZipSession.putBytes(it, "images/train/frame_000001.jpg", ByteArray(1))
            ZipSession.putBytes(it, "annotations/instances_val.json", ByteArray(1))
        }
        val names = ArrayList<String>()
        ZipInputStream(ByteArrayInputStream(out.toByteArray())).use { z -> while (true) names += (z.nextEntry ?: break).name }
        assertEquals(2, names.size)
    }

    @Test
    fun resolveNeverProducesAnUnsafeOrRepeatedName() {
        val file = File.createTempFile("frame", ".bin").apply { deleteOnExit() }
        fun f(n: String, i: Int) = StagedFrame(n, file, i.toLong(), i, i, 0L, 1, 1)
        val frames = listOf("../a.jpg", "a.jpg", "A.JPG", "CON.png", "..", "a.jpg.", "x\u202Ey.jpg").mapIndexed { i, n -> f(n, i + 1) }
        val names = ArchiveNames.resolve(frames, setOf("metadata.csv")).map { it.name }
        assertTrue(names.all { ZipNames.isSafePath(it) })
        assertEquals(names.size, names.map { ZipNames.key(it) }.toSet().size)
    }
}
