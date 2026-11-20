package com.frameout.core.edit

object ProFilterSlot {
    private var source: (() -> Map<String, FloatArray>)? = null
    @Volatile private var loaded: Map<String, FloatArray>? = null

    fun install(provider: () -> Map<String, FloatArray>) {
        synchronized(this) {
            source = provider
            loaded = null
        }
    }

    fun warm() {
        table()
    }

    fun matrix(id: String): FloatArray {
        val found = table()[id]
        return if (found != null && found.size == MATRIX_SIZE) found.copyOf() else ColorOps.identity()
    }

    private fun table(): Map<String, FloatArray> {
        loaded?.let { return it }
        return synchronized(this) {
            loaded ?: (source?.let { runCatching { it() }.getOrNull() } ?: emptyMap()).also { loaded = it }
        }
    }

    private const val MATRIX_SIZE = 20
}
