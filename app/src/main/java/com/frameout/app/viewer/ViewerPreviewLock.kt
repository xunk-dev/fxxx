package com.frameout.app.viewer

import com.frameout.core.edit.FrameEdit
import com.frameout.core.pro.ProAccess
import com.frameout.core.pro.ProCatalog
import com.frameout.core.pro.ProEditGuard
import com.frameout.core.pro.ProFeature

internal fun ViewerState.previewLocked(): Boolean {
    if (tool == EditTool.None) return false
    if (ProEditGuard.firstLock(FrameEdit(0L, filter = draft.filter, marks = draft.marks)) != null) return true
    if (tool == EditTool.Draw && draft.drawMode == DrawMode.Shapes &&
        !ProAccess.allows(ProFeature.Shapes, ProCatalog.shapeTier(draft.shapeKind))
    ) return true
    if (tool == EditTool.Cover && !ProAccess.allows(ProFeature.EditTools, ProCatalog.coverTier(draft.style))) return true
    return false
}
