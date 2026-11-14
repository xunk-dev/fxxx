package com.frameout.core.pro

import com.frameout.core.edit.ShapeKind
import com.frameout.core.storage.ImageFormat

object ProCatalog {
    val freeFilters: Set<String> = setOf(
        "", "mono", "noir", "silver", "sepia", "vintage", "fade", "warm", "cool", "vivid", "dramatic", "cinema", "sunset", "chrome"
    )

    val adFilters: Set<String> = setOf("kodak", "polaroid", "matte", "lomo")

    val freeShapes: Set<ShapeKind> = setOf(
        ShapeKind.Arrow, ShapeKind.Line, ShapeKind.Rect, ShapeKind.RoundRect, ShapeKind.Ellipse, ShapeKind.Triangle
    )
    val adShapes: Set<ShapeKind> = setOf(
        ShapeKind.Highlighter, ShapeKind.DoubleArrow, ShapeKind.Diamond, ShapeKind.Check, ShapeKind.Cross, ShapeKind.Bracket
    )

    val freeCollages: Set<String> = setOf("2x1", "1x2", "2x2")
    const val DYNAMIC_ACCENT = "dynamic"
    val accentKeys: Set<String> = setOf("indigo", "ocean", "black", "emerald", "sunset", "crimson", "lime", "rose", "gold", "violet", "mocha")
    val freeAccents: Set<String> = setOf("black")
    val proOnlyAccents: Set<String> = setOf("rose", "gold", "violet", "mocha", DYNAMIC_ACCENT)
    val freeFormats: Set<ImageFormat> = setOf(ImageFormat.Jpg, ImageFormat.Png, ImageFormat.WebpLossy)
    const val TRIALS_PER_FEATURE = 0
    const val GRACE_DAYS = 14

    fun filterTier(id: String): ProTier = when (id) {
        in freeFilters -> ProTier.Free
        in adFilters -> ProTier.Ad
        else -> ProTier.Pro
    }

    fun shapeTier(kind: ShapeKind): ProTier = when (kind) {
        in freeShapes -> ProTier.Free
        in adShapes -> ProTier.Ad
        else -> ProTier.Pro
    }

    fun textTier(): ProTier = ProTier.Free

    fun coverTier(style: com.frameout.core.edit.CoverStyle): ProTier = when (style) {
        com.frameout.core.edit.CoverStyle.Solid -> ProTier.Free
        com.frameout.core.edit.CoverStyle.Blur -> ProTier.Ad
        com.frameout.core.edit.CoverStyle.Pixelate -> ProTier.Pro
    }

    fun orderedFilters(ids: List<String>): List<String> = ids.sortedBy { filterTier(it).ordinal }
    fun orderedShapes(): List<ShapeKind> = ShapeKind.entries.sortedBy { shapeTier(it).ordinal }

    fun filterIsFree(id: String): Boolean = filterTier(id) == ProTier.Free
    fun collageIsFree(id: String): Boolean = id in freeCollages
    fun accentIsFree(key: String, granted: String? = null): Boolean = key in freeAccents || (granted != null && key == granted)
    fun welcomePickable(key: String): Boolean = key in accentKeys
    fun accentIsProOnly(key: String): Boolean = key in proOnlyAccents
    fun formatIsFree(format: ImageFormat): Boolean = format in freeFormats
}
