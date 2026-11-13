package com.frameout.app.viewer

import com.frameout.core.edit.CoverMark

internal fun ViewerToolEditor.seedCovers(seeds: List<CoverMark>) {
    val space = state.value.markSpace() ?: return
    var chosen = -1
    update { d ->
        var marks = d.marks
        seeds.forEach { seed ->
            val twin = marks.indexOfFirst { m -> m is CoverMark && DetectionBlur.same(space.toOutput(m) as CoverMark, seed) }
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
