package com.frameout.app.collage

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.frameout.core.collage.CellCrop
import com.frameout.core.collage.CollageAspect
import com.frameout.core.collage.CollageBlend
import com.frameout.core.collage.CollageTemplate
import com.frameout.core.collage.HistoryStack
import com.frameout.core.db.FrameEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

private const val COMMIT_DELAY_MS = 350L

internal class CollageSnap(
    val template: CollageTemplate,
    val userPicked: Boolean,
    val aspect: CollageAspect,
    val aspectAuto: Boolean,
    val resolution: Int,
    val frames: List<FrameEntity>,
    val paths: List<String?>,
    val previews: List<Bitmap?>,
    val previewSide: Int,
    val order: List<Int>,
    val crops: List<CellCrop>,
    val galleryIds: Set<Long>,
    val gap: Int,
    val bgColor: Int,
    val rounded: Boolean,
    val stamps: Boolean,
    val blend: CollageBlend,
    val blendStrength: Int,
    val signature: List<Any?>
)

internal class CollageHistory(private val s: CollageState) {
    private var stacks by mutableStateOf(HistoryStack<CollageSnap>())
    private var committed by mutableStateOf<CollageSnap?>(null)
    private var active by mutableStateOf(false)
    private var holds by mutableIntStateOf(0)

    private val idle: Boolean get() = active && holds == 0 && !s.saving
    private val pending: Boolean get() = committed?.let { it.signature != s.signature() } == true

    val canUndo: Boolean get() = idle && (stacks.canUndo || pending)
    val canRedo: Boolean get() = idle && stacks.canRedo && !pending

    fun start() {
        stacks = HistoryStack()
        committed = snapshot()
        active = true
    }

    fun stop() {
        active = false
        stacks = HistoryStack()
        committed = null
    }

    fun hold() { holds++ }

    fun release() { holds = (holds - 1).coerceAtLeast(0) }

    suspend fun track() {
        snapshotFlow { if (active && holds == 0) s.signature() else null }.collectLatest { signature ->
            if (signature != null) {
                delay(COMMIT_DELAY_MS)
                commit()
            }
        }
    }

    private fun commit() {
        if (!active || holds > 0) return
        val base = committed ?: return
        val now = snapshot()
        if (now.signature == base.signature) return
        stacks = stacks.commit(base)
        committed = now
    }

    fun undo() {
        if (!canUndo) return
        commit()
        val current = committed ?: return
        val (target, next) = stacks.undone(current) ?: return
        stacks = next
        restore(target)
    }

    fun redo() {
        if (!canRedo) return
        val current = committed ?: return
        val (target, next) = stacks.redone(current) ?: return
        stacks = next
        restore(target)
    }

    private fun snapshot() = CollageSnap(
        s.template, s.userPicked, s.aspect, s.aspectAuto, s.resolution, s.frames, s.paths, s.previews, s.previewSide, s.order,
        s.crops, s.galleryIds, s.gap, s.bgColor, s.rounded, s.stamps, s.blend, s.blendStrength, s.signature()
    )

    private fun restore(t: CollageSnap) {
        s.frames = t.frames
        s.paths = t.paths
        s.previews = t.previews
        s.previewSide = t.previewSide
        s.order = t.order
        s.crops = t.crops
        s.galleryIds = t.galleryIds
        s.template = t.template
        s.userPicked = t.userPicked
        s.aspect = t.aspect
        s.aspectAuto = t.aspectAuto
        s.resolution = t.resolution
        s.gap = t.gap
        s.bgColor = t.bgColor
        s.rounded = t.rounded
        s.stamps = t.stamps
        s.blend = t.blend
        s.blendStrength = t.blendStrength
        s.selectedCell = null
        committed = t
    }
}
