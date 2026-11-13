package com.frameout.core.storage

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.frameout.core.db.SavedImageDao
import com.frameout.core.db.SavedImageEntity
import com.frameout.core.prefs.AppPrefs
import com.frameout.core.util.DiagLog
import com.frameout.core.util.FrameStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FrameCaptureSaver(private val context: Context, private val prefs: AppPrefs, private val savedImages: SavedImageDao? = null) {
    private val storage = StorageManager(context)
    private val exif = ExifWriter(context)

    suspend fun save(bitmap: Bitmap, format: ImageFormat, source: CaptureSource): CaptureSave = withContext(Dispatchers.IO) {
        val tree = prefs.saveFolderUri.value.ifBlank { null }
        if (!storage.hasDestinationRoom(tree, bitmap.byteCount.toLong())) return@withContext CaptureSave.Failed(CaptureSave.Reason.NoSpace)
        val meta = metaFor(source.uri)
        val project = meta.project
        val stamp = stampFor(meta, source)
        val name = CaptureNaming.name(prefs.namingTemplate.value, project, source.positionMs, source.sequence, format.ext)
        val block = FormatMetadata.block(format, stamp)
        val uri = storage.saveBitmapToGallery(bitmap, name, format, prefs.jpegQuality.value, prefs.saveFolder.value, stamp.takenAt, tree, "", block)
            ?: return@withContext CaptureSave.Failed(CaptureSave.Reason.Unwritable)
        val metadataExpected = format.modern || block != null || exif.supports(format, stamp)
        val metadataWritten = when {
            format == ImageFormat.Png || format == ImageFormat.PngHdr16 -> block != null
            format.modern -> block != null
            exif.supports(format, stamp) -> exif.write(uri, format, stamp)
            else -> true
        }
        if (!hasContent(uri)) return@withContext discard(uri)
        register(uri, name, bitmap, stamp.takenAt)
        CaptureSave.Saved(uri, format.mime, name, !metadataExpected || metadataWritten)
    }

    private suspend fun register(uri: Uri, name: String, bitmap: Bitmap, takenAt: Long) {
        val dao = savedImages ?: return
        try {
            dao.insert(SavedImageEntity(uri = uri.toString(), displayName = name, album = FrameStrings.appName(), width = bitmap.width, height = bitmap.height, createdAt = takenAt))
        } catch (e: Exception) {
            DiagLog.append("capture", "snapshot saved to the gallery but not listed in Saved: ${e.javaClass.simpleName}")
        }
    }

    fun failureMessage(failed: CaptureSave.Failed): String = when (failed.reason) {
        CaptureSave.Reason.NoSpace -> FrameStrings.noSpace()
        CaptureSave.Reason.Empty -> FrameStrings.saveEmpty()
        CaptureSave.Reason.Unwritable -> SaveFailure.message(context, FrameStrings.saveFailed())
    }

    private class SourceMeta(val uri: String, val project: String, val date: Long?) {
        @Volatile var location: GeoPoint? = null
        @Volatile var locationRead = false
    }

    @Volatile private var cachedMeta: SourceMeta? = null

    private fun metaFor(uri: String): SourceMeta {
        cachedMeta?.takeIf { it.uri == uri }?.let { return it }
        return SourceMeta(uri, SourceInfo.name(context, uri), SourceDate.read(context, uri)).also { cachedMeta = it }
    }

    suspend fun prewarm(uri: String) = withContext(Dispatchers.IO) {
        val meta = metaFor(uri)
        if (prefs.keepLocation.value && !meta.locationRead) {
            meta.location = SourceLocation.read(context, uri)
            meta.locationRead = true
        }
    }

    private fun stampFor(meta: SourceMeta, source: CaptureSource): ExifStamp {
        val base = meta.date ?: System.currentTimeMillis()
        val takenAt = if (prefs.datePolicy.value == "video") base else base + source.positionMs
        val keep = prefs.keepLocation.value
        if (keep && !meta.locationRead) {
            meta.location = SourceLocation.read(context, source.uri)
            meta.locationRead = true
        }
        val location = if (keep) meta.location else null
        return ExifStamp(takenAt, ExifText.description(meta.project, source.sequence, source.positionMs * 1000L), location, keep)
    }

    private suspend fun discard(uri: Uri): CaptureSave {
        storage.deleteGalleryUri(uri.toString())
        return CaptureSave.Failed(CaptureSave.Reason.Empty)
    }

    private fun hasContent(uri: Uri): Boolean = runCatching {
        context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length != 0L } ?: false
    }.getOrDefault(false)
}
