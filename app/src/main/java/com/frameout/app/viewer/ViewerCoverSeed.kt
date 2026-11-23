package com.frameout.app.viewer

import com.frameout.core.edit.BrushCoverMark
import com.frameout.core.edit.CoverMark
import com.frameout.core.edit.EditMark
import kotlin.math.abs

private const val TWIN_SHIFT = 0.01f

internal fun ViewerToolEditor.seedCovers(seeds: List<EditMark>) {
    val space = state.value.markSpace() ?: return
    var chosen = -1
    update { d ->
        var marks = d.marks
        seeds.forEach { seed ->
            val twin = marks.indexOfFirst { m -> sameSeed(space.toOutput(m), seed) }
            if (twin >= 0) {
                chosen = twin
            } else {
                marks = marks + space.toSource(seed)
                chosen = marks.size - 1
            }
        }
        d.copy(marks = marks)
    }
    if (chosen >= 0) covers.select(chosen)
}

private fun sameSeed(existing: EditMark, seed: EditMark): Boolean = when {
    existing is CoverMark && seed is CoverMark -> DetectionBlur.same(existing, seed)
    existing is BrushCoverMark && seed is BrushCoverMark && existing.filled && seed.filled -> {
        val a = existing.points.firstOrNull()
        val b = seed.points.firstOrNull()
        a != null && b != null && abs(a.x - b.x) < TWIN_SHIFT && abs(a.y - b.y) < TWIN_SHIFT
    }
    else -> false
}
