package com.frameout.engine.decode

internal object GopSeek {
    fun canDecodeForward(lastOutputPtsUs: Long, targetPtsUs: Long, syncPtsUs: Long, drained: Boolean): Boolean {
        if (drained) return false
        if (lastOutputPtsUs == Long.MIN_VALUE) return false
        if (syncPtsUs < 0L) return false
        if (targetPtsUs <= lastOutputPtsUs) return false
        return lastOutputPtsUs >= syncPtsUs
    }
}
