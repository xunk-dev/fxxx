package com.frameout.engine.repair

sealed class RecoveryStep {
    data class SkipTo(val positionMs: Long) : RecoveryStep()
    data object Salvage : RecoveryStep()
    data object GiveUp : RecoveryStep()
}

object RecoveryPlan {
    private val RECOVERABLE = setOf(1000, 1004, 2000, 2002, 2008, 3001, 4003)
    private val SKIP_LADDER_MS = longArrayOf(1L, 750L, 2_000L, 5_000L, 10_000L, 20_000L)
    const val END_MARGIN_MS = 1_000L
    const val PROGRESS_RESET_MS = 3_000L

    fun isRecoverable(errorCode: Int): Boolean = errorCode in RECOVERABLE

    fun next(attempt: Int, positionMs: Long, durationMs: Long, alreadySalvaged: Boolean): RecoveryStep {
        val nearEnd = durationMs > 0L && positionMs >= durationMs - END_MARGIN_MS
        if (nearEnd || attempt >= SKIP_LADDER_MS.size) return exhausted(alreadySalvaged)
        val target = positionMs + SKIP_LADDER_MS[attempt]
        if (durationMs > 0L && target >= durationMs - END_MARGIN_MS) return exhausted(alreadySalvaged)
        return RecoveryStep.SkipTo(target)
    }

    private fun exhausted(alreadySalvaged: Boolean): RecoveryStep =
        if (alreadySalvaged) RecoveryStep.GiveUp else RecoveryStep.Salvage

    fun resetsAttempts(positionMs: Long, lastTargetMs: Long): Boolean =
        lastTargetMs >= 0L && positionMs >= lastTargetMs + PROGRESS_RESET_MS
}
