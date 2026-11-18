package com.frameout.engine.nudge

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.frameout.core.nudge.NudgePrefs
import kotlin.coroutines.cancellation.CancellationException

class NewVideoWatchWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val ctx = applicationContext
        if (!NudgePrefs.get(ctx).newVideoEnabled) return Result.success()
        NudgeScheduler.armWatcher(ctx, append = true)
        try {
            NudgeRunner.checkNewVideos(ctx)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
        return Result.success()
    }
}

class NudgeSweepWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val ctx = applicationContext
        try {
            NudgeRunner.checkNewVideos(ctx)
            NudgeRunner.maybeRemind(ctx)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
        return Result.success()
    }
}
