package com.frameout.app

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import com.frameout.app.library.SavedMediaDecoder
import com.frameout.app.viewer.SnapshotAdopter
import com.frameout.core.db.FrameEntity
import com.frameout.core.media.MotionPhotoExtractor

class StillOutcome(
    val frames: List<FrameEntity> = emptyList(),
    val others: List<Uri> = emptyList(),
    val skipped: Int = 0,
    val failed: Int = 0
)

internal object IncomingStill {
    fun isStill(context: Context, uri: Uri): Boolean = runCatching {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri).orEmpty()
        if (mime.startsWith("video/")) return@runCatching false
        val name = displayName(context, uri)
        if (SavedMediaDecoder(context).isMoving(uri, name)) return@runCatching false
        if (MotionPhotoExtractor.isMotionCandidate(mime, name) && MotionPhotoExtractor.isMotionPhotoQuick(context, uri)) return@runCatching false
        decodable(context, uri)
    }.getOrDefault(false)

    private fun decodable(context: Context, uri: Uri): Boolean {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        runCatching { context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) } }
        if (options.outWidth > 0 && options.outHeight > 0) return true
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
        return runCatching {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, _, _ ->
                decoder.setTargetSize(1, 1)
            }.recycle()
            true
        }.getOrDefault(false)
    }

    private fun displayName(context: Context, uri: Uri): String {
        val cursor = runCatching { context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null) }.getOrNull()
        cursor?.use { if (it.moveToFirst()) it.getString(0)?.let { name -> return name } }
        return uri.lastPathSegment.orEmpty()
    }

    suspend fun process(context: Context, uris: List<Uri>): StillOutcome {
        val app = context.applicationContext as? FrameOutApp ?: return StillOutcome(others = uris)
        val (stills, others) = uris.partition { isStill(app, it) }
        if (stills.isEmpty()) return StillOutcome(others = others)
        if (others.isNotEmpty()) return StillOutcome(others = others, skipped = stills.size)
        val frames = SnapshotAdopter.adoptStills(app, stills)
        return StillOutcome(frames = frames, failed = stills.size - frames.size)
    }
}

internal fun StillOutcome.noticeRes(): Int? = when {
    failed > 0 && frames.isEmpty() -> R.string.incoming_photo_failed
    skipped > 0 -> R.string.incoming_photos_skipped
    else -> null
}

internal fun stillRoute(frames: List<FrameEntity>, edit: Boolean = false): AppRoute? =
    frames.firstOrNull()?.let { AppRoute.Viewer(it.projectId, it.id, edit = edit) }
