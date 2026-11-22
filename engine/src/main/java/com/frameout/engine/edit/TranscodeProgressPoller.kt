package com.frameout.engine.edit

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.frameout.engine.work.TranscodeNotice

@androidx.media3.common.util.UnstableApi
internal class TranscodeProgressPoller(
    private val context: Context,
    private val plan: ReassemblePlan,
    private val transformer: () -> Transformer?
) {
    private val main = Handler(Looper.getMainLooper())
    private val startedAt = SystemClock.elapsedRealtime()
    private val holder = ProgressHolder()
    private val tick = object : Runnable {
        override fun run() {
            publish()
            main.postDelayed(this, POLL_MS)
        }
    }

    fun start() {
        main.post(tick)
    }

    fun stop() {
        main.removeCallbacks(tick)
    }

    private fun publish() {
        val current = transformer() ?: return
        val fraction = if (current.getProgress(holder) == Transformer.PROGRESS_STATE_AVAILABLE) holder.progress / PERCENT else -1f
        TranscodeNotice.post(context, TranscodeNotice.Progress(plan.projectId, plan.frameCount, fraction, SystemClock.elapsedRealtime() - startedAt))
    }

    private companion object {
        const val POLL_MS = 700L
        const val PERCENT = 100f
    }
}
