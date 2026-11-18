package com.frameout.app.viewer

import com.frameout.core.edit.EditMark
import kotlinx.coroutines.flow.update

data class EditSnap(
    val brightness: Float, val contrast: Float, val saturation: Float,
    val filter: String, val filterStrength: Float,
    val rotation: Int, val flipH: Boolean, val flipV: Boolean,
    val marks: List<EditMark>,
    val cropL: Float, val cropT: Float, val cropR: Float, val cropB: Float
)

data class RedoEntry(val before: EditSnap, val after: EditSnap)

internal fun ViewerState.snap() = EditSnap(
    draft.brightness, draft.contrast, draft.saturation, draft.filter, draft.filterStrength,
    draft.rotation, draft.flipH, draft.flipV, draft.marks, cropL, cropT, cropR, cropB
)

private fun ViewerState.restored(s: EditSnap, redo: List<RedoEntry>) = copy(
    cropL = s.cropL, cropT = s.cropT, cropR = s.cropR, cropB = s.cropB,
    draft = draft.copy(
        brightness = s.brightness, contrast = s.contrast, saturation = s.saturation,
        filter = s.filter, filterStrength = s.filterStrength,
        rotation = s.rotation, flipH = s.flipH, flipV = s.flipV, marks = s.marks,
        selected = -1, selectedShape = -1, selectedCover = -1, liveCover = -1, redo = redo
    )
)

internal val ViewerState.canRedo: Boolean
    get() = !toolBusy && draft.redo.lastOrNull()?.after == snap()

internal fun ViewerToolEditor.recordingRedo(op: () -> Unit) {
    val before = state.value.snap()
    op()
    state.update { s ->
        val after = s.snap()
        if (after == before) return@update s
        val kept = if (s.draft.redo.lastOrNull()?.after == before) s.draft.redo else emptyList()
        s.copy(draft = s.draft.copy(redo = kept + RedoEntry(before, after)))
    }
}

internal fun ViewerToolEditor.redo() {
    val s = state.value
    if (!s.canRedo) return
    val entry = s.draft.redo.last()
    state.update { it.restored(entry.before, it.draft.redo.dropLast(1)) }
    rerender()
}
