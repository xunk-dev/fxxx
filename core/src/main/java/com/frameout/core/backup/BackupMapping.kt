package com.frameout.core.backup

import com.frameout.core.db.FrameEditEntity
import com.frameout.core.db.FrameEditHistoryEntity
import com.frameout.core.db.FrameEntity
import com.frameout.core.db.ProjectEntity
import com.frameout.core.edit.toEdit
import com.frameout.core.edit.toEntity as toFrameEditEntity
import com.frameout.core.pro.ProEditGuard

internal fun ProjectEntity.toBackup() = BackupProject(name, sourceName, sourceUri, durationMs, extractMode, saveFormat, jpegQuality, startMs, endMs, saveSubfolder, createdAt)

internal fun FrameEditEntity.toBackup() = BackupEdit(rotation, flipH, flipV, cropL, cropT, cropR, cropB, brightness, contrast, saturation, isDeleted, marks, filter, filterStrength)

internal fun FrameEntity.toBackup(edit: FrameEditEntity?) = BackupFrame(index, ptsUs, isKeyframe, isFavorite, isDeleted, edit?.toBackup())

private fun BackupEdit.sanitizedEntity(frameId: Long): FrameEditEntity {
    val raw = FrameEditEntity(frameId, rotation, flipH, flipV, cropL, cropT, cropR, cropB, brightness, contrast, saturation, 0, isDeleted, marks, filter, filterStrength)
    return ProEditGuard.sanitize(raw.toEdit()).toFrameEditEntity()
}

internal fun BackupEdit.toEntity(frameId: Long): FrameEditEntity = sanitizedEntity(frameId)

internal fun BackupEdit.toHistory(frameId: Long): FrameEditHistoryEntity {
    val e = sanitizedEntity(frameId)
    return FrameEditHistoryEntity(
        frameId = frameId, revision = 0, rotation = e.rotation, flipH = e.flipH, flipV = e.flipV,
        cropL = e.cropL, cropT = e.cropT, cropR = e.cropR, cropB = e.cropB, brightness = e.brightness,
        contrast = e.contrast, saturation = e.saturation, isDeleted = e.isDeleted, marks = e.marks,
        filter = e.filter, filterStrength = e.filterStrength
    )
}
