package com.frameout.app.trim

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln

internal class ScrubGlide private constructor(
    private val from: Double,
    val target: Double,
    private val tauS: Double,
    val durationS: Double
) {
    fun at(elapsedS: Double): Double =
        if (elapsedS >= durationS) target else from + (target - from) * (1.0 - exp(-elapsedS / tauS))

    companion object {
        fun plan(
            from: Double,
            velocity: Double,
            tauS: Double,
            lo: Double,
            hi: Double,
            settle: Double,
            snap: (Double) -> Double
        ): ScrubGlide? {
            if (velocity == 0.0 || tauS <= 0.0 || settle <= 0.0 || hi <= lo) return null
            val reach = (from + velocity * tauS).coerceIn(lo, hi)
            val target = snap(reach).coerceIn(lo, hi)
            val distance = target - from
            if (distance * velocity <= 0.0 || abs(distance) <= settle) return null
            val tau = distance / velocity
            return ScrubGlide(from, target, tau, tau * ln(abs(distance) / settle))
        }
    }
}
