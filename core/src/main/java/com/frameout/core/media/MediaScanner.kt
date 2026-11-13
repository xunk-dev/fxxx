package com.frameout.core.media

import com.frameout.core.util.DiagLog
import com.frameout.core.util.FrameStrings
import android.content.Context
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MediaScanner(private val context: Context) {
    private class Cached(val summary: MediaSummary, val version: Int, val access: Int, val at: Long)

    private companion object {
        const val SUMMARY_TTL_MS = 300_000L
        const val REC = "\u001E"
        const val FLD = "\u001F"
        const val PREFS = "media_summary"
        const val TAG = "picker"

        val summaryMutex = Mutex()
        @Volatile var cached: Cached? = null
    }

    private fun accessSignature(): Int =
        (if (PermissionHelper.canReadVideos(context)) 1 else 0) or
            (if (PermissionHelper.canReadImages(context)) 2 else 0) or
            (if (PermissionHelper.isPartialMediaAccess(context)) 4 else 0)

    fun pagingSource(bucketId: String?, kind: MediaKind?) = MediaStorePagingSource(context, bucketId, kind)

    suspend fun summary(force: Boolean = false): MediaSummary = summaryMutex.withLock {
        MediaChangeTracker.start(context)
        val now = System.currentTimeMillis()
        val version = MediaChangeTracker.version
        val access = accessSignature()
        cached?.takeIf { !force && it.version == version && it.access == access && now - it.at < SUMMARY_TTL_MS }?.let {
            DiagLog.append(TAG, "summary cache hit age=${now - it.at}ms")
            return@withLock it.summary
        }
        val started = System.nanoTime()
        ProbeStats.reset()
        val result = withContext(Dispatchers.IO) { buildSummary(FolderMediaIndex.peek(), probeFiles = false) }
        DiagLog.append(TAG, "summary total=${elapsedMs(started)}ms albums=${result.albums.size} ${ProbeStats.describe()}")
        cached = Cached(result, version, access, now)
        store(result, access)
        result
    }

    private fun elapsedMs(startNanos: Long): Long = (System.nanoTime() - startNanos) / 1_000_000L

    fun peekSummary(): MediaSummary? = cached?.takeIf { it.access == accessSignature() }?.summary ?: restore(accessSignature())

    private fun store(summary: MediaSummary, access: Int) {
        runCatching {
            val albums = summary.albums.joinToString(REC) { listOf(it.bucketId, it.name, it.count.toString()).joinToString(FLD) }
            val filters = summary.filters.joinToString(REC) { (it.kind?.name ?: "ALL") + FLD + it.count }
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt("access", access).putString("albums", albums).putString("filters", filters).apply()
        }
    }

    private fun restore(access: Int): MediaSummary? = runCatching {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getInt("access", -1) != access) return@runCatching null
        val albumsRaw = prefs.getString("albums", null) ?: return@runCatching null
        val albums = if (albumsRaw.isEmpty()) emptyList() else albumsRaw.split(REC).map { record ->
            val f = record.split(FLD)
            MediaAlbum(f[0], f[1], f[2].toInt())
        }
        val filtersRaw = prefs.getString("filters", "").orEmpty()
        val filters = if (filtersRaw.isEmpty()) emptyList() else filtersRaw.split(REC).map { record ->
            val f = record.split(FLD)
            val kind = f[0].takeIf { it != "ALL" }?.let { MediaKind.valueOf(it) }
            MediaFilterCount(kind, filterLabel(kind), f[1].toInt())
        }
        MediaSummary(albums, filters)
    }.getOrNull()

    private fun filterLabel(kind: MediaKind?): String = when (kind) {
        null -> FrameStrings.allMedia()
        MediaKind.Video -> FrameStrings.mediaVideo()
        MediaKind.Gif -> "GIF"
        MediaKind.LivePhoto -> FrameStrings.mediaLive()
        MediaKind.MotionPhoto -> FrameStrings.mediaMotion()
        MediaKind.Animated -> FrameStrings.mediaAnimated()
        MediaKind.Image -> FrameStrings.mediaPhoto()
    }

    private fun buildSummary(folder: List<FolderEntry>, probeFiles: Boolean): MediaSummary {
        val albums = linkedMapOf<String, Pair<String, Int>>()
        var all = 0
        var video = 0
        var gif = 0
        var live = 0
        var motion = 0
        var animated = 0
        scanSummary(folder, probeFiles) { item ->
            val key = item.bucketId.ifEmpty { "0" }
            val old = albums[key]
            albums[key] = (old?.first ?: item.bucketName.ifEmpty { "Media" }) to ((old?.second ?: 0) + 1)
            all++
            when (item.kind) {
                MediaKind.Video -> video++
                MediaKind.Gif -> gif++
                MediaKind.LivePhoto -> live++
                MediaKind.MotionPhoto -> motion++
                MediaKind.Animated -> animated++
                MediaKind.Image -> Unit
            }
        }
        val albumList = albums.map { (id, value) -> MediaAlbum(id, value.first, value.second) }.sortedByDescending { it.count }
        val filters = buildList {
            if (all > 0) add(MediaFilterCount(null, FrameStrings.allMedia(), all))
            if (video > 0) add(MediaFilterCount(MediaKind.Video, FrameStrings.mediaVideo(), video))
            if (gif > 0) add(MediaFilterCount(MediaKind.Gif, "GIF", gif))
            if (live > 0) add(MediaFilterCount(MediaKind.LivePhoto, FrameStrings.mediaLive(), live))
            if (motion > 0) add(MediaFilterCount(MediaKind.MotionPhoto, FrameStrings.mediaMotion(), motion))
            if (animated > 0) add(MediaFilterCount(MediaKind.Animated, FrameStrings.mediaAnimated(), animated))
        }
        return MediaSummary(albumList, filters)
    }

    fun clearSummaryCache() {
        FolderMediaIndex.invalidate()
        cached = null
    }

    private fun scanSummary(folder: List<FolderEntry>, probeFiles: Boolean, consumer: (MediaItem) -> Unit) {
        val liveKeys = linkedSetOf<String>()
        val videos = ArrayList<MediaItem>()
        var mark = System.nanoTime()
        if (PermissionHelper.canReadVideos(context)) {
            MediaStoreMediaQuery.queryCursor(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, MediaStoreMediaQuery.videoProjection, null, true) { item ->
                videos += item
                if (item.durationMs in 0..LivePhotoMatcher.MAX_CLIP_MS) LivePhotoMatcher.videoPairKey(item.bucketId, item.displayName)?.let { liveKeys += it }
            }
        }
        DiagLog.append(TAG, "stage videos rows=${videos.size} ${elapsedMs(mark)}ms")
        val pairedKeys = hashSetOf<String>()
        if (PermissionHelper.canReadImages(context)) {
            mark = System.nanoTime()
            val liveBases = liveKeys + LivePhotoMatcher.heifMarkers(context, liveKeys)
            DiagLog.append(TAG, "stage heifMarkers ${elapsedMs(mark)}ms")
            mark = System.nanoTime()
            var rows = 0
            MediaStoreMediaQuery.queryCursor(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaStoreMediaQuery.imageProjection, null, false, liveBases, MediaStoreMediaQuery.specialImages, probeFiles) { item ->
                rows++
                if (item.kind == MediaKind.LivePhoto) pairedKeys += LivePhotoMatcher.keyOf(item.bucketId, item.displayName)
                consumer(item)
            }
            DiagLog.append(TAG, "stage images kept=$rows probe=$probeFiles ${elapsedMs(mark)}ms ${ProbeStats.describe()}")
        }
        videos.forEach { item ->
            val hidden = item.displayName.endsWith(".mov", true) && item.durationMs in 0..LivePhotoMatcher.MAX_CLIP_MS && LivePhotoMatcher.keyOf(item.bucketId, item.displayName) in pairedKeys
            if (!hidden) consumer(item)
        }
        folder.forEach { consumer(it.toMediaItem()) }
    }
}
