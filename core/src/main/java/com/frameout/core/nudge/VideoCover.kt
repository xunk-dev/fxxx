package com.frameout.core.nudge

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Size

object VideoCover {
    private const val WIDE_W = 640
    private const val WIDE_H = 360
    private const val ICON = 192

    fun wide(context: Context, uri: Uri, durationMs: Long): Bitmap? {
        val raw = systemThumbnail(context, uri) ?: retrieverFrame(context, uri, durationMs) ?: return null
        return fitWithin(raw, WIDE_W, WIDE_H)
    }

    fun squareIcon(source: Bitmap): Bitmap {
        val side = minOf(source.width, source.height)
        val x = (source.width - side) / 2
        val y = (source.height - side) / 2
        val cropped = Bitmap.createBitmap(source, x, y, side, side)
        return if (side == ICON) cropped else Bitmap.createScaledBitmap(cropped, ICON, ICON, true)
    }

    private fun systemThumbnail(context: Context, uri: Uri): Bitmap? {
        if (Build.VERSION.SDK_INT < 29) return null
        return runCatching { context.contentResolver.loadThumbnail(uri, Size(WIDE_W, WIDE_H), null) }.getOrNull()
    }

    private fun retrieverFrame(context: Context, uri: Uri, durationMs: Long): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val atUs = if (durationMs >= 4_000L) 1_000_000L else 0L
            if (Build.VERSION.SDK_INT >= 27) {
                retriever.getScaledFrameAtTime(atUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, WIDE_W, WIDE_H)
            } else {
                retriever.getFrameAtTime(atUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            }
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun fitWithin(src: Bitmap, maxW: Int, maxH: Int): Bitmap {
        val scale = minOf(maxW.toFloat() / src.width, maxH.toFloat() / src.height, 1f)
        if (scale >= 1f) return src
        return Bitmap.createScaledBitmap(src, (src.width * scale).toInt().coerceAtLeast(1), (src.height * scale).toInt().coerceAtLeast(1), true)
    }
}
