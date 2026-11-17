package com.frameout.core.nudge

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.frameout.core.media.PermissionHelper

data class NewVideo(
    val id: Long,
    val uri: Uri,
    val name: String,
    val addedSec: Long,
    val durationMs: Long,
    val sizeBytes: Long
)

data class ScanResult(
    val videos: List<NewVideo>,
    val advanceTo: Long?
)

object NewVideoScanner {
    private const val ROW_CAP = 40
    private const val UNREADY_WINDOW_MS = 3L * 60L * 1000L

    private val collection: Uri get() = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

    @Suppress("DEPRECATION")
    private fun pathColumn(): String =
        if (Build.VERSION.SDK_INT >= 29) MediaStore.MediaColumns.RELATIVE_PATH else MediaStore.MediaColumns.DATA

    private fun projection() = arrayOf(
        MediaStore.Video.Media._ID,
        MediaStore.Video.Media.DISPLAY_NAME,
        MediaStore.Video.Media.DATE_ADDED,
        MediaStore.Video.Media.DURATION,
        MediaStore.Video.Media.SIZE,
        pathColumn()
    )

    fun scan(
        context: Context,
        sinceSec: Long,
        nowMs: Long,
        knownSources: Set<String>,
        alreadyNotified: Set<Long>
    ): ScanResult {
        if (!PermissionHelper.canReadVideos(context)) return ScanResult(emptyList(), null)
        val found = ArrayList<NewVideo>()
        var maxSettled = -1L
        var minUnready = Long.MAX_VALUE
        runCatching {
            context.contentResolver.query(
                collection,
                projection(),
                "${MediaStore.Video.Media.DATE_ADDED} >= ?",
                arrayOf(sinceSec.toString()),
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )?.use { c ->
                var rows = 0
                while (c.moveToNext() && rows < ROW_CAP) {
                    rows++
                    val id = c.getLong(0)
                    val name = c.getString(1).orEmpty()
                    val addedSec = c.getLong(2)
                    val duration = c.getLong(3)
                    val size = c.getLong(4)
                    val path = c.getString(5).orEmpty()
                    val ageMs = nowMs - addedSec * 1000L

                    val stillWriting = (size <= 0L || duration <= 0L) && ageMs in 0..UNREADY_WINDOW_MS
                    if (stillWriting) {
                        minUnready = minOf(minUnready, addedSec)
                        continue
                    }
                    maxSettled = maxOf(maxSettled, addedSec)

                    val uri = ContentUris.withAppendedId(collection, id)
                    val skip = name.startsWith(".") ||
                        duration < NudgePolicy.MIN_VIDEO_MS ||
                        ageMs < 0L || ageMs > NudgePolicy.MAX_VIDEO_AGE_MS ||
                        NudgePolicy.isBlockedPath(path) ||
                        id in alreadyNotified ||
                        uri.toString() in knownSources
                    if (!skip) found += NewVideo(id, uri, name, addedSec, duration, size)
                }
            }
        }
        val advance = when {
            maxSettled < 0L -> null
            minUnready != Long.MAX_VALUE -> minOf(maxSettled, minUnready - 1L)
            else -> maxSettled
        }
        return ScanResult(found, advance)
    }

    fun byId(context: Context, id: Long): NewVideo? {
        if (!PermissionHelper.canReadVideos(context)) return null
        return runCatching {
            context.contentResolver.query(
                collection, projection(), "${MediaStore.Video.Media._ID} = ?", arrayOf(id.toString()), null
            )?.use { c ->
                if (!c.moveToFirst()) return@use null
                NewVideo(
                    id = c.getLong(0),
                    uri = ContentUris.withAppendedId(collection, c.getLong(0)),
                    name = c.getString(1).orEmpty(),
                    addedSec = c.getLong(2),
                    durationMs = c.getLong(3),
                    sizeBytes = c.getLong(4)
                )
            }
        }.getOrNull()
    }
}
