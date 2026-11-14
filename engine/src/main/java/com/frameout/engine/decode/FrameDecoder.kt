package com.frameout.engine.decode

import android.content.Context
import android.graphics.Bitmap
import android.media.Image
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.SystemClock
import com.frameout.codecpack.DecoderSupport
import com.frameout.codecpack.MediaFailure
import com.frameout.codecpack.MediaFailureException
import com.frameout.codecpack.VideoTrackSelector
import com.frameout.core.hdr.HdrSupport
import com.frameout.core.quality.LumaGrid
import com.frameout.core.util.DiagLog
import com.frameout.core.util.logged

class FrameDecoder(private val context: Context) {
    internal var extractor: MediaExtractor? = null
    internal var codec: MediaCodec? = null
    private var trackIndex = -1
    internal var inputDone = false
    internal var outputDone = false
    private val fallback = FallbackFrames(context)
    private val converter = YuvBitmapConverter()
    var captureSignal: Boolean by converter::captureSignal
    private val buffers = BufferYuvConverter()
    private var inputFormat: MediaFormat? = null
    private var sourceUri: Uri? = null
    private var allowP010 = true
    private var calibrationPending = false
    private var flatTries = 0
    private var recreations = 0
    private var forceSoftware = false
    @Volatile var usingP010 = false
        private set
    private var rotation = 0
    private var flipH = false
    private var pixelAspect = 1f
    internal var displayWidth = 0
    internal var displayHeight = 0
    internal val info = MediaCodec.BufferInfo()
    @Volatile var lastOutputPtsUs = Long.MIN_VALUE
        internal set
    @Volatile var drained = false
        internal set
    @Volatile var fallbackFrames = 0
        private set
    @Volatile var consecutiveFallbacks = 0
        private set
    @Volatile var seeksDone = 0
        private set
    @Volatile var seeksAvoided = 0
        private set
    private var probe: MediaExtractor? = null
    private var probeFailed = false

    fun open(uri: Uri) {
        close()
        if (uri != sourceUri) { allowP010 = true; forceSoftware = false; recreations = 0 }
        sourceUri = uri
        probeFailed = false
        usingP010 = false
        val ex = SystemSource.open(context, uri)
        var dec: MediaCodec? = null
        try {
            trackIndex = VideoTrackSelector.select(ex)
            if (trackIndex < 0) throw MediaFailureException(MediaFailure.NoVideoStream, "No video track")
            ex.selectTrack(trackIndex)
            val format = ex.getTrackFormat(trackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: throw MediaFailureException(MediaFailure.UnsupportedContainer, "No mime")
            val name = (if (forceSoftware) DecoderSupport.softwareDecoderName(format) else null) ?: DecoderSupport.decoderName(format)
            dec = SystemSource.createDecoder(name, mime)
            val configured = CodecConfigurator.configure(dec, format, name, mime, allowP010)
            dec = configured.codec
            usingP010 = configured.p010
            inputFormat = format
            val o = readOrientation(context, uri, ex, trackIndex, format)
            rotation = o.rotation
            flipH = o.flipH
            pixelAspect = o.aspect
            displayWidth = o.width
            displayHeight = o.height
            configureConverter()
            prepareCalibration(uri)
            dec.start()
            extractor = ex
            codec = dec
            fallback.bind(uri)
            resetPlayback()
        } catch (e: Exception) {
            runCatching { dec?.stop() }
            runCatching { dec?.release() }
            runCatching { ex.release() }
            throw SystemSource.failOpen(e)
        }
    }

    private fun configureConverter() {
        converter.resetRange(); buffers.resetRange()
        converter.hdrGainmap = HdrSupport.enabled(context)
        converter.also { it.rotation = rotation; it.flipH = flipH; it.aspect = pixelAspect }
    }

    private fun prepareCalibration(uri: Uri) {
        val cached = ColorCalibrator.cached(uri)
        val id = cached ?: ColorVariants.AUTO
        converter.variantId = id
        buffers.variantId = id
        calibrationPending = cached == null && ColorCalibrator.wanted(uri)
        flatTries = 0
    }

    private fun calibrate(image: Image, outFormat: MediaFormat?, ptsUs: Long) {
        if (!calibrationPending || ptsUs < 0L) return
        val uri = sourceUri ?: run { calibrationPending = false; return }
        ColorCalibrator.cached(uri)?.let { shared ->
            calibrationPending = false
            converter.variantId = shared
            buffers.variantId = shared
            return
        }
        val base = converter.peekParams(image, outFormat, inputFormat)
        if (base.hdr && (converter.hdrGainmap || converter.captureSignal)) { calibrationPending = false; return }
        val outcome = runCatching {
            ColorCalibrator.calibrate(context, uri, ptsUs) { id ->
                converter.variantId = id
                val bitmap = imageToBitmap(image, outFormat)
                when {
                    bitmap == null -> null
                    converter.oriented -> bitmap
                    else -> FrameOrientation.apply(bitmap, rotation, pixelAspect, flipH)
                }
            }
        }.logged("FrameDecoder").getOrNull()
        if (outcome != null && outcome.retry && ++flatTries < MAX_FLAT_TRIES) {
            calibrationPending = true
            converter.variantId = ColorVariants.AUTO
            buffers.variantId = ColorVariants.AUTO
            return
        }
        calibrationPending = false
        val picked = outcome?.id ?: ColorVariants.AUTO
        converter.variantId = picked
        buffers.variantId = picked
    }

    private fun resetPlayback() {
        inputDone = false; outputDone = false; lastOutputPtsUs = Long.MIN_VALUE; drained = false
        fallbackFrames = 0; consecutiveFallbacks = 0
    }

    fun reopenWithoutP010(): Boolean {
        val uri = sourceUri ?: return false
        if (!usingP010) return false
        allowP010 = false
        open(uri)
        return true
    }

    fun seekForTarget(timeUs: Long): Boolean {
        val sync = syncAtOrBefore(timeUs)
        if (GopSeek.canDecodeForward(lastOutputPtsUs, timeUs, sync, drained || outputDone || inputDone)) {
            seeksAvoided++
            return false
        }
        seekToKeyframe(timeUs)
        seeksDone++
        return true
    }

    private fun syncAtOrBefore(timeUs: Long): Long {
        if (probeFailed) return -1L
        val uri = sourceUri ?: return -1L
        return try {
            val ex = probe ?: SystemSource.open(context, uri).also { opened ->
                if (trackIndex < 0) { opened.release(); throw IllegalStateException("no track") }
                opened.selectTrack(trackIndex)
                probe = opened
            }
            ex.seekTo(timeUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            ex.sampleTime
        } catch (e: Exception) {
            probeFailed = true
            runCatching { probe?.release() }
            probe = null
            DiagLog.append("decoder", "keyframe probe unavailable, seeking normally: ${e.message}")
            -1L
        }
    }

    fun videoWidth(): Int = displayWidth
    fun videoHeight(): Int = displayHeight
    fun seekToKeyframe(timeUs: Long) {
        extractor?.seekTo(timeUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
        try {
            codec?.flush()
        } catch (e: MediaCodec.CodecException) {
            if (e.isTransient) retryFlush(e) else if (!recreateAfter(e, "flush")) throw e
            extractor?.seekTo(timeUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
        }
        inputDone = false; outputDone = false; lastOutputPtsUs = Long.MIN_VALUE; drained = false
    }

    fun decodeNextBitmap(): Pair<Long, Bitmap>? {
        while (true) convert(nextOutput() ?: return null)?.let { return it }
    }

    internal fun nextOutput(): RawOutput? = try {
        pullOutput()
    } catch (e: MediaCodec.CodecException) {
        recreateAfter(e, "decode")
        throw e
    }

    private fun retryFlush(first: MediaCodec.CodecException) {
        DiagLog.append("decoder", "transient CodecException at flush code=${first.errorCode}, retrying"); SystemClock.sleep(TRANSIENT_RETRY_MS)
        try { codec?.flush() } catch (e: MediaCodec.CodecException) { if (!recreateAfter(e, "flush")) throw e }
    }

    private fun recreateAfter(e: MediaCodec.CodecException, stage: String): Boolean {
        val uri = sourceUri ?: return false
        if (e.isTransient) return false.also { DiagLog.append("decoder", "transient CodecException at $stage code=${e.errorCode}, codec kept") }
        DiagLog.append("decoder", "CodecException at $stage code=${e.errorCode} transient=${e.isTransient} recoverable=${e.isRecoverable} diag=${e.diagnosticInfo} at ${e.stackTrace.take(3).joinToString(" < ")}")
        if (recreations >= MAX_RECREATIONS) return false
        recreations++
        forceSoftware = true
        return runCatching { open(uri) }.onFailure { DiagLog.append("decoder", "recreate failed: $it") }.isSuccess
    }

    internal fun convert(raw: RawOutput): Pair<Long, Bitmap>? {
        val dec = codec ?: return null
        val bitmap = toBitmap(dec, raw.index, raw.ptsUs)
        runCatching { dec.releaseOutputBuffer(raw.index, false) }
        if (raw.ptsUs < 0L && bitmap == null) return null
        if (bitmap == null && raw.size == 0) return null
        if (bitmap != null) {
            consecutiveFallbacks = 0; recreations = 0
            return raw.ptsUs to bitmap
        }
        noteFallback(raw.ptsUs)
        return fallback.frameAt(raw.ptsUs)?.let { raw.ptsUs to it }
    }

    private fun noteFallback(ptsUs: Long) {
        fallbackFrames++
        consecutiveFallbacks++
        if (fallbackFrames == FALLBACK_WARN_AT) logSlowPath(fallbackFrames, ptsUs, inputFormat)
    }

    private fun toBitmap(dec: MediaCodec, index: Int, ptsUs: Long): Bitmap? {
        val viaImage = runCatching {
            val image = dec.getOutputImage(index)
            try {
                image?.let {
                    val outFormat = dec.getOutputFormat(index)
                    calibrate(it, outFormat, ptsUs)
                    imageToBitmap(it, outFormat)
                }
            } finally { image?.close() }
        }.logged("FrameDecoder").getOrNull()
        val decoded = viaImage ?: runCatching { dec.getOutputBuffer(index)?.let { buffers.convert(it, dec.getOutputFormat(index), inputFormat) } }.logged("FrameDecoder").getOrNull()
        if (decoded == null) return null
        return (if (viaImage != null && converter.oriented) decoded else FrameOrientation.apply(decoded, rotation, pixelAspect, flipH)).also { r ->
            if (viaImage != null) converter.takeSignal()?.let { com.frameout.core.hdr.HdrSignalRegistry.attach(r, it) }
        }
    }

    internal fun <T> withLuma(raw: RawOutput, block: (Int, Int, LumaGrid) -> T): T? {
        val dec = codec ?: return null
        return LumaAccess.read(dec, raw.index, inputFormat, rotation, flipH, block)
    }

    internal fun drop(raw: RawOutput) { runCatching { codec?.releaseOutputBuffer(raw.index, false) } }

    fun close() {
        try { codec?.stop() } catch (_: Exception) {}
        try { codec?.release() } catch (_: Exception) {}
        try { extractor?.release() } catch (_: Exception) {}
        try { probe?.release() } catch (_: Exception) {}
        probe = null
        fallback.close()
        converter.release()
        buffers.release()
        codec = null
        extractor = null
    }

    private fun imageToBitmap(image: Image, format: MediaFormat?): Bitmap? = converter.convert(image, format, inputFormat)

    private companion object {
        const val TRANSIENT_RETRY_MS = 20L
        const val FALLBACK_WARN_AT = 3
        const val MAX_RECREATIONS = 5
        const val MAX_FLAT_TRIES = 12
    }
}
