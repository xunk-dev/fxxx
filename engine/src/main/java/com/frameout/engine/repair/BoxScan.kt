package com.frameout.engine.repair

import java.nio.ByteBuffer
import java.nio.channels.FileChannel

object BoxScan {
    private const val HEADER = 16
    private val OPENING = setOf("ftyp", "moov", "mdat", "styp", "wide", "free", "skip")
    private val EBML = byteArrayOf(0x1A, 0x45, 0xDF.toByte(), 0xA3.toByte())

    private class State {
        var moov = false
        var moovCut = false
        var mdat = false
        var mdatCut = false
        var moof = false
        var moofCut = false
        var validEnd = 0L
        var bad = false
    }

    fun scan(channel: FileChannel): ContainerReport {
        val length = channel.size()
        if (length < 8) return ContainerReport(ContainerHealth.NotIsoBmff, 0L, length)
        if (isEbml(channel)) return ContainerReport(ContainerHealth.NotIsoBmff, 0L, length)
        val state = State()
        var pos = 0L
        var first = true
        while (pos + 8 <= length && !state.bad) {
            val box = readBox(channel, pos, length) ?: break
            if (first && box.type !in OPENING) {
                return ContainerReport(ContainerHealth.NotIsoBmff, 0L, length)
            }
            first = false
            note(state, box, length)
            if (box.end >= length) break
            pos = box.end
        }
        return report(state, length)
    }

    fun scan(bytes: ByteArray): ContainerReport {
        val file = java.io.File.createTempFile("scan", ".bin")
        return try {
            file.writeBytes(bytes)
            java.io.RandomAccessFile(file, "r").use { scan(it.channel) }
        } finally {
            file.delete()
        }
    }

    private class Box(val type: String, val start: Long, val end: Long, val valid: Boolean)

    private fun readBox(channel: FileChannel, pos: Long, length: Long): Box? {
        val head = ByteBuffer.allocate(HEADER)
        val read = channel.read(head, pos)
        if (read < 8) return null
        head.flip()
        val size32 = head.int.toLong() and 0xFFFFFFFFL
        val typeBytes = ByteArray(4).also { head.get(it) }
        val valid = typeBytes.all { it in 0x20..0x7E }
        val type = String(typeBytes, Charsets.ISO_8859_1)
        val size = when {
            !valid -> 0L
            size32 == 0L -> length - pos
            size32 == 1L -> if (read >= HEADER) head.long else 0L
            else -> size32
        }
        val sane = valid && size >= 8
        return Box(type, pos, if (sane) pos + size else pos, sane)
    }

    private fun note(state: State, box: Box, length: Long) {
        if (!box.valid) { state.bad = true; return }
        val cut = box.end > length
        when (box.type) {
            "moov" -> { state.moov = true; state.moovCut = cut }
            "mdat" -> { state.mdat = true; state.mdatCut = state.mdatCut || cut }
            "moof" -> { state.moof = true; state.moofCut = state.moofCut || cut }
        }
        if (!cut) state.validEnd = box.end
    }

    private fun report(state: State, length: Long): ContainerReport {
        val fragmented = state.moof
        val health = when {
            state.bad -> ContainerHealth.Damaged
            !state.moov || state.moovCut -> if (state.mdat || state.moof) ContainerHealth.MissingIndex else ContainerHealth.Damaged
            state.mdatCut || state.moofCut -> ContainerHealth.TruncatedData
            state.validEnd < length -> ContainerHealth.Damaged
            else -> ContainerHealth.Healthy
        }
        return ContainerReport(health, state.validEnd, length, fragmented)
    }

    private fun isEbml(channel: FileChannel): Boolean {
        val head = ByteBuffer.allocate(4)
        if (channel.read(head, 0L) < 4) return false
        return head.array().contentEquals(EBML)
    }
}
