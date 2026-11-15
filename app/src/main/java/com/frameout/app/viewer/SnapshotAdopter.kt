package com.frameout.app.viewer

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.media.ExifInterface
import android.os.Build
import com.frameout.core.db.SQL_IN_CHUNK
import com.frameout.core.db.getByIdsChunked
import com.frameout.core.db.permanentDeleteAtomic
import android.net.Uri
import com.frameout.app.FrameOutApp
import com.frameout.core.db.FrameEntity
import com.frameout.core.db.ProjectEntity
import com.frameout.core.db.SavedImageEntity
import com.frameout.core.db.SnapshotHome
import com.frameout.core.model.ProjectStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

internal object SnapshotAdopter {
    private val mutex = Mutex()
    private const val ORPHAN_AGE_MS = 24L * 60L * 60L * 1000L
    private const val MIN_FREE_BYTES = 32L * 1024L * 1024L
    private const val MAX_PIXELS = 40_000_000L

    suspend fun homeProjectId(app: FrameOutApp): Long = mutex.withLock { ensureHome(app) }

    suspend fun adopt(app: FrameOutApp, saved: SavedImageEntity): FrameEntity? = withContext(Dispatchers.IO) {
        val c = app.container
        saved.sourceFrameId?.let { c.frames.getActiveById(it) }?.let { return@withContext it }
        mutex.withLock {
            val fresh = c.savedImages.getById(saved.id) ?: return@withLock null
            fresh.sourceFrameId?.let { c.frames.getActiveById(it) }?.let { return@withLock it }
            val file = copyToPrivate(app, fresh) ?: return@withLock null
            val projectId = ensureHome(app)
            val index = (c.frames.lastFrame(projectId)?.index ?: -1) + 1
            c.frames.insertAll(listOf(FrameEntity(projectId = projectId, index = index, ptsUs = 0L, fullPath = file.absolutePath)))
            val frame = c.frames.lastFrame(projectId) ?: return@withLock null
            c.savedImages.setSourceFrame(fresh.id, frame.id)
            frame
        }
    }


    suspend fun adoptStills(app: FrameOutApp, uris: List<Uri>): List<FrameEntity> = withContext(Dispatchers.IO) {
        val c = app.container
        mutex.withLock {
            val projectId = ensureHome(app)
            runCatching { purgeOrphans(app, projectId) }
            val adopted = ArrayList<FrameEntity>()
            for (uri in uris) {
                coroutineContext.ensureActive()
                val file = copyStillToPrivate(app, uri) ?: continue
                val index = (c.frames.lastFrame(projectId)?.index ?: -1) + 1
                c.frames.insertAll(listOf(FrameEntity(projectId = projectId, index = index, ptsUs = 0L, fullPath = file.absolutePath)))
                val frame = c.frames.lastFrame(projectId)
                if (frame != null) adopted += frame else file.delete()
            }
            adopted
        }
    }

    private suspend fun purgeOrphans(app: FrameOutApp, projectId: Long) {
        val c = app.container
        val cutoff = System.currentTimeMillis() - ORPHAN_AGE_MS
        val candidates = c.frames.getByIdsChunked(c.frames.listActiveIds(projectId))
        if (candidates.isEmpty()) return
        val kept = candidates.map { it.id }.chunked(SQL_IN_CHUNK).flatMap { c.savedImages.referencedFrameIds(it) }.toHashSet()
        val stale = candidates.filter { frame ->
            frame.id !in kept && frame.fullPath?.let { File(it).let { f -> !f.exists() || f.lastModified() < cutoff } } != false
        }
        if (stale.isEmpty()) return
        val removable = stale.filter { frame ->
            listOfNotNull(frame.thumbnailPath, frame.fullPath).all { path -> File(path).let { !it.exists() || it.delete() } }
        }
        if (removable.isNotEmpty()) c.database.permanentDeleteAtomic(removable.map { it.id })
    }

    private fun copyStillToPrivate(app: Application, uri: Uri): File? {
        val dir = File(app.filesDir, "snapshots").apply { mkdirs() }
        if (dir.usableSpace < MIN_FREE_BYTES) return null
        val out = File(dir, "shared_${System.currentTimeMillis()}_${System.nanoTime()}.img")
        val ok = runCatching {
            val decoder = com.frameout.app.library.SavedMediaDecoder(app)
            val orientation = decoder.orientation(uri)
            if (orientation == ExifInterface.ORIENTATION_NORMAL || orientation == ExifInterface.ORIENTATION_UNDEFINED) {
                if (hasBounds(app, uri)) rawCopy(app, uri, out) else bake(app, uri, out, ExifInterface.ORIENTATION_NORMAL, decoder)
            } else bake(app, uri, out, orientation, decoder)
        }.getOrDefault(false)
        if (ok && out.length() > 0) return out
        out.delete()
        return null
    }

    private fun hasBounds(app: Application, uri: Uri): Boolean {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        runCatching { app.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) } }
        return options.outWidth > 0 && options.outHeight > 0
    }

    private fun rawCopy(app: Application, uri: Uri, out: File): Boolean =
        app.contentResolver.openInputStream(uri)?.use { input -> out.outputStream().use { input.copyTo(it) }; true } ?: false

    private fun bake(app: Application, uri: Uri, out: File, orientation: Int, decoder: com.frameout.app.library.SavedMediaDecoder): Boolean {
        val resolver = app.contentResolver
        var bitmap: Bitmap? = if (hasBounds(app, uri)) decodeSampled(app, uri) else null
        if (bitmap != null && orientation != ExifInterface.ORIENTATION_NORMAL) bitmap = decoder.applyOrientation(bitmap, orientation)
        if (bitmap == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            bitmap = runCatching {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { d, info, _ ->
                    d.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    val pixels = info.size.width.toLong() * info.size.height
                    if (pixels > MAX_PIXELS) {
                        val scale = Math.sqrt(MAX_PIXELS.toDouble() / pixels)
                        d.setTargetSize((info.size.width * scale).toInt().coerceAtLeast(1), (info.size.height * scale).toInt().coerceAtLeast(1))
                    }
                }
            }.getOrNull()
        }
        val ready = bitmap ?: return false
        return try {
            val format = if (ready.hasAlpha()) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
            out.outputStream().use { ready.compress(format, 100, it) }
        } finally {
            if (!ready.isRecycled) ready.recycle()
        }
    }

    private fun decodeSampled(app: Application, uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        app.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth.toLong() * bounds.outHeight / (sample.toLong() * sample) > MAX_PIXELS) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample; inPreferredConfig = Bitmap.Config.ARGB_8888 }
        return app.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }

    private suspend fun ensureHome(app: FrameOutApp): Long {
        val projects = app.container.projects
        projects.getBySourceUri(SnapshotHome.URI)?.let { return it.id }
        val now = System.currentTimeMillis()
        return projects.insert(
            ProjectEntity(
                name = SnapshotHome.NAME,
                sourceUri = SnapshotHome.URI,
                sourceName = SnapshotHome.NAME,
                status = ProjectStatus.Completed.name,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    private fun copyToPrivate(app: Application, saved: SavedImageEntity): File? = runCatching {
        val dir = File(app.filesDir, "snapshots").apply { mkdirs() }
        val out = File(dir, "snap_${saved.id}_${System.currentTimeMillis()}.img")
        val copied = app.contentResolver.openInputStream(Uri.parse(saved.uri))?.use { input ->
            out.outputStream().use { input.copyTo(it) }
            true
        } ?: false
        if (copied && out.length() > 0) out else { out.delete(); null }
    }.getOrNull()
}
