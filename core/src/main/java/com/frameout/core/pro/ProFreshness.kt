package com.frameout.core.pro

object ProFreshness {
    fun fresh(now: Long, verifiedAt: Long, highWater: Long, graceMs: Long, skewMs: Long): Boolean {
        if (verifiedAt <= 0L) return false
        if (now + skewMs < highWater) return false
        if (verifiedAt > now + skewMs) return false
        return now - verifiedAt < graceMs
    }
}
