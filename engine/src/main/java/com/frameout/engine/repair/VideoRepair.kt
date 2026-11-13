package com.frameout.engine.repair

import android.content.Context
import android.net.Uri
import com.frameout.core.util.logFailure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

sealed class RepairOutcome {
    data object Intact : RepairOutcome()
    data class Repaired(val uri: Uri, val droppedMs: Long, val truncated: Boolean) : RepairOutcome()
    data class Unrecoverable(val reason: Reason) : RepairOutcome()
    enum class Reason { MissingIndex, NoSpace, Failed }
}

object VideoRepair {
    private const val DIR = "repaired"
    private const val KEEP = 20
    private const val RESERVE_BYTES = 64L * 1024 * 1024

    fun directory(context: Context): File = File(context.filesDir, DIR).apply { mkdirs() }

    fun isRepairedFile(context: Context, uri: Uri): Boolean {
        val path = if (uri.scheme == "file") uri.path else null
        return path != null && File(path).parentFile?.canonicalPath == directory(context).canonicalPath
    }

    suspend fun prepare(context: Context, uri: Uri): RepairOutcome = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        if (isRepairedFile(app, uri)) return@withContext RepairOutcome.Intact
        val report = inspect(app, uri) ?: return@withContext RepairOutcome.Intact
        val cached = cacheFile(app, uri)
        when {
            cached.isFile && cached.length() > 0L ->
                RepairOutcome.Repaired(Uri.fromFile(cached), 0L, false)
            report.health == ContainerHealth.MissingIndex -> RepairOutcome.Unrecoverable(RepairOutcome.Reason.MissingIndex)
            report.needsRepair -> salvage(app, uri, DropRanges())
            else -> RepairOutcome.Intact
        }
    }

    suspend fun salvage(context: Context, uri: Uri, drops: DropRanges): RepairOutcome = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        val target = if (isRepairedFile(app, uri)) File(uri.path!!) else cacheFile(app, uri)
        val needed = sizeOf(app, uri)
        if (needed > 0L && directory(app).usableSpace < needed + RESERVE_BYTES) {
            return@withContext RepairOutcome.Unrecoverable(RepairOutcome.Reason.NoSpace)
        }
        val part = File(target.parentFile, target.name + ".part")
        val stats = runCatching { SalvageRemuxer(app).run(uri, part, drops) }.onFailure { logFailure("VideoRepair", it) }.getOrNull()
        if (stats == null || !commit(part, target)) {
            part.delete()
            return@withContext RepairOutcome.Unrecoverable(RepairOutcome.Reason.Failed)
        }
        prune(app, target)
        RepairOutcome.Repaired(Uri.fromFile(target), stats.droppedUs / 1000L, stats.truncated)
    }

    private fun commit(part: File, target: File): Boolean = part.length() > 0L && part.renameTo(target)

    private fun inspect(context: Context, uri: Uri): ContainerReport? = runCatching {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            BoxScan.scan(FileInputStream(pfd.fileDescriptor).channel)
        }
    }.getOrNull()

    private fun sizeOf(context: Context, uri: Uri): Long = runCatching {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize }
    }.getOrNull() ?: -1L

    private fun cacheFile(context: Context, uri: Uri): File {
        val digest = MessageDigest.getInstance("SHA-1").digest("$uri|${sizeOf(context, uri)}".toByteArray())
        val name = digest.joinToString("") { "%02x".format(it) }.take(24)
        return File(directory(context), "$name.mp4")
    }

    private fun prune(context: Context, keep: File) {
        val files = directory(context).listFiles { f -> f.isFile && f.name.endsWith(".mp4") }.orEmpty()
        files.filter { it != keep }.sortedByDescending { it.lastModified() }.drop(KEEP - 1).forEach { it.delete() }
    }
}
