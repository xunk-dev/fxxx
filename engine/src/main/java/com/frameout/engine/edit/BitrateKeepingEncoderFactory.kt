package com.frameout.engine.edit

import android.media.metrics.LogSessionId
import androidx.media3.common.Format
import androidx.media3.transformer.Codec
import androidx.media3.transformer.ExportException

@androidx.media3.common.util.UnstableApi
internal class BitrateKeepingEncoderFactory(
    private val delegate: Codec.EncoderFactory,
    private val videoBps: Int
) : Codec.EncoderFactory {
    override fun createForAudioEncoding(format: Format, logSessionId: LogSessionId?): Codec =
        delegate.createForAudioEncoding(format, logSessionId)

    @Throws(ExportException::class)
    override fun createForVideoEncoding(format: Format, logSessionId: LogSessionId?): Codec {
        val keep = format.averageBitrate == Format.NO_VALUE || format.averageBitrate < videoBps
        val request = if (keep) format.buildUpon().setAverageBitrate(videoBps).build() else format
        return delegate.createForVideoEncoding(request, logSessionId)
    }

    override fun audioNeedsEncoding(): Boolean = false

    override fun videoNeedsEncoding(): Boolean = false
}
