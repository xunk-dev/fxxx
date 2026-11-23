package com.frameout.core.collage

internal object CollageLayouts {
    private const val PORTRAIT = 0.8f
    private const val SQUARE = 1f
    private const val LANDSCAPE = 4f / 3f
    private const val THIRD = 1f / 3f
    private const val GOLD = 0.618f
    private const val GOLD_REST = 0.382f

    val extra: List<CollageTemplate> by lazy {
        listOf(
            t("p2a", LANDSCAPE, row(0f, 1f, GOLD, GOLD_REST)),
            t("p2b", PORTRAIT, col(0f, 1f, GOLD, GOLD_REST)),
            t("p3a", LANDSCAPE, row(0f, 1f, 0.27f, 0.46f, 0.27f)),
            t("p3b", PORTRAIT, row(0f, 0.62f, 1f) + row(0.62f, 0.38f, GOLD_REST, GOLD)),
            t("p3c", SQUARE, col(0f, 0.5f, 1f) + col(0.5f, 0.5f, 0.38f, 0.62f)),
            t("p4a", SQUARE, listOf(r(0f, 0f, GOLD, GOLD), r(GOLD, 0f, GOLD_REST, 0.309f), r(GOLD, 0.309f, GOLD_REST, 0.309f)) + row(GOLD, GOLD_REST, 1f)),
            t("p4b", LANDSCAPE, col(0f, 0.72f, 1f) + col(0.72f, 0.28f, THIRD, THIRD, THIRD)),
            t("p4c", SQUARE, listOf(r(0f, 0f, 0.5f, 0.4f), r(0.5f, 0f, 0.5f, 0.6f), r(0f, 0.4f, 0.5f, 0.6f), r(0.5f, 0.6f, 0.5f, 0.4f))),
            t("p5a", SQUARE, listOf(r(0f, 0f, 0.6f, 0.4f), r(0.6f, 0f, 0.4f, 0.6f), r(0.4f, 0.6f, 0.6f, 0.4f), r(0f, 0.4f, 0.4f, 0.6f), r(0.4f, 0.4f, 0.2f, 0.2f))),
            t("p5b", LANDSCAPE, col(0f, 0.5f, 1f) + col(0.5f, 0.25f, 0.5f, 0.5f) + col(0.75f, 0.25f, 0.5f, 0.5f)),
            t("p5c", SQUARE, row(0f, 0.5f, GOLD, GOLD_REST) + row(0.5f, 0.5f, GOLD_REST, 0.309f, 0.309f)),
            t("p6a", SQUARE, row(0f, 0.6f, 0.34f, 0.33f, 0.33f) + row(0.6f, 0.4f, 0.34f, 0.33f, 0.33f)),
            t("p6b", PORTRAIT, col(0f, 0.34f, 0.45f, 0.55f) + col(0.34f, 0.33f, 0.6f, 0.4f) + col(0.67f, 0.33f, 0.38f, 0.62f)),
            t("p7a", PORTRAIT, row(0f, 0.5f, 1f) + row(0.5f, 0.25f, THIRD, THIRD, THIRD) + row(0.75f, 0.25f, THIRD, THIRD, THIRD)),
            t("p7b", SQUARE, col(0f, 0.28f, THIRD, THIRD, THIRD) + col(0.28f, 0.44f, 1f) + col(0.72f, 0.28f, THIRD, THIRD, THIRD)),
            t("p8a", PORTRAIT, row(0f, 0.3f, 0.5f, 0.5f) + row(0.3f, 0.4f, 0.25f, 0.25f, 0.25f, 0.25f) + row(0.7f, 0.3f, 0.5f, 0.5f)),
            t("p9a", SQUARE, listOf(r(0f, 0f, 0.5f, 0.5f), r(0.5f, 0f, 0.25f, 0.25f), r(0.75f, 0f, 0.25f, 0.25f), r(0.5f, 0.25f, 0.25f, 0.25f), r(0.75f, 0.25f, 0.25f, 0.25f)) + row(0.5f, 0.5f, 0.25f, 0.25f, 0.25f, 0.25f)),
            t("p10a", PORTRAIT, row(0f, 0.3f, THIRD, THIRD, THIRD) + row(0.3f, 0.4f, 0.25f, 0.25f, 0.25f, 0.25f) + row(0.7f, 0.3f, THIRD, THIRD, THIRD)),
            gridOf("4x3", 4, 3),
            gridOf("3x4", 3, 4)
        )
    }

    private fun r(x: Float, y: Float, w: Float, h: Float) = CellRect(x, y, w, h)

    private fun row(y: Float, h: Float, vararg widths: Float): List<CellRect> {
        var x = 0f
        return widths.map { w -> CellRect(x, y, w, h).also { x += w } }
    }

    private fun col(x: Float, w: Float, vararg heights: Float): List<CellRect> {
        var y = 0f
        return heights.map { h -> CellRect(x, y, w, h).also { y += h } }
    }

    private fun t(id: String, aspect: Float, cells: List<CellRect>) = CollageTemplate(id, id, 1, 1, cells, aspect)

    private fun gridOf(id: String, cols: Int, rows: Int): CollageTemplate {
        val cells = ArrayList<CellRect>()
        for (rr in 0 until rows) for (c in 0 until cols) cells.add(CellRect(c.toFloat() / cols, rr.toFloat() / rows, 1f / cols, 1f / rows))
        return CollageTemplate(id, id, cols, rows, cells)
    }
}
