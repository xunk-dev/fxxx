package com.frameout.app

import android.content.Context
import android.net.Uri
import com.frameout.app.trim.estimateFrames
import com.frameout.core.db.ProjectEntity
import com.frameout.core.util.AppContainer
import com.frameout.core.util.FrameStrings
import com.frameout.engine.extract.ExtractModeCodec
import com.frameout.engine.extract.ExtractRequest
import com.frameout.engine.timeline.TrimRange
import com.frameout.engine.work.ExtractScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val BackgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

suspend fun launchExtract(
    context: Context,
    container: AppContainer,
    uri: Uri,
    range: TrimRange,
    autoSave: Boolean,
    awaitCover: Boolean = false,
    onLowSpace: (() -> Unit)? = null
): Long {
    val prefs = container.prefs
    val useAuto = autoSave || prefs.autoSave.value
    val modePref = prefs.extractMode.value
    val grant = resolveSmartGrant(prefs, ExtractModeCodec.decode(modePref))
    val mode = grant.mode
    val startMs = if (range.useFull) 0 else range.startMs
    val endMs = range.endMs
    val name = com.frameout.core.util.NameSanitizer.clean(uri.lastPathSegment?.substringAfterLast('/'), "video")
    val now = System.currentTimeMillis()
    val projectId = container.projects.insert(
        ProjectEntity(
            name = name,
            sourceUri = uri.toString(),
            sourceName = name,
            createdAt = now,
            updatedAt = now,
            extractMode = ExtractModeCodec.encode(mode),
            gateFlags = grant.gateFlags,
            autoSave = useAuto,
            saveFormat = prefs.saveFormat.value,
            jpegQuality = prefs.jpegQuality.value,
            startMs = startMs,
            endMs = range.endMs
        )
    )
    ExtractScheduler.enqueue(context, ExtractRequest(projectId, uri.toString(), startMs, endMs, mode, useAuto, prefs.saveFormat.value, prefs.jpegQuality.value))
    val appContext = context.applicationContext

    BackgroundScope.launch {
        runCatching {
            val probe = com.frameout.app.trim.SourceProbe.load(appContext, uri)
            val estFrames = estimateFrames(range.endMs, range, ExtractModeCodec.encode(mode), probe)
            if (!container.storage.hasSpaceForResolution(estFrames, 0, 0, useAuto, prefs.saveFormat.value)) {
                ExtractScheduler.cancel(appContext, projectId)
                onLowSpace?.let { withContext(Dispatchers.Main) { it() } }
            }
        }
    }
    if (awaitCover) {
        ProjectCover.create(appContext, container.storage, container.projects, projectId, uri, startMs)
    } else {
        BackgroundScope.launch { ProjectCover.create(appContext, container.storage, container.projects, projectId, uri, startMs) }
    }
    return projectId
}
