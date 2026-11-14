package com.frameout.engine.index

import java.io.File
import kotlin.math.abs

internal object IndexReuse {
    const val DURATION_TOLERANCE_MS = 1000L
    private const val MARKER = "index.system"
    private const val WINDOW_MARKER = "index.window"
    private const val FULL = "full"

    fun canReuse(markerCount: Int?, storedCount: Int, projectDurationMs: Long, trackDurationUs: Long): Boolean {
        if (markerCount == null || storedCount <= 0 || markerCount != storedCount) return false
        if (projectDurationMs <= 0L || trackDurationUs <= 0L) return false
        return abs(trackDurationUs / 1000L - projectDurationMs) <= DURATION_TOLERANCE_MS
    }

    fun coversRequest(stored: ScanWindow?, requested: ScanWindow?): Boolean {
        if (stored == null) return true
        if (requested == null) return false
        return stored.covers(requested)
    }

    fun readMarker(dir: File): Int? =
        runCatching { File(dir, MARKER).takeIf { it.isFile }?.readText()?.trim()?.toInt() }.getOrNull()

    fun writeMarker(dir: File, frameCount: Int) {
        runCatching { File(dir, MARKER).writeText(frameCount.toString()) }
    }

    fun readWindow(dir: File): ScanWindow? =
        runCatching { File(dir, WINDOW_MARKER).takeIf { it.isFile }?.readText() }.getOrNull()?.let(ScanWindow::decode)

    fun writeWindow(dir: File, window: ScanWindow?) {
        runCatching { File(dir, WINDOW_MARKER).writeText(window?.encode() ?: FULL) }
    }

    fun clearMarker(dir: File) {
        File(dir, MARKER).delete()
        File(dir, WINDOW_MARKER).delete()
    }
}
