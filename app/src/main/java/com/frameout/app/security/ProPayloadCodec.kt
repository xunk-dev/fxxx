package com.frameout.app.security

import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.IOException

internal object ProPayloadCodec {
    const val MATRIX_SIZE = 20
    private const val MAX_ENTRIES = 256

    fun parse(bytes: ByteArray): Map<String, FloatArray>? =
        try {
            read(DataInputStream(ByteArrayInputStream(bytes)))
        } catch (e: IOException) {
            null
        }

    private fun read(input: DataInputStream): Map<String, FloatArray>? {
        val count = input.readUnsignedShort()
        if (count > MAX_ENTRIES) return null
        val out = LinkedHashMap<String, FloatArray>(count)
        repeat(count) {
            val id = readId(input) ?: return null
            val matrix = FloatArray(MATRIX_SIZE) { input.readFloat() }
            if (matrix.any { !it.isFinite() } || out.put(id, matrix) != null) return null
        }
        return if (input.available() == 0) out else null
    }

    private fun readId(input: DataInputStream): String? {
        val length = input.readUnsignedByte()
        if (length == 0) return null
        val raw = ByteArray(length)
        input.readFully(raw)
        return String(raw, Charsets.UTF_8)
    }
}
