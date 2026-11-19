package com.frameout.engine.player

import android.net.Uri
import com.frameout.engine.decode.HdrDetector
import com.frameout.engine.repair.RepairOutcome
import com.frameout.engine.repair.VideoRepair
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal fun PlayerController.setUriImpl(uri: Uri) {
    mediaUri = uri
    fallbackTried = false
    recovery.reset(VideoRepair.isRepairedFile(context.applicationContext, uri))
    clearSource()
    val app = context.applicationContext
    openJob = scope.launch {
        val outcome = VideoRepair.prepare(app, uri)
        val source = (outcome as? RepairOutcome.Repaired)?.uri ?: uri
        val candidate = AnimatedPlayback.looksAnimated(app, source)
        val still = if (candidate) AnimatedPlayback.open(app, source, scope) { _frame.value = it } else null
        val clip = still ?: AnimatedPlayback.openVideo(app, source, scope, false) { _frame.value = it }
        val hdr = clip == null && withContext(Dispatchers.IO) { HdrDetector.isHdr(app, source) }
        val hopeless = clip == null && ((candidate && AnimatedPlayback.isExoHopeless(app, source)) || PreviewSupport.exoCannotPlay(app, source))
        withContext(Dispatchers.Main) {
            if (released || mediaUri != uri) {
                clip?.close()
            } else if (clip != null) {
                adopt(clip)
            } else {
                applyPrepared(outcome, source)
                sourceHdr = hdr
                _hdr.value = hdr
                startExo(source)
            }
        }
    }
}

internal fun PlayerController.clearSource() {
    openJob?.cancel()
    indexJob?.cancel()
    ptsIndex = null
    precapture.clear()
    warmJob?.cancel()
    frameCache?.clear()
    frameCache = null
    lastWarmMs = NO_POSITION
    animated?.close()
    animated = null
    _frame.value = null
    sourceHdr = false
    _hdr.value = false
    dropStill()
    _state.value = _state.value.copy(animated = false, notice = PreviewNotice.None, hasAudio = false)
    grabber?.close()
    grabber = null
    player.stop()
}
