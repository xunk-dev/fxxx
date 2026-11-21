package com.frameout.core.collage

enum class BlendFamily { None, Edge, Cell, Fade }

enum class CollageBlend(val id: String, val family: BlendFamily, val defaultStrength: Int) {
    None("none", BlendFamily.None, 0),
    Wave("wave", BlendFamily.Edge, 55),
    Zigzag("zigzag", BlendFamily.Edge, 55),
    Slant("slant", BlendFamily.Edge, 45),
    Arc("arc", BlendFamily.Edge, 55),
    Steps("steps", BlendFamily.Edge, 50),
    Ripple("ripple", BlendFamily.Edge, 60),
    Circle("circle", BlendFamily.Cell, 100),
    Squircle("squircle", BlendFamily.Cell, 100),
    Diamond("diamond", BlendFamily.Cell, 100),
    Hexagon("hexagon", BlendFamily.Cell, 100),
    Octagon("octagon", BlendFamily.Cell, 100),
    Ticket("ticket", BlendFamily.Cell, 100),
    Feather("feather", BlendFamily.Fade, 50),
    Mist("mist", BlendFamily.Fade, 60),
    Flow("flow", BlendFamily.Edge, 55),
    Scallop("scallop", BlendFamily.Edge, 55),
    Torn("torn", BlendFamily.Edge, 60),
    Heart("heart", BlendFamily.Cell, 100),
    Arch("arch", BlendFamily.Cell, 100),
    Leaf("leaf", BlendFamily.Cell, 100),
    Blob("blob", BlendFamily.Cell, 100);

    companion object {
        val all: List<CollageBlend> = entries.toList()

        fun fromId(id: String?): CollageBlend = entries.firstOrNull { it.id == id } ?: None
    }
}

const val BLEND_STRENGTH_MAX = 100

class Ramps(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val any: Boolean get() = left > 0f || top > 0f || right > 0f || bottom > 0f

    companion object {
        val None = Ramps(0f, 0f, 0f, 0f)
    }
}

class CellBlend(
    val bounds: PxRect,
    val polygon: FloatArray?,
    val ramps: Ramps = Ramps.None,
    val padX: Float = 0f,
    val padY: Float = 0f
)
