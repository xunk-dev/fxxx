package com.frameout.app.frames

import com.frameout.core.db.FrameDao
import com.frameout.core.db.FrameTagDao
import com.frameout.core.quality.FrameImportanceAnalyzer
import com.frameout.core.quality.DuplicateFinder
import com.frameout.core.quality.FrameSharpnessAnalyzer
import com.frameout.core.quality.PeopleScan
import com.frameout.core.util.QualityStrings
import com.frameout.core.util.logFailure
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private suspend fun <T> analysed(tag: String, fallback: T, block: suspend () -> T): T = try {
    withContext(Dispatchers.Default) { block() }
} catch (e: CancellationException) {
    throw e
} catch (e: Throwable) {
    logFailure(tag, e)
    fallback
}

internal suspend fun sharpestSelection(dao: FrameDao, projectId: Long): Pair<Set<Long>, String> {
    val ids = analysed("Sharpest", emptyList()) { FrameSharpnessAnalyzer.bestIds(dao, projectId) }
    val text = if (ids.isEmpty()) QualityStrings.noThumbnails() else QualityStrings.sharpestPicked(ids.size)
    return ids.toSet() to text
}

internal suspend fun duplicateSelection(dao: FrameDao, projectId: Long): Pair<Set<Long>, String> {
    val ids = analysed("Duplicates", emptyList()) { DuplicateFinder.duplicateIds(dao, projectId) }
    val text = if (ids.isEmpty()) QualityStrings.noDuplicates() else QualityStrings.duplicatesPicked(ids.size)
    return ids.toSet() to text
}

internal suspend fun noPeopleSelection(dao: FrameDao, projectId: Long): Pair<Set<Long>, String> {
    val ids = analysed("NoPeople", emptyList()) { PeopleScan.withoutPeople(dao, projectId) }
    val text = if (ids.isEmpty()) QualityStrings.noPeopleFound() else QualityStrings.noPeoplePicked(ids.size)
    return ids.toSet() to text
}

internal suspend fun importantSelection(frames: FrameDao, tags: FrameTagDao, projectId: Long): Pair<Set<Long>, String> {
    val ids = analysed("Important", emptyList()) { FrameImportanceAnalyzer.bestIds(frames, tags, projectId) }
    val text = if (ids.isEmpty()) QualityStrings.noThumbnails() else QualityStrings.importantPicked(ids.size)
    return ids.toSet() to text
}
