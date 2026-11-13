package com.frameout.core.pro

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface GrantStore {
    fun read(): String?

    fun write(value: String): Boolean
}

class PrefsGrantStore(private val prefs: SharedPreferences, private val key: String = "welcome_accent") : GrantStore {
    override fun read(): String? = runCatching { prefs.getString(key, null) }.getOrNull()

    override fun write(value: String): Boolean = runCatching { prefs.edit().putString(key, value).commit() }.getOrDefault(false)
}

class WelcomeGrant(private val store: GrantStore, private val persistent: Boolean = true) {
    private val saved: String? = if (persistent) store.read() else null
    private val _key = MutableStateFlow(saved?.takeIf(ProCatalog::welcomePickable))
    val key: StateFlow<String?> = _key.asStateFlow()

    private val _trial = MutableStateFlow(_key.value == null && saved == TRIAL)
    val trial: StateFlow<Boolean> = _trial.asStateFlow()

    private val _open = MutableStateFlow(_key.value == null && (saved == OPEN || saved == TRIAL))
    val open: StateFlow<Boolean> = _open.asStateFlow()

    val committed: Boolean get() = _key.value != null

    val spent: Boolean get() = committed || _trial.value

    val decided: Boolean get() = committed || _open.value

    fun defer(): Boolean {
        if (decided) return false
        val saved = !persistent || store.write(OPEN)
        _open.value = true
        return saved
    }

    fun beginTrial(spent: Boolean): Boolean {
        if (committed || !_open.value || _trial.value || spent || !persistent) return false
        if (!store.write(TRIAL)) return false
        _trial.value = true
        return true
    }

    fun settle(current: String): String? {
        if (!_trial.value) return null
        val chosen = current.takeIf(ProCatalog::welcomePickable) ?: FALLBACK
        if (!store.write(chosen)) return null
        _key.value = chosen
        _trial.value = false
        _open.value = false
        return chosen
    }

    fun exhaust() {
        if (committed || !_open.value || _trial.value) return
        if (persistent) store.write(FALLBACK)
        _key.value = FALLBACK
        _open.value = false
    }

    fun commit(candidate: String, spent: Boolean = false): Boolean {
        if (committed) return false
        val chosen = if (!spent && ProCatalog.welcomePickable(candidate)) candidate else FALLBACK
        val saved = !persistent || store.write(chosen)
        _key.value = chosen
        return saved
    }

    fun restore(key: String): Boolean {
        if (!ProCatalog.welcomePickable(key)) return false
        if (persistent) store.write(key)
        _key.value = key
        _open.value = false
        _trial.value = false
        return true
    }

    fun revoke(): String? {
        val old = _key.value ?: return null
        if (old == FALLBACK) return null
        if (persistent) store.write(FALLBACK)
        _key.value = FALLBACK
        return old
    }

    fun adoptExisting(onboardingDone: Boolean, current: String) {
        if (decided || !onboardingDone) return
        commit(if (current in LEGACY_FREE) current else FALLBACK)
    }

    companion object {
        const val FALLBACK = "indigo"
        const val OPEN = "open"
        const val TRIAL = "trial"
        private val LEGACY_FREE = setOf("indigo", "ocean", "black")
    }
}
