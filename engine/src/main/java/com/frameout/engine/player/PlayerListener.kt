package com.frameout.engine.player

import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize

internal class PlayerListener(private val controller: PlayerController) : Player.Listener {
    override fun onIsPlayingChanged(isPlaying: Boolean) {
        controller._state.value = controller._state.value.copy(isPlaying = isPlaying)
    }

    override fun onVideoSizeChanged(videoSize: VideoSize) {
        if (videoSize.width <= 0 || videoSize.height <= 0) return
        val swapped = videoSize.unappliedRotationDegrees % 180 != 0
        val w = videoSize.width * videoSize.pixelWidthHeightRatio
        val h = videoSize.height.toFloat()
        controller._state.value = controller._state.value.copy(videoAspect = if (swapped) h / w else w / h)
    }

    override fun onTracksChanged(tracks: Tracks) {
        controller._state.value = controller._state.value.copy(hasAudio = tracks.containsType(C.TRACK_TYPE_AUDIO))
        if (tracks.containsType(C.TRACK_TYPE_VIDEO) && !tracks.isTypeSupported(C.TRACK_TYPE_VIDEO)) controller.fallbackToFfmpeg()
    }

    override fun onPlayerError(error: PlaybackException) {
        if (controller.tryRecover(error)) return
        val notice = if (controller.recovery.incomplete) PreviewNotice.Incomplete else PreviewNotice.Unavailable
        controller._state.value = controller._state.value.copy(ready = false, isPlaying = false, notice = notice)
        controller.fallbackToFfmpeg()
    }

    override fun onRenderedFirstFrame() {
        if (controller.awaitRender) controller.releaseCachedStill()
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        controller.noteBuffering(playbackState == Player.STATE_BUFFERING)
        controller._state.value = controller._state.value.copy(buffering = controller.spinnerVisible())
        if (playbackState == Player.STATE_READY) {
            controller.onReadyAfterRecovery()
            controller._state.value = controller._state.value.copy(ready = true, durationMs = controller.player.duration.coerceAtLeast(0))
        }
    }
}
