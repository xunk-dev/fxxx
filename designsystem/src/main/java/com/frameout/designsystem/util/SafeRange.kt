package com.frameout.designsystem.util

/**
 * coerceIn that never throws. Kotlin's coerceIn throws IllegalArgumentException when
 * max < min, which happens easily with float rounding (e.g. min = 2.98e-8, max = 0f).
 * When the range is inverted or NaN we collapse it to a single point instead of crashing.
 */
fun Float.safeCoerceIn(min: Float, max: Float): Float {
    if (this.isNaN()) return if (min.isNaN()) 0f else min
    if (min.isNaN() || max.isNaN()) return this
    if (min > max) return (min + max) / 2f
    return if (this < min) min else if (this > max) max else this
}

fun Double.safeCoerceIn(min: Double, max: Double): Double {
    if (this.isNaN()) return if (min.isNaN()) 0.0 else min
    if (min.isNaN() || max.isNaN()) return this
    if (min > max) return (min + max) / 2.0
    return if (this < min) min else if (this > max) max else this
}

fun Int.safeCoerceIn(min: Int, max: Int): Int =
    if (min > max) min else if (this < min) min else if (this > max) max else this

fun Long.safeCoerceIn(min: Long, max: Long): Long =
    if (min > max) min else if (this < min) min else if (this > max) max else this
