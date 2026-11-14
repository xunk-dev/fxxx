package com.frameout.core.quality

import com.frameout.core.db.FrameDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap

object FrameSharpnessAnalyzer {
    private const val ID_CHUNK = 500

    suspend fun scoreProject(dao: FrameDao, projectId: Long, onProgress: (Int, Int) -> Unit = { _, _ -> }): List<ScoredFrame> {
        val blocks = BlockLedger.Lookup()
        val parts = FrameScan.mapThumbnailsParallel(dao, projectId, dao.countActive(projectId), scanWorkers(), onProgress) { frame, bitmap ->
            val luma = Sharpness.lumaOf(bitmap)
            val focus = FocusScore.detail(luma, bitmap.width, bitmap.height)
            val profile = MotionEstimate.profile(luma, bitmap.width, bitmap.height)
            val factor = synchronized(blocks) { blocks.factor(frame.thumbnailPath, frame.index) }
            val scored = ScoredFrame(
                frame.id, frame.index, focus.score * factor, ptsUs = frame.ptsUs, isKeyframe = frame.isKeyframe,
                sig = FrameSignature.of(luma, bitmap.width, bitmap.height),
                noise = FrameSignature.cellNoise(focus.noise, bitmap.width, bitmap.height)
            )
            scored to profile
        }
        val speeds = MotionEstimate.speeds(parts.map { it.second })
        return parts.mapIndexed { i, part -> part.first.copy(speed = speeds[i]) }
    }

    private fun scanWorkers(): Int = Runtime.getRuntime().availableProcessors().coerceIn(2, 4)

    suspend fun bestIds(
        dao: FrameDao,
        projectId: Long,
        onProgress: (Int, Int) -> Unit = { _, _ -> },
        onQuick: suspend (List<Long>) -> Unit = {}
    ): List<Long> {
        val scored = scoreProject(dao, projectId, onProgress)
        if (scored.isEmpty()) return emptyList()
        val quick = SharpestPicker.pick(scored)
        onQuick(quick)
        val candidateIds = SharpestPicker.candidates(scored)
        val faces = faceScores(dao, candidateIds)
        if (faces.isEmpty()) return quick
        val pool = scored.map { f ->
            when {
                f.id !in candidateIds -> f.copy(score = -1.0)
                else -> faces[f.id]?.let { f.copy(faceScore = it) } ?: f
            }
        }
        return SharpestPicker.pick(pool)
    }

    private suspend fun faceScores(dao: FrameDao, ids: Set<Long>): Map<Long, Float> {
        val out = ConcurrentHashMap<Long, Float>()
        withTimeoutOrNull(FACE_BUDGET_MS) {
            withContext(Dispatchers.IO) {
                val permits = Semaphore(FACE_PARALLEL)
                ids.toList().chunked(ID_CHUNK).forEach { chunk ->
                    val frames = dao.getByIds(chunk)
                    coroutineScope {
                        frames.map { frame ->
                            async {
                                permits.withPermit {
                                    ensureActive()
                                    val assessment = FaceScanner.assessFrameBounded(frame)
                                    if (assessment != null && assessment.level != FaceLevel.None) out[frame.id] = assessment.score
                                }
                            }
                        }.awaitAll()
                    }
                }
            }
        }
        return HashMap(out)
    }

    private const val FACE_BUDGET_MS = 25_000L
    private const val FACE_PARALLEL = 2
}
