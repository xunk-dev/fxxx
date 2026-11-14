package com.frameout.core.export

class GifDelay(private val frameMs: Int) {
    private var carryMs = 0

    fun nextCentiseconds(): Int {
        carryMs += frameMs.coerceAtLeast(0)
        val whole = carryMs / UNIT_MS
        carryMs -= whole * UNIT_MS
        return whole.coerceAtLeast(MIN_CS)
    }

    private companion object {
        const val UNIT_MS = 10
        const val MIN_CS = 2
    }
}
