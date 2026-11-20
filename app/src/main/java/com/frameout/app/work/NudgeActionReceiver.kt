package com.frameout.app.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.frameout.app.FrameOutApp
import com.frameout.app.launchExtract
import com.frameout.app.trim.SourceProbe
import com.frameout.app.trim.estimateFrames
import com.frameout.core.nudge.NudgePolicy
import com.frameout.core.nudge.NudgePrefs
import com.frameout.core.prefs.AppPrefs
import com.frameout.core.util.LocaleHelper
import com.frameout.engine.nudge.NudgeIntents
import com.frameout.engine.nudge.NudgeNotifications
import com.frameout.engine.nudge.NudgeScheduler
import com.frameout.engine.timeline.TrimRange
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.cancellation.CancellationException

class NudgeActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            NudgeIntents.ACTION_STOP_REMINDERS -> {
                NudgePrefs.get(context).remindersEnabled = false
                NudgeScheduler.sync(context)
                NudgeNotifications.cancelReminder(context)
            }
            NudgeIntents.ACTION_EXTRACT_NEW -> extract(context.applicationContext, intent)
        }
    }

    private fun extract(context: Context, intent: Intent) {
        val uri = intent.getStringExtra(NudgeIntents.EXTRA_VIDEO_URI)?.let(Uri::parse) ?: return
        val id = intent.getLongExtra(NudgeIntents.EXTRA_VIDEO_ID, -1L)
        val duration = intent.getLongExtra(NudgeIntents.EXTRA_VIDEO_DURATION, 0L)
        val app = context as? FrameOutApp ?: return
        val pending = goAsync()
        app.appScope.launch {
            try {
                LocaleHelper.apply(app, AppPrefs.peekLanguage(app))
                when (withTimeoutOrNull(BUDGET_MS) { start(app, uri, duration) }) {
                    Outcome.Started -> {
                        val prefs = NudgePrefs.get(app)
                        prefs.removePending(id)
                        if (NudgeNotifications.showPending(app) == 0) prefs.clearWaitingVideo()
                    }
                    Outcome.TooLong, null -> NudgeNotifications.showNeedsReview(app, uri, id)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                NudgeNotifications.showStartFailed(app, uri, id)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun start(app: FrameOutApp, uri: Uri, durationMs: Long): Outcome {
        val range = TrimRange.full(durationMs)
        val probe = SourceProbe.load(app, uri)
        val estimate = estimateFrames(durationMs, range, app.container.prefs.extractMode.value, probe)
        if (estimate > NudgePolicy.ONE_TAP_FRAME_CAP) return Outcome.TooLong
        launchExtract(app, app.container, uri, range, autoSave = false, awaitCover = true)
        return Outcome.Started
    }

    private enum class Outcome { Started, TooLong }

    private companion object {
        const val BUDGET_MS = 8_000L
    }
}
