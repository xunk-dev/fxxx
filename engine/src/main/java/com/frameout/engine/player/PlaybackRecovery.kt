package com.frameout.engine.player

import android.net.Uri
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.SeekParameters
import com.frameout.engine.repair.DropRanges
import com.frameout.engine.repair.RecoveryPlan
import com.frameout.engine.repair.RecoveryStep
import com.frameout.engine.repair.RepairOutcome
import com.frameout.engine.repair.VideoRepair
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class PlaybackRecovery {
    var attempt = 0
    var lastTargetMs = -1L
    var salvaged = false
    var incomplete = false
    var everReady = false
    var restoreSeek = false
    var busy = false
    var pendingSeekMs = -1L
    var resumePlay = false
    var announce: PreviewNotice? = null
    var bad = DropRanges()

    fun reset(keepSalvaged: Boolean) {
        attempt = 0
        lastTargetMs = -1L
        incomplete = false
        everReady = false
        restoreSeek = false
        busy = false
        pendingSeekMs = -1L
        announce = null
        bad = DropRanges()
        if (!keepSalvaged) salvaged = false
    }
}

internal fun PlayerController.tryRecover(error: PlaybackException): Boolean {
    if (released || animated != null || recovery.busy || !RecoveryPlan.isRecoverable(error.errorCode)) return false
    if (!recovery.everReady && !recovery.salvaged) return startSalvage(0L)
    val at = player.currentPosition.coerceAtLeast(0L)
    if (RecoveryPlan.resetsAttempts(at, recovery.lastTargetMs)) recovery.attempt = 0
    val duration = player.duration.takeIf { it > 0L } ?: _state.value.durationMs
    return when (val step = RecoveryPlan.next(recovery.attempt, at, duration, recovery.salvaged)) {
        is RecoveryStep.SkipTo -> skipDamaged(at, step.positionMs)
        RecoveryStep.Salvage -> startSalvage(at)
        RecoveryStep.GiveUp -> false
    }
}

private fun PlayerController.skipDamaged(at: Long, target: Long): Boolean {
    recovery.attempt++
    recovery.lastTargetMs = target
    recovery.bad.add(at * US_PER_MS, target * US_PER_MS)
    recovery.restoreSeek = true
    val resume = player.playWhenReady
    player.setSeekParameters(SeekParameters.NEXT_SYNC)
    player.seekTo(target)
    player.prepare()
    player.playWhenReady = resume
    showTransient(PreviewNotice.SkippedDamage, TRANSIENT_MS)
    return true
}

private fun PlayerController.startSalvage(at: Long): Boolean {
    val uri = mediaUri ?: return false
    recovery.busy = true
    recovery.salvaged = true
    val resume = player.playWhenReady
    val drops = recovery.bad.copy()
    _state.value = _state.value.copy(ready = false, isPlaying = false, notice = PreviewNotice.Repairing)
    scope.launch {
        val outcome = VideoRepair.salvage(context.applicationContext, uri, drops)
        withContext(Dispatchers.Main) { finishSalvage(uri, outcome, at, resume, drops) }
    }
    return true
}

private fun PlayerController.finishSalvage(uri: Uri, outcome: RepairOutcome, at: Long, resume: Boolean, drops: DropRanges) {
    recovery.busy = false
    if (released || mediaUri != uri) return
    if (outcome !is RepairOutcome.Repaired) {
        _state.value = _state.value.copy(notice = if (recovery.incomplete) PreviewNotice.Incomplete else PreviewNotice.Unavailable)
        fallbackToFfmpeg()
        return
    }
    val position = (at - drops.shiftAt(at * US_PER_MS) / US_PER_MS).coerceAtLeast(0L)
    setUri(outcome.uri)
    recovery.pendingSeekMs = position
    recovery.resumePlay = resume
    recovery.announce = PreviewNotice.Repaired
    onSourceRepaired?.invoke(outcome.uri)
}

internal fun PlayerController.applyPrepared(outcome: RepairOutcome, source: Uri) {
    when {
        outcome is RepairOutcome.Repaired -> {
            mediaUri = source
            recovery.announce = PreviewNotice.Repaired
            onSourceRepaired?.invoke(source)
        }
        outcome is RepairOutcome.Unrecoverable && outcome.reason == RepairOutcome.Reason.MissingIndex -> recovery.incomplete = true
    }
}

internal fun PlayerController.resumeAfterRecovery() {
    val seek = recovery.pendingSeekMs
    if (seek >= 0L) {
        player.seekTo(seek)
        player.playWhenReady = recovery.resumePlay
        recovery.pendingSeekMs = -1L
    }
    recovery.announce?.let { showTransient(it, TRANSIENT_MS) }
    recovery.announce = null
}

internal fun PlayerController.onReadyAfterRecovery() {
    recovery.everReady = true
    if (recovery.restoreSeek) {
        recovery.restoreSeek = false
        player.setSeekParameters(SeekParameters.EXACT)
    }
}

internal fun PlayerController.showTransient(notice: PreviewNotice, ms: Long) {
    _state.value = _state.value.copy(notice = notice)
    main.postDelayed({
        if (_state.value.notice == notice) _state.value = _state.value.copy(notice = PreviewNotice.None)
    }, ms)
}

private const val US_PER_MS = 1000L
private const val TRANSIENT_MS = 5_000L
