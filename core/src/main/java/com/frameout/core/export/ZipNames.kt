package com.frameout.core.export

import java.text.Normalizer
import java.util.Locale

object ZipNames {
    const val MAX_SEGMENT_BYTES = 200
    private const val MAX_PATH_BYTES = 480
    private val unsafeChars = Regex("[\\p{Cntrl}\\p{Cf}\\p{Zl}\\p{Zp}\\\\/:*?\"<>|]")
    private val reserved = Regex("^(con|prn|aux|nul|com[1-9]|lpt[1-9])(\\..*)?$", RegexOption.IGNORE_CASE)

    fun key(name: String): String =
        Normalizer.normalize(name, Normalizer.Form.NFC).lowercase(Locale.ROOT).trimEnd('.', ' ')

    fun safeFile(raw: String, fallbackStem: String): String {
        val last = raw.substringAfterLast('/').substringAfterLast('\\')
        val cleaned = unsafeChars.replace(Normalizer.normalize(last, Normalizer.Form.NFC), "_").trim().trim('.').trim()
        val base = if (cleaned.isBlank() || cleaned.all { it == '_' || it == '.' }) fallbackStem else cleaned
        val guarded = if (reserved.matches(base)) "_$base" else base
        return capBytes(guarded)
    }

    fun isSafePath(name: String): Boolean {
        if (name.isEmpty() || name.startsWith("/") || name.endsWith("/")) return false
        if (name.toByteArray(Charsets.UTF_8).size > MAX_PATH_BYTES) return false
        return name.split('/').all { part ->
            part.isNotEmpty() && part != "." && part != ".." &&
                part.toByteArray(Charsets.UTF_8).size <= MAX_SEGMENT_BYTES &&
                !unsafeChars.containsMatchIn(part) && part == part.trimEnd('.', ' ')
        }
    }

    fun requireSafePath(name: String): String {
        require(isSafePath(name)) { "Unsafe archive entry name" }
        return name
    }

    private fun capBytes(name: String): String {
        if (name.toByteArray(Charsets.UTF_8).size <= MAX_SEGMENT_BYTES) return name
        val dot = name.lastIndexOf('.')
        val ext = if (dot > 0 && name.length - dot <= 12) name.substring(dot) else ""
        var stem = name.removeSuffix(ext)
        while (stem.isNotEmpty() && (stem + ext).toByteArray(Charsets.UTF_8).size > MAX_SEGMENT_BYTES) stem = stem.dropLast(1)
        return stem.ifBlank { "file" } + ext
    }
}

class ZipNameSet(reserved: Collection<String> = emptyList()) {
    private val used = HashSet<String>()

    init {
        reserved.forEach { used.add(ZipNames.key(it)) }
    }

    fun unique(name: String): String {
        if (used.add(ZipNames.key(name))) return name
        val stem = name.substringBeforeLast('.', name)
        val ext = name.substringAfterLast('.', "").let { if (it.isEmpty() || stem == name) "" else ".$it" }
        var counter = 2
        while (true) {
            val candidate = "${stem}_$counter$ext"
            if (used.add(ZipNames.key(candidate))) return candidate
            counter++
        }
    }
}
