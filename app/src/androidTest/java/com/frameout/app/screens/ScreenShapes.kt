package com.frameout.app.screens

class ScreenShape(val name: String, val widthDp: Int, val heightDp: Int) {
    fun rotated(): ScreenShape = ScreenShape("$name-landscape", heightDp, widthDp)

    override fun toString(): String = "$name ${widthDp}x$heightDp"
}

object ScreenShapes {
    private val portrait = listOf(
        ScreenShape("small-phone", 320, 568),
        ScreenShape("compact-phone", 360, 640),
        ScreenShape("phone", 411, 914),
        ScreenShape("large-phone", 448, 998),
        ScreenShape("fold-cover", 344, 882),
        ScreenShape("fold-inner", 673, 841),
        ScreenShape("fold-inner-wide", 768, 884),
        ScreenShape("split-half-phone", 411, 457),
        ScreenShape("tablet-8", 600, 960),
        ScreenShape("tablet-10", 800, 1280),
        ScreenShape("tablet-12", 1024, 1366),
        ScreenShape("tablet-14", 1138, 1752)
    )

    val all: List<ScreenShape> = portrait + portrait.map { it.rotated() }
}
