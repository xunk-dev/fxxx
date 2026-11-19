package com.frameout.core.collage

import kotlin.math.abs
import kotlin.math.ln

object CollageAutoAspect {
    private const val TEMPLATE_BIAS = 0.04

    fun choose(template: CollageTemplate, imageRatios: List<Float>): CollageAspect {
        val usable = template.cells.indices.mapNotNull { i -> imageRatios.getOrNull(i)?.takeIf { it > 0f }?.let { i to it } }
        if (usable.isEmpty()) return CollageAspect.Template
        val scored = CollageAspect.entries.map { it to loss(template, it, usable) }
        val best = scored.minBy { it.second }
        val base = scored.first { it.first == CollageAspect.Template }
        return if (base.second <= best.second + TEMPLATE_BIAS * usable.size) CollageAspect.Template else best.first
    }

    private fun loss(template: CollageTemplate, aspect: CollageAspect, cells: List<Pair<Int, Float>>): Double {
        val canvas = aspect.ratio ?: template.aspect
        return cells.sumOf { (i, image) ->
            val cell = template.cells[i]
            abs(ln((cell.w / cell.h * canvas / image).toDouble()))
        }
    }
}
