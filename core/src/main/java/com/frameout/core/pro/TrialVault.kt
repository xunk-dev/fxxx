package com.frameout.core.pro

interface TrialVault {
    fun read(onResult: (ByteArray?) -> Unit)

    fun write(bytes: ByteArray, onDone: (Boolean) -> Unit)
}

data class TrialSnapshot(val state: Int, val wipes: Int, val granted: Boolean = false, val accent: String? = null)

object TrialRisk {
    const val MAX_WIPES = 3

    fun blocked(emulator: Boolean, clockRolledBack: Boolean, wipes: Int): Boolean =
        emulator || clockRolledBack || wipes >= MAX_WIPES

    fun looksLikeEmulator(fingerprint: String, hardware: String, model: String, product: String): Boolean {
        val f = fingerprint.lowercase(); val h = hardware.lowercase(); val m = model.lowercase(); val p = product.lowercase()
        return f.startsWith("generic") || f.contains("emulator") || f.contains("test-keys") && f.contains("sdk") ||
            h.contains("goldfish") || h.contains("ranchu") || m.contains("emulator") || m.contains("android sdk built for") ||
            p.contains("sdk_gphone") || p.contains("vbox") || p.contains("simulator")
    }
}

object TrialLedger {
    const val NEW = 0
    const val ARMED = 1
    const val USED = 2

    private const val PREFIX = "ft2:"
    private const val PREFIX_GRANT = "ft3:"
    private const val PREFIX_ACCENT = "ft4:"
    private const val GRANT_MASK = 0x6D
    private const val SALT = 0x5A17

    fun encode(state: Int, wipes: Int = 0, granted: Boolean = false, accent: String? = null): ByteArray {
        val s = state.coerceIn(NEW, USED)
        val w = wipes.coerceIn(0, 99)
        if (granted && accent != null && accent.all { it.isLetterOrDigit() || it == '_' }) {
            return "$PREFIX_ACCENT$s:$w:$accent:${accentSum(s, w, accent)}".toByteArray(Charsets.UTF_8)
        }
        if (granted) return "$PREFIX_GRANT$s:$w:1:${checksum(s, w) xor GRANT_MASK}".toByteArray(Charsets.UTF_8)
        return "$PREFIX$s:$w:${checksum(s, w)}".toByteArray(Charsets.UTF_8)
    }

    fun decode(bytes: ByteArray): TrialSnapshot? {
        if (bytes.isEmpty()) return null
        val bad = TrialSnapshot(USED, TrialRisk.MAX_WIPES)
        val text = runCatching { String(bytes, Charsets.UTF_8) }.getOrNull() ?: return bad
        if (text.startsWith(PREFIX_GRANT)) return decodeGranted(text) ?: bad
        if (text.startsWith(PREFIX_ACCENT)) return decodeAccent(text) ?: bad
        if (!text.startsWith(PREFIX)) return bad
        val parts = text.removePrefix(PREFIX).split(':')
        if (parts.size != 3) return bad
        val state = parts[0].toIntOrNull() ?: return bad
        val wipes = parts[1].toIntOrNull() ?: return bad
        if (state !in NEW..USED || wipes !in 0..99) return bad
        return if (parts[2].toIntOrNull() == checksum(state, wipes)) TrialSnapshot(state, wipes) else bad
    }

    private fun decodeGranted(text: String): TrialSnapshot? {
        val parts = text.removePrefix(PREFIX_GRANT).split(':')
        if (parts.size != 4 || parts[2] != "1") return null
        val state = parts[0].toIntOrNull() ?: return null
        val wipes = parts[1].toIntOrNull() ?: return null
        if (state !in NEW..USED || wipes !in 0..99) return null
        return if (parts[3].toIntOrNull() == (checksum(state, wipes) xor GRANT_MASK)) TrialSnapshot(state, wipes, true) else null
    }

    private fun decodeAccent(text: String): TrialSnapshot? {
        val parts = text.removePrefix(PREFIX_ACCENT).split(':')
        if (parts.size != 4) return null
        val state = parts[0].toIntOrNull() ?: return null
        val wipes = parts[1].toIntOrNull() ?: return null
        if (state !in NEW..USED || wipes !in 0..99) return null
        return if (parts[3].toIntOrNull() == accentSum(state, wipes, parts[2])) TrialSnapshot(state, wipes, true, parts[2]) else null
    }

    private fun accentSum(state: Int, wipes: Int, accent: String): Int =
        accent.fold(checksum(state, wipes) xor GRANT_MASK) { acc, c -> (acc * 31 + c.code) and 0x7FFFFFFF }

    fun merge(local: Int, remote: Int?): Int = maxOf(local.coerceIn(NEW, USED), remote ?: NEW)

    private fun checksum(state: Int, wipes: Int): Int = ((state * 31 + wipes) * 131 + SALT) xor 0x2B
}
