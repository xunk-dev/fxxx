package com.frameout.app.collage

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.Color
import com.frameout.app.FrameOutApp
import com.frameout.app.IncomingStill
import com.frameout.app.viewer.SnapshotAdopter
import com.frameout.app.trim.SavedCapture
import com.frameout.core.collage.COLLAGE_CORNER_REFERENCE
import com.frameout.core.collage.COLLAGE_MAX_WIDTH
import com.frameout.core.collage.CollageAutoAspect
import com.frameout.core.collage.CellCrop
import com.frameout.core.collage.CollageAspect
import com.frameout.core.collage.CollageTemplate
import com.frameout.core.collage.CollageTemplates
import com.frameout.core.db.FrameEntity
import com.frameout.core.prefs.AppPrefs
import com.frameout.core.util.ThumbCache
import com.frameout.engine.timeline.formatTimestamp
import kotlinx.coroutines.flow.first


@Stable
class CollageState(frameCount: Int, prefs: AppPrefs) {
    var frames by mutableStateOf<List<FrameEntity>>(emptyList())
    var paths by mutableStateOf<List<String?>>(emptyList())
    var previews by mutableStateOf<List<Bitmap?>>(emptyList())
    var order by mutableStateOf<List<Int>>(emptyList())
    var ready by mutableStateOf(false)
    var template by mutableStateOf(CollageTemplates.suggest(frameCount))
    var aspect by mutableStateOf(CollageAspect.Template)
    var resolution by mutableIntStateOf(COLLAGE_MAX_WIDTH)
    var crops by mutableStateOf<List<CellCrop>>(emptyList())
    var gap by mutableIntStateOf(prefs.collageGap.value)
    var bgColor by mutableIntStateOf(prefs.collageBgColor.value)
    var pickerOpen by mutableStateOf(false)
    var rounded by mutableStateOf(prefs.collageRounded.value)
    var stamps by mutableStateOf(prefs.collageStamps.value)
    var selectedCell by mutableStateOf<Int?>(null)
    var pickerSlot by mutableStateOf<Int?>(null)
    var galleryIds by mutableStateOf<Set<Long>>(emptySet())
    var saving by mutableStateOf(false)
    var message by mutableStateOf<String?>(null)
    var messageIsError by mutableStateOf(false)
    var result by mutableStateOf<SavedCapture?>(null)
    private var userPicked = false
    var saveJob: kotlinx.coroutines.Job? = null
    private var leaveArmed = false

    fun holdBack(): Boolean {
        if (!saving) return false
        if (leaveArmed) {
            saveJob?.cancel()
            return false
        }
        leaveArmed = true
        messageIsError = false
        message = com.frameout.core.util.ClipStrings.saveWait()
        return true
    }

    fun saveEnded() {
        saving = false
        saveJob = null
        leaveArmed = false
    }

    var aspectAuto by mutableStateOf(true)

    val effectiveAspect: CollageAspect
        get() = if (aspectAuto) CollageAutoAspect.choose(template, previews.map { p -> p?.let { it.width.toFloat() / it.height } ?: 0f }) else aspect

    val canvasTemplate: CollageTemplate get() = template.withAspect(effectiveAspect)
    val background: Color get() = Color(bgColor)
    val style: PreviewStyle get() = PreviewStyle(gap, if (rounded) COLLAGE_CORNER_REFERENCE else 0f, background, stamps)

    fun pickTemplate(t: CollageTemplate) {
        template = t
        userPicked = true
        selectedCell = null
        crops = crops.map { CellCrop() }
    }

    fun cropOf(frameIndex: Int): CellCrop = crops.getOrNull(frameIndex) ?: CellCrop()

    fun setCrop(cell: Int, crop: CellCrop) {
        val frameIndex = order.getOrNull(cell) ?: return
        if (frameIndex !in crops.indices) return
        crops = crops.toMutableList().also { it[frameIndex] = crop }
    }

    fun tapCell(i: Int) {
        val filled = i < order.size && i < template.cells.size
        if (!filled && i < template.cells.size && !saving) {
            selectedCell = null
            pickerSlot = i
            return
        }
        val sel = selectedCell
        selectedCell = when {
            !filled -> null
            sel == null -> i
            sel == i -> null
            else -> {
                swap(sel, i)
                null
            }
        }
    }

    fun swapCells(a: Int, b: Int) {
        if (saving || a == b || a !in order.indices || b !in order.indices) return
        swap(a, b)
        selectedCell = null
    }

    private fun swap(a: Int, b: Int) {
        val first = order[a]
        val second = order[b]
        order = order.toMutableList().also { it[a] = second; it[b] = first }
        crops = crops.toMutableList().also {
            if (first in it.indices) it[first] = CellCrop()
            if (second in it.indices) it[second] = CellCrop()
        }
    }

    fun slots(): List<PreviewSlot?> = template.cells.indices.map { i ->
        val idx = order.getOrNull(i) ?: return@map null
        val frame = frames.getOrNull(idx) ?: return@map null
        PreviewSlot(previews.getOrNull(idx), stampOf(frame), i + 1, cropOf(idx))
    }

    suspend fun addFrame(context: Context, app: FrameOutApp, frame: FrameEntity): Boolean {
        if (!ready) snapshotFlow { ready }.first { it }
        if (frames.any { it.id == frame.id } || order.size >= template.cells.size) return false
        val path = CollageMaterializer(context, app.container.projects, app.container.frameEdits).materialize(listOf(frame)).firstOrNull() ?: return false
        val thumb = ThumbCache.load(frame.thumbnailPath ?: frame.fullPath, 512)
        val index = frames.size
        frames = frames + frame
        paths = paths + path
        previews = previews + (loadPreview(path) ?: thumb)
        crops = crops + CellCrop()
        order = order + index
        return true
    }

    fun cropsInOrder(): List<CellCrop> = order.map { cropOf(it) }

    fun stampsInOrder(): List<String> = order.map { stampOf(frames.getOrNull(it)) }

    private fun stampOf(frame: FrameEntity?): String =
        if (frame == null || frame.id in galleryIds) "" else formatTimestamp(frame.ptsUs / 1000L)

    val roomLeft: Int get() = (template.cells.size - order.size).coerceAtLeast(0)

    suspend fun addGallery(context: Context, app: FrameOutApp, uris: List<Uri>): Int {
        val room = roomLeft
        if (room == 0 || uris.isEmpty()) return 0
        val stills = uris.filter { IncomingStill.isStill(context, it) }.take(room)
        val adopted = if (stills.isEmpty()) emptyList() else SnapshotAdopter.adoptStills(app, stills)
        galleryIds = galleryIds + adopted.map { it.id }
        return adopted.count { addFrame(context, app, it) }
    }

    suspend fun load(context: Context, app: FrameOutApp, ids: List<Long>) {
        ready = false
        val loaded = ids.mapNotNull { app.container.frames.getById(it) }
        frames = loaded
        order = loaded.indices.toList()
        crops = loaded.map { CellCrop() }
        if (!userPicked) template = CollageTemplates.suggest(loaded.size)
        previews = loaded.map { f -> ThumbCache.load(f.thumbnailPath ?: f.fullPath, 512) }
        val materialized = CollageMaterializer(context, app.container.projects, app.container.frameEdits).materialize(loaded)
        val usable = loaded.indices.filter { materialized[it] != null }
        if (usable.size != loaded.size) {
            val thumbs = previews
            frames = usable.map { loaded[it] }
            previews = usable.map { thumbs.getOrNull(it) }
            order = frames.indices.toList()
            crops = frames.map { CellCrop() }
            selectedCell = null
            if (!userPicked) template = CollageTemplates.suggest(frames.size)
        }
        paths = usable.map { materialized[it] }
        ready = true
        previews = paths.mapIndexed { i, p -> loadPreview(p) ?: previews.getOrNull(i) }
        if (frames.size != ids.size) {
            messageIsError = true
            message = com.frameout.core.util.CollageStrings.skipped(ids.size - frames.size, ids.size)
        }
    }
}

private data class CollageOptions(val gap: Int, val bgColor: Int, val rounded: Boolean, val stamps: Boolean)

suspend fun CollageState.persistOptions(prefs: AppPrefs) {
    snapshotFlow { CollageOptions(gap, bgColor, rounded, stamps) }.collect {
        prefs.setCollageGap(it.gap)
        prefs.setCollageBgColor(it.bgColor)
        prefs.setCollageRounded(it.rounded)
        prefs.setCollageStamps(it.stamps)
    }
}
