package com.frameout.engine.decode

import android.media.MediaCodec
import android.media.MediaExtractor
import android.os.SystemClock
import com.frameout.codecpack.MediaFailure
import com.frameout.codecpack.MediaFailureException

private const val MAX_PENDING_INPUTS = 512
private const val MAX_FEED_BURST = 32

internal fun FrameDecoder.pullOutput(): RawOutput? {
    val ex = extractor ?: return null
    val dec = codec ?: return null
    var progressAt = SystemClock.elapsedRealtime()
    val timeoutMs = DecoderTimeouts.outputMs(displayWidth, displayHeight)
    var pending = 0
    while (!outputDone) {
        val fed = feedAvailable(ex, dec)
        if (fed > 0) { progressAt = SystemClock.elapsedRealtime(); pending += fed }
        dequeueRaw(dec)?.let { return it }
        if (outputDone) break
        if (pending > MAX_PENDING_INPUTS || SystemClock.elapsedRealtime() - progressAt > timeoutMs) {
            throw MediaFailureException(MediaFailure.Internal, "Decoder stalled: no output for ${timeoutMs}ms (not necessarily corrupt data)")
        }
    }
    return null
}

internal fun FrameDecoder.feedAvailable(ex: MediaExtractor, dec: MediaCodec): Int {
    var count = 0
    while (!inputDone && count < MAX_FEED_BURST && feedInput(ex, dec)) count++
    return count
}

internal fun FrameDecoder.feedInput(ex: MediaExtractor, dec: MediaCodec): Boolean {
    val inIdx = dec.dequeueInputBuffer(0)
    if (inIdx < 0) return false
    val buf = dec.getInputBuffer(inIdx) ?: run { dec.queueInputBuffer(inIdx, 0, 0, 0, 0); return false }
    val size = ex.readSampleData(buf, 0)
    if (size < 0) {
        dec.queueInputBuffer(inIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
        inputDone = true
    } else {
        dec.queueInputBuffer(inIdx, 0, size, ex.sampleTime, 0)
        ex.advance()
    }
    return true
}

internal fun FrameDecoder.dequeueRaw(dec: MediaCodec): RawOutput? {
    val outIdx = dec.dequeueOutputBuffer(info, 10_000)
    if (outIdx < 0) return null
    val eos = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
    if (eos) { outputDone = true; drained = true }
    if (!eos || info.size > 0) lastOutputPtsUs = info.presentationTimeUs
    if (eos && info.size == 0) {
        val hasImage = runCatching { dec.getOutputImage(outIdx)?.also { it.close() } != null }.getOrDefault(false)
        if (!hasImage) {
            runCatching { dec.releaseOutputBuffer(outIdx, false) }
            return null
        }
    }
    return RawOutput(outIdx, info.presentationTimeUs, info.size, info.flags)
}
