package com.frameout.core.export

object ContactSheetPreview {
    const val TINY_CELL = 330

    data class Summary(
        val plan: ContactSheetLayout.Plan,
        val count: Int,
        val perSheet: Int,
        val firstCount: Int,
        val pages: Int,
        val totalRows: Int,
        val height: Int
    ) {
        val width: Int get() = plan.sheetWidth
        val megapixels: Float get() = width.toLong() * height / 1_000_000f
        val tooMany: Boolean get() = plan.tooMany
        val tiny: Boolean get() = plan.cellW < TINY_CELL
        val empty: Boolean get() = count <= 0
        val hasRoom: Boolean get() = firstCount < perSheet
    }

    fun summarize(count: Int, aspect: Float, options: ContactSheetLayout.Options): Summary {
        val safeCount = count.coerceAtLeast(0)
        val plan = ContactSheetLayout.plan(safeCount.coerceAtLeast(1), aspect, options)
        val perSheet = plan.rowsPerSheet * plan.columns
        val first = minOf(safeCount, perSheet)
        val rows = (safeCount + plan.columns - 1) / plan.columns
        val pages = if (safeCount == 0) 0 else plan.sheets.size
        return Summary(plan, safeCount, perSheet, first, pages, rows, plan.sheetHeight(0))
    }
}
