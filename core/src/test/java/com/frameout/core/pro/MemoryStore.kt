package com.frameout.core.pro

internal class MemoryStore(var saved: String? = null, var failWrites: Boolean = false) : GrantStore {
    var writes = 0

    override fun read(): String? = saved

    override fun write(value: String): Boolean {
        writes++
        if (failWrites) return false
        saved = value
        return true
    }
}
