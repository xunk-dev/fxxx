package com.frameout.core.collage

import kotlin.math.abs
import com.frameout.core.util.safeCoerceIn

data class CellRect(
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float
)

data class PxRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}

data class CollageTemplate(
    val id: String,
    val label: String,
    val columns: Int,
    val rows: Int,
    val cells: List<CellRect>,
    val aspectOverride: Float? = null
) {
    val aspect: Float get() = aspectOverride ?: (columns.toFloat() / rows)

    fun withAspect(choice: CollageAspect): CollageTemplate = copy(aspectOverride = choice.ratio)
}

private const val FILL_EPS = 0.0005f

private class FillSlot(var rect: CellRect, val empty: Boolean)

private fun near(a: Float, b: Float): Boolean = abs(a - b) < FILL_EPS

private fun FillSlot.absorbInto(slots: MutableList<FillSlot>): Boolean {
    val e = rect
    for (dir in 0..3) {
        val group = slots.filter { s ->
            if (s === this) return@filter false
            val r = s.rect
            when (dir) {
                0 -> near(r.x + r.w, e.x) && r.y >= e.y - FILL_EPS && r.y + r.h <= e.y + e.h + FILL_EPS
                1 -> near(r.y + r.h, e.y) && r.x >= e.x - FILL_EPS && r.x + r.w <= e.x + e.w + FILL_EPS
                2 -> near(r.x, e.x + e.w) && r.y >= e.y - FILL_EPS && r.y + r.h <= e.y + e.h + FILL_EPS
                else -> near(r.y, e.y + e.h) && r.x >= e.x - FILL_EPS && r.x + r.w <= e.x + e.w + FILL_EPS
            }
        }
        if (group.isEmpty()) continue
        val span = group.sumOf { (if (dir % 2 == 0) it.rect.h else it.rect.w).toDouble() }.toFloat()
        if (!near(span, if (dir % 2 == 0) e.h else e.w)) continue
        group.forEach { s ->
            val r = s.rect
            s.rect = when (dir) {
                0 -> r.copy(w = r.w + e.w)
                1 -> r.copy(h = r.h + e.h)
                2 -> r.copy(x = r.x - e.w, w = r.w + e.w)
                else -> r.copy(y = r.y - e.h, h = r.h + e.h)
            }
        }
        slots.remove(this)
        return true
    }
    return false
}

fun CollageTemplate.compacted(filled: Int): CollageTemplate {
    val n = filled.safeCoerceIn(0, cells.size)
    if (n == 0 || n >= cells.size) return this
    val slots = cells.mapIndexed { i, c -> FillSlot(c, i >= n) }.toMutableList()
    var progress = true
    while (progress) {
        progress = false
        for (slot in slots.filter { it.empty }.asReversed()) {
            if (slot.absorbInto(slots)) {
                progress = true
                break
            }
        }
    }
    return copy(cells = slots.map { it.rect })
}

const val COLLAGE_GAP_MAX = 80
const val COLLAGE_GAP_DEFAULT = 12

const val COLLAGE_REFERENCE_WIDTH = 1536f
const val COLLAGE_CORNER_REFERENCE = 30f

fun PxRect.snapped(): PxRect {
    val l = Math.round(left)
    val t = Math.round(top)
    val r = maxOf(Math.round(right), l + 1)
    val b = maxOf(Math.round(bottom), t + 1)
    return PxRect(l.toFloat(), t.toFloat(), r.toFloat(), b.toFloat())
}

fun CellRect.toPx(outW: Float, outH: Float, gap: Float): PxRect {
    val eps = 0.001f
    val half = gap / 2f
    val l = x * outW + if (x < eps) gap else half
    val t = y * outH + if (y < eps) gap else half
    val r = (x + w) * outW - if (x + w > 1f - eps) gap else half
    val b = (y + h) * outH - if (y + h > 1f - eps) gap else half
    return PxRect(l, t, maxOf(r, l + 1f), maxOf(b, t + 1f))
}

object CollageTemplates {
    val all: List<CollageTemplate> = listOf(
        grid("2x1", 2, 1),
        grid("1x2", 1, 2),
        grid("2x2", 2, 2),
        rows("1+2", 1, 2),
        rows("2+1", 2, 1),
        grid("3x1", 3, 1),
        grid("1x3", 1, 3),
        grid("2x3", 2, 3),
        grid("3x3", 3, 3),
        grid("4x4", 4, 4),
        hero(),
        mosaic(),
        grid("film_strip", 1, 5),
        grid("contact_sheet", 4, 5),
        rows("1+3", 1, 3),
        rows("3+1", 3, 1),
        rows("1+2+1", 1, 2, 1),
        rows("2+3", 2, 3),
        rows("3+2", 3, 2),
        rows("1+4", 1, 4),
        cols("1|2", 1, 2),
        cols("2|1", 2, 1),
        cols("1|3", 1, 3),
        cols("3|1", 3, 1),
        cols("1|2|1", 1, 2, 1),
        grid("3x2", 3, 2),
        grid("2x4", 2, 4),
        grid("4x2", 4, 2)
    ) + CollageLayouts.extra

    val maxCells: Int by lazy { all.maxOf { it.cells.size } }

    fun suggest(count: Int): CollageTemplate {
        val n = count.coerceAtLeast(1)
        val fitting = all.filter { it.cells.size >= n }
        return fitting.minWithOrNull(compareBy({ it.cells.size }, { all.indexOf(it) }))
            ?: all.maxByOrNull { it.cells.size }!!
    }

    private fun weights(counts: IntArray): List<Float> {
        val mixed = counts.size > 1 && counts.any { it > 1 }
        return counts.map { if (mixed && it == 1) 1.6f else 1f }
    }

    private fun rows(id: String, vararg counts: Int): CollageTemplate {
        val w = weights(counts)
        val total = w.sum()
        val cells = mutableListOf<CellRect>()
        var y = 0f
        counts.forEachIndexed { r, n ->
            val h = w[r] / total
            for (c in 0 until n) cells += CellRect(c.toFloat() / n, y, 1f / n, h)
            y += h
        }
        return CollageTemplate(id, id, 1, 1, cells)
    }

    private fun cols(id: String, vararg counts: Int): CollageTemplate {
        val w = weights(counts)
        val total = w.sum()
        val cells = mutableListOf<CellRect>()
        var x = 0f
        counts.forEachIndexed { c, n ->
            val cw = w[c] / total
            for (r in 0 until n) cells += CellRect(x, r.toFloat() / n, cw, 1f / n)
            x += cw
        }
        return CollageTemplate(id, id, 1, 1, cells)
    }

    private fun hero(): CollageTemplate = CollageTemplate(
        "hero", "Hero", 3, 3, listOf(
            CellRect(0f, 0f, 1f, 2f / 3f),
            CellRect(0f, 2f / 3f, 1f / 3f, 1f / 3f),
            CellRect(1f / 3f, 2f / 3f, 1f / 3f, 1f / 3f),
            CellRect(2f / 3f, 2f / 3f, 1f / 3f, 1f / 3f)
        )
    )

    private fun mosaic(): CollageTemplate = CollageTemplate(
        "mosaic", "Mosaic", 3, 3, listOf(
            CellRect(0f, 0f, 2f / 3f, 2f / 3f),
            CellRect(2f / 3f, 0f, 1f / 3f, 1f / 3f),
            CellRect(2f / 3f, 1f / 3f, 1f / 3f, 1f / 3f),
            CellRect(0f, 2f / 3f, 1f / 3f, 1f / 3f),
            CellRect(1f / 3f, 2f / 3f, 1f / 3f, 1f / 3f),
            CellRect(2f / 3f, 2f / 3f, 1f / 3f, 1f / 3f)
        )
    )

    private fun grid(id: String, cols: Int, rows: Int): CollageTemplate {
        val cells = mutableListOf<CellRect>()
        val cw = 1f / cols
        val rh = 1f / rows
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                cells += CellRect(c * cw, r * rh, cw, rh)
            }
        }
        return CollageTemplate(id, id, cols, rows, cells)
    }
}

data class CollageConfig(
    val template: CollageTemplate = CollageTemplates.all.first(),
    val spacingPx: Int = COLLAGE_GAP_DEFAULT,
    val backgroundColor: Long = 0xFF000000,
    val outputWidth: Int = COLLAGE_DEFAULT_WIDTH,
    val cornerRadiusPx: Float = 0f,
    val timestamps: List<String> = emptyList(),
    val crops: List<CellCrop> = emptyList(),
    val blend: CollageBlend = CollageBlend.None,
    val blendStrength: Int = 0
)
