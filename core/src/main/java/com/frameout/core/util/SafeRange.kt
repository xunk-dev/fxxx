package com.frameout.core.util

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
