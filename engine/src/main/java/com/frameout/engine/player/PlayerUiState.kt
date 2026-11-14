package com.frameout.engine.player

enum class PreviewNotice { None, StillOnly, Unavailable, Repairing, Repaired, SkippedDamage, Incomplete }

data class PlayerUiState(
    val isPlaying: Boolean = false,
    val durationMs: Long = 0,
    val ready: Boolean = false,
    val speed: Float = PlaybackSpeeds.NORMAL,
    val videoAspect: Float = 0f,
    val animated: Boolean = false,
    val notice: PreviewNotice = PreviewNotice.None,
    val hasAudio: Boolean = false,
    val muted: Boolean = false,
    val buffering: Boolean = false
)
