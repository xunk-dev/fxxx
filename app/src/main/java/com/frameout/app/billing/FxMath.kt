package com.frameout.app.billing

internal fun seg(t: Float, from: Float, to: Float): Float {
    val x = ((t - from) / (to - from)).coerceIn(0f, 1f)
    return x * x * (3f - 2f * x)
}

internal fun easeOut(p: Float): Float = 1f - (1f - p) * (1f - p) * (1f - p)

internal fun easeOutBack(p: Float): Float {
    val c1 = 1.70158f
    val c3 = c1 + 1f
    val x = p - 1f
    return 1f + c3 * x * x * x + c1 * x * x
}

internal fun unitHash(seed: Float): Float {
    val v = kotlin.math.sin(seed * 12.9898f) * 43758.547f
    return v - kotlin.math.floor(v)
}
