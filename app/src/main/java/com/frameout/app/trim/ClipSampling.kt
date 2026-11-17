package com.frameout.app.trim

import kotlin.math.sqrt

internal data class ClipSamples(val timesMs: List<Long>, val frameMs: Int, val capped: Boolean)

internal object ClipSampling {
    const val SIDE = 720
    const val QUALITY = 90
    const val FPS = 15
    const val MAX_FRAMES = 300
    const val MAX_SPAN_MS = 20_000L
    const val GIF_SIDE = 720
    const val GIF_FPS = 20
    const val GIF_MAX_FRAMES = 160
    private const val GIF_PIXEL_BUDGET = 48_000_000.0
    private const val GIF_MIN_SIDE = 160
    private const val MIN_FRAMES = 2

    fun plan(startMs: Long, endMs: Long, maxSpanMs: Long = MAX_SPAN_MS, fps: Int = FPS, maxFrames: Int = MAX_FRAMES): ClipSamples? {
        val available = endMs - startMs
        if (available <= 0L) return null
        val span = minOf(available, maxSpanMs)
        val interval = maxOf(1000L / fps, ceilDiv(span, maxFrames.toLong()))
        val times = generateSequence(startMs) { it + interval }.takeWhile { it < startMs + span }.toList()
        if (times.size < MIN_FRAMES) return null
        return ClipSamples(times, interval.toInt(), available > maxSpanMs)
    }

    fun gifSide(width: Int, height: Int, frames: Int): Int {
        val long = maxOf(width, height).coerceAtLeast(1)
        val short = minOf(width, height).coerceAtLeast(1)
        val fit = sqrt(GIF_PIXEL_BUDGET / frames.coerceAtLeast(1) * long / short).toInt()
        return minOf(GIF_SIDE, fit).coerceAtLeast(GIF_MIN_SIDE)
    }

    private fun ceilDiv(value: Long, divisor: Long): Long = (value + divisor - 1L) / divisor
}
