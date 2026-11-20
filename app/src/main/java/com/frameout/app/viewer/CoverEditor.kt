package com.frameout.app.viewer

import com.frameout.core.edit.BrushCoverMark
import com.frameout.core.edit.CoverMark
import com.frameout.core.edit.CoverStyle
import com.frameout.core.edit.EditMark
import com.frameout.core.edit.MarkPoint
import com.frameout.core.edit.MarkSpace
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.max
import kotlin.math.min

private const val DUPLICATE_OFFSET = 0.04f
private const val MIN_SIDE = 0.01f

internal fun EditMark.isCover(): Boolean = this is CoverMark || this is BrushCoverMark

internal fun EditMark.withStyle(style: CoverStyle): EditMark = when (this) {
    is CoverMark -> copy(style = style)
    is BrushCoverMark -> copy(style = style)
    else -> this
}

internal fun EditMark.withStrength(strength: Float): EditMark = when (this) {
    is CoverMark -> copy(strength = strength)
    is BrushCoverMark -> copy(strength = strength)
    else -> this
}

internal fun coverBounds(mark: EditMark): FloatArray? = when (mark) {
    is CoverMark -> floatArrayOf(mark.left, mark.top, mark.right, mark.bottom)
    is BrushCoverMark -> mark.points.takeIf { it.isNotEmpty() }?.let { pts ->
        floatArrayOf(pts.minOf { it.x }, pts.minOf { it.y }, pts.maxOf { it.x }, pts.maxOf { it.y })
    }
    else -> null
}

internal fun shiftedCover(mark: EditMark, dx: Float, dy: Float): EditMark {
    val b = coverBounds(mark) ?: return mark
    val sx = dx.coerceIn(-b[0], 1f - b[2])
    val sy = dy.coerceIn(-b[1], 1f - b[3])
    return when (mark) {
        is CoverMark -> mark.copy(left = mark.left + sx, top = mark.top + sy, right = mark.right + sx, bottom = mark.bottom + sy)
        is BrushCoverMark -> mark.copy(points = mark.points.map { MarkPoint(it.x + sx, it.y + sy) })
        else -> mark
    }
}

internal class CoverEditor(
    private val state: MutableStateFlow<ViewerState>,
    private val rerender: () -> Unit
) {
    private fun update(op: (EditDraft) -> EditDraft) = state.update { if (it.toolBusy) it else it.copy(draft = op(it.draft)) }

    private fun space(): MarkSpace? = state.value.markSpace()

    private fun shown(raw: EditMark): EditMark = space()?.toOutput(raw) ?: raw

    private fun stored(mark: EditMark): EditMark = space()?.toSource(mark) ?: mark

    fun select(index: Int) = update { d ->
        val raw = d.marks.getOrNull(index)?.takeIf { it.isCover() } ?: return@update if (d.selectedCover == -1) d else d.copy(selectedCover = -1)
        when (val m = shown(raw)) {
            is CoverMark -> d.copy(selectedCover = index, style = m.style, strength = m.strength)
            is BrushCoverMark -> d.copy(selectedCover = index, style = m.style, strength = m.strength)
            else -> d
        }
    }

    fun restyle(draft: EditDraft, op: (EditMark) -> EditMark): EditDraft {
        val i = draft.selectedCover
        val raw = draft.marks.getOrNull(i)?.takeIf { it.isCover() } ?: return draft
        return draft.copy(marks = draft.marks.replaceAt(i, op(raw)))
    }

    fun deleteSelected() {
        if (state.value.draft.coverSelection == null) return
        update { d -> d.copy(marks = d.marks.filterIndexed { n, _ -> n != d.selectedCover }, selectedCover = -1, liveCover = -1) }
        rerender()
    }

    fun duplicateSelected() {
        if (state.value.draft.coverSelection == null) return
        update { d ->
            val raw = d.coverSelection ?: return@update d
            val m = shown(raw)
            val b = coverBounds(m) ?: return@update d
            val dx = if (1f - b[2] >= DUPLICATE_OFFSET) DUPLICATE_OFFSET else -min(DUPLICATE_OFFSET, b[0])
            val dy = if (1f - b[3] >= DUPLICATE_OFFSET) DUPLICATE_OFFSET else -min(DUPLICATE_OFFSET, b[1])
            d.copy(marks = d.marks + stored(shiftedCover(m, dx, dy)), selectedCover = d.marks.size, liveCover = -1)
        }
        rerender()
    }

    fun begin(index: Int) {
        update { it.copy(liveCover = index) }
        rerender()
    }

    fun end() {
        if (state.value.draft.liveCover < 0) return
        update { it.copy(liveCover = -1) }
        rerender()
    }

    fun moveBy(index: Int, dx: Float, dy: Float) = update { d ->
        val raw = d.marks.getOrNull(index)?.takeIf { it.isCover() } ?: return@update d
        d.copy(marks = d.marks.replaceAt(index, stored(shiftedCover(shown(raw), dx, dy))))
    }

    fun resize(index: Int, anchor: MarkPoint, to: MarkPoint) = update { d ->
        val raw = d.marks.getOrNull(index) as? CoverMark ?: return@update d
        val m = shown(raw) as? CoverMark ?: return@update d
        val x = to.x.coerceIn(0f, 1f)
        val y = to.y.coerceIn(0f, 1f)
        val next = m.copy(left = min(anchor.x, x), top = min(anchor.y, y), right = max(anchor.x, x), bottom = max(anchor.y, y))
        if (next.right - next.left < MIN_SIDE || next.bottom - next.top < MIN_SIDE) d
        else d.copy(marks = d.marks.replaceAt(index, stored(next)))
    }
}
