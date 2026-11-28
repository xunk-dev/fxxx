package com.frameout.core.pro

internal object TrialWindow {
    fun expired(now: Long, lastPick: Long, started: Long): Boolean =
        now - lastPick > TRIAL_IDLE_MS || now - started > TRIAL_CAP_MS
}
