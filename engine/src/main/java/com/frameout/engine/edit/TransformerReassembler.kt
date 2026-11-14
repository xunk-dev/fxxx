package com.frameout.engine.edit

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.frameout.core.util.logFailure
import com.frameout.engine.work.ReencodeForeground
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import kotlin.coroutines.resume

@androidx.media3.common.util.UnstableApi
internal class TransformerReassembler(private val context: Context) {
    suspend fun reencode(plan: ReassemblePlan, out: File, optimizeTrim: Boolean = false): ReassembleResult {
        val sourceBps = withContext(Dispatchers.IO) { SourceBitrate.videoBps(context, Uri.parse(plan.sourceUri)) }
        val result = try {
            withTimeoutOrNull(TIMEOUT_MS) { ReencodeForeground.around(context) { run(plan, out, optimizeTrim, sourceBps) } }
        } catch (e: CancellationException) {
            out.delete()
            throw e
        }
        if (result == null) {
            out.delete()
            return ReassembleResult.Failure("Reassembly timed out")
        }
        if (result !is ReassembleResult.Success) out.delete()
        return result
    }

    private suspend fun run(plan: ReassemblePlan, out: File, optimizeTrim: Boolean, sourceBps: Long?): ReassembleResult = suspendCancellableCoroutine { cont ->
        val main = Handler(Looper.getMainLooper())
        var transformer: Transformer? = null
        val listener = object : Transformer.Listener {
            override fun onCompleted(composition: Composition, result: ExportResult) {
                val outcome = if (out.isFile && out.length() > 0L) ReassembleResult.Success(out.absolutePath, out.length()) else ReassembleResult.Failure("Empty output")
                if (cont.isActive) cont.resume(outcome)
            }

            override fun onError(composition: Composition, result: ExportResult, exception: ExportException) {
                logFailure("TransformerReassembler", exception)
                if (cont.isActive) cont.resume(ReassembleResult.Failure("Reassembly failed: ${exception.errorCodeName}"))
            }
        }
        cont.invokeOnCancellation {
            main.post {
                runCatching { transformer?.cancel() }
                out.delete()
            }
        }
        main.post {
            if (!cont.isActive) return@post
            try {
                transformer = startTransformer(plan, listener, out, optimizeTrim, sourceBps)
            } catch (e: Exception) {
                logFailure("TransformerReassembler", e)
                if (cont.isActive) cont.resume(ReassembleResult.Failure("Reassembly failed to start"))
            }
        }
    }

    private fun startTransformer(plan: ReassemblePlan, listener: Transformer.Listener, out: File, optimizeTrim: Boolean, sourceBps: Long?): Transformer {
        val items = plan.keptRanges.map { range ->
            val media = MediaItem.Builder().setUri(Uri.parse(plan.sourceUri)).setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(range.startUs / 1000L)
                    .setEndPositionMs(range.endUs / 1000L)
                    .build()
            ).build()
            EditedMediaItem.Builder(media).build()
        }
        val composition = Composition.Builder(EditedMediaItemSequence.withAudioAndVideoFrom(items)).build()
        val builder = Transformer.Builder(context)
            .setLooper(Looper.getMainLooper())
            .addListener(listener)
            .experimentalSetTrimOptimizationEnabled(optimizeTrim)
        sourceBps?.let {
            val target = SourceBitrate.target(it).let { bps -> if (plan.verifiedQuality) bps else degraded(bps) }
            builder.setEncoderFactory(BitrateKeepingEncoderFactory(DefaultEncoderFactory.Builder(context).build(), target))
        }
        val transformer = builder.build()
        transformer.start(composition, out.absolutePath)
        return transformer
    }

    private fun degraded(targetBps: Int): Int = (targetBps / DEGRADE_FACTOR).coerceAtLeast(MIN_DEGRADED_BPS)

    private companion object {
        const val TIMEOUT_MS = 30L * 60L * 1000L
        const val DEGRADE_FACTOR = 4
        const val MIN_DEGRADED_BPS = 400_000
    }
}
