package com.frameout.core.pro

class AccentSeal(private val store: GrantStore, val active: Boolean) {
    fun key(): String? = if (active) store.read()?.takeIf(::valid) else null

    fun set(key: String) {
        if (active && valid(key)) store.write(key)
    }

    fun adopt(current: String) {
        if (key() == null) set(current)
    }

    fun restoreTarget(current: String): String? = key()?.takeIf { it != current }

    private fun valid(key: String): Boolean = key == ProCatalog.DYNAMIC_ACCENT || ProCatalog.welcomePickable(key)
}
