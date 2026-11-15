package com.frameout.app.home

internal class HomeFit(val pro: Float, val tall: Float, val short: Float)

private const val TITLE_LINE = 24f
private const val MIN_PRO = 140f
private const val MIN_TALL = 144f
private const val MIN_SHORT = 66f

internal fun fitHomeHeights(pro: Float, tall: Float, short: Float, available: Float, adTotal: Float): HomeFit {
    if (adTotal <= 0f) return HomeFit(pro, tall, short)
    val gaps = HomeMetrics.SectionGap.value + TITLE_LINE + 2f * HomeMetrics.LabelGap.value + 2f * HomeMetrics.TileGap.value
    var over = pro + 2f * tall + short + gaps + adTotal - available
    if (over <= 0f) return HomeFit(pro, tall, short)
    val s = minOf(over, (short - MIN_SHORT).coerceAtLeast(0f))
    over -= s
    val t = minOf(over / 2f, (tall - MIN_TALL).coerceAtLeast(0f))
    over -= 2f * t
    val p = minOf(over, (pro - MIN_PRO).coerceAtLeast(0f))
    return HomeFit(pro - p, tall - t, short - s)
}
