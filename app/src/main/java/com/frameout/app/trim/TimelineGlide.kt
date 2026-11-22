package com.frameout.app.trim

import kotlin.math.roundToLong

internal class TimelineGlideSpec(
    val durationMs: Long,
    val frameMs: Double,
    val snap: (Long) -> Long,
    val seek: (Long) -> Unit,
    val scrub: (Boolean) -> Unit,
    val scrubbing: () -> Boolean
)

internal fun ScrubGlider.flingTimeline(startMs: Long, velocityMsPerS: Double, spec: TimelineGlideSpec): Boolean {
    val glide = ScrubGlide.plan(
        startMs.toDouble(),
        velocityMsPerS,
        ScrubProfile.FRICTION_TAU_S,
        0.0,
        spec.durationMs.toDouble(),
        spec.frameMs / 2.0
    ) { spec.snap(it.roundToLong()).toDouble() } ?: return false
    var last = Long.MIN_VALUE
    start(
        glide,
        { position ->
            val ms = position.roundToLong()
            val alive = spec.scrubbing()
            if (alive && ms != last) {
                last = ms
                spec.seek(ms)
            }
            alive
        },
        { spec.scrub(false) }
    )
    return true
}
