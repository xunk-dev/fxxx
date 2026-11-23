package com.frameout.app.trim

import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal class ScrubGlider(private val scope: CoroutineScope) {
    private var job: Job? = null
    private var finish: (() -> Unit)? = null

    val active: Boolean get() = job?.isActive == true

    fun stop() {
        job?.cancel()
        job = null
        runFinish()
    }

    fun start(glide: ScrubGlide, onStep: (Double) -> Boolean, onEnd: () -> Unit) {
        stop()
        finish = onEnd
        job = scope.launch {
            val origin = withFrameNanos { it }
            var elapsed = 0.0
            var alive = true
            while (alive && elapsed < glide.durationS) {
                elapsed = (withFrameNanos { it } - origin) / NANOS_PER_S
                alive = onStep(glide.at(elapsed))
            }
            this@ScrubGlider.job = null
            runFinish()
        }
    }

    private fun runFinish() {
        val action = finish ?: return
        finish = null
        action()
    }

    private companion object {
        const val NANOS_PER_S = 1_000_000_000.0
    }
}
