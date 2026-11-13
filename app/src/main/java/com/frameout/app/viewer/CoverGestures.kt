package com.frameout.app.viewer

import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.runtime.State
import com.frameout.core.edit.BrushCoverMark
import com.frameout.core.edit.CoverMark
import com.frameout.core.edit.EditMark
import com.frameout.core.edit.MarkPoint
import com.frameout.core.edit.Transform
import com.frameout.core.edit.ViewGeometry
import kotlin.math.max
import kotlin.math.min

internal const val COVER_SLOP_DP = 8f
private const val COVER_BODY_TOLERANCE_DP = 8f
private const val COVER_STROKE_TOLERANCE_DP = 14f

internal sealed interface CoverGrab {
    object Create : CoverGrab
    object Delete : CoverGrab
    object Duplicate : CoverGrab
    class Move(val index: Int) : CoverGrab
    class Corner(val index: Int, val anchor: MarkPoint) : CoverGrab
}

internal fun coverNorm(p: Offset, geo: ViewGeometry, t: Transform) = MarkPoint(
    geo.toNormX(p.x - geo.viewW / 2f, t).coerceIn(0f, 1f),
    geo.toNormY(p.y - geo.viewH / 2f, t).coerceIn(0f, 1f)
)

private fun brushPoints(mark: BrushCoverMark, geo: ViewGeometry, t: Transform): List<Offset> =
    mark.points.map { shapeScreen(it.x, it.y, geo, t) }

internal fun coverScreenRect(mark: EditMark, geo: ViewGeometry, t: Transform): Rect? = when (mark) {
    is CoverMark -> {
        val a = shapeScreen(mark.left, mark.top, geo, t)
        val b = shapeScreen(mark.right, mark.bottom, geo, t)
        Rect(min(a.x, b.x), min(a.y, b.y), max(a.x, b.x), max(a.y, b.y))
    }
    is BrushCoverMark -> brushPoints(mark, geo, t).takeIf { it.isNotEmpty() }?.let { pts ->
        val half = brushDiameterOnScreen(mark.width, geo, t) / 2f
        Rect(pts.minOf { it.x } - half, pts.minOf { it.y } - half, pts.maxOf { it.x } + half, pts.maxOf { it.y } + half)
    }
    else -> null
}

private fun segmentDistance(p: Offset, a: Offset, b: Offset): Float {
    val ab = b - a
    val len2 = ab.x * ab.x + ab.y * ab.y
    if (len2 < 1e-3f) return (p - a).getDistance()
    val k = (((p.x - a.x) * ab.x + (p.y - a.y) * ab.y) / len2).coerceIn(0f, 1f)
    return (p - Offset(a.x + ab.x * k, a.y + ab.y * k)).getDistance()
}

private fun coverHit(mark: EditMark, at: Offset, geo: ViewGeometry, t: Transform, density: Float): Boolean = when (mark) {
    is CoverMark -> coverScreenRect(mark, geo, t)?.inflate(COVER_BODY_TOLERANCE_DP * density)?.contains(at) == true
    is BrushCoverMark -> {
        val pts = brushPoints(mark, geo, t)
        val reach = brushDiameterOnScreen(mark.width, geo, t) / 2f + COVER_STROKE_TOLERANCE_DP * density
        when {
            pts.isEmpty() -> false
            pts.size == 1 -> (at - pts[0]).getDistance() <= reach
            else -> pts.zipWithNext().any { (a, b) -> segmentDistance(at, a, b) <= reach }
        }
    }
    else -> false
}

internal fun hitCover(input: OverlayInput, at: Offset, density: Float): Int {
    val t = input.transformNow()
    for (i in input.display.marks.indices.reversed()) {
        val m = input.display.marks[i]
        if (m.isCover() && coverHit(m, at, input.geo, t, density)) return i
    }
    return -1
}

internal fun pickCoverGrab(input: OverlayInput, at: Offset, density: Float): CoverGrab {
    val d = input.display
    val geo = input.geo
    val t = input.transformNow()
    val sel = d.coverSelection
    val box = sel?.let { coverScreenRect(it, geo, t) }
    if (sel != null && box != null) {
        val reachAction = DELETE_RADIUS_DP * density * 1.4f
        if ((at - deleteAnchor(box.topLeft, box.bottomRight, geo, density)).getDistance() <= reachAction) return CoverGrab.Delete
        if ((at - duplicateAnchor(box.topLeft, box.bottomRight, geo, density)).getDistance() <= reachAction) return CoverGrab.Duplicate
        if (sel is CoverMark) {
            val reach = HANDLE_REACH_DP * density
            val corners = listOf(box.topLeft, box.topRight, box.bottomRight, box.bottomLeft)
            val anchors = listOf(
                MarkPoint(sel.right, sel.bottom), MarkPoint(sel.left, sel.bottom),
                MarkPoint(sel.left, sel.top), MarkPoint(sel.right, sel.top)
            )
            val nearest = corners.indices.minByOrNull { (at - corners[it]).getDistance() }
            if (nearest != null && (at - corners[nearest]).getDistance() <= reach) return CoverGrab.Corner(d.selectedCover, anchors[nearest])
        }
        if (coverHit(sel, at, geo, t, density)) return CoverGrab.Move(d.selectedCover)
    }
    if (d.shape == CoverShape.Rect) {
        for (i in d.marks.indices.reversed()) {
            val m = d.marks[i]
            if (m is CoverMark && coverHit(m, at, geo, t, density)) return CoverGrab.Move(i)
        }
    }
    return CoverGrab.Create
}

internal suspend fun AwaitPointerEventScope.coverEditGesture(
    latest: State<OverlayInput>,
    grab: CoverGrab,
    start: Offset,
    density: Float
) {
    var moved = false
    var multi = false
    var live = false
    var last = start
    if (grab is CoverGrab.Move && latest.value.display.selectedCover != grab.index) latest.value.tools.covers.select(grab.index)
    do {
        val event = awaitPointerEvent()
        val pressed = event.changes.filter { it.pressed }
        if (pressed.size >= 2) {
            multi = true
            val input = latest.value
            val centroid = event.calculateCentroid(useCurrent = true)
            val pan = event.calculatePan()
            input.onTransform(
                input.geo.gesture(
                    input.transformNow(), event.calculateZoom(),
                    centroid.x - input.geo.viewW / 2f, centroid.y - input.geo.viewH / 2f, pan.x, pan.y
                )
            )
        } else if (!multi && pressed.size == 1) {
            val p = pressed[0].position
            val input = latest.value
            if (!moved && (p - start).getDistance() > COVER_SLOP_DP * density) {
                moved = true
                last = p
                val index = when (grab) {
                    is CoverGrab.Move -> grab.index
                    is CoverGrab.Corner -> grab.index
                    else -> -1
                }
                if (index >= 0) {
                    live = true
                    input.tools.covers.begin(index)
                }
            } else if (moved) {
                when (grab) {
                    is CoverGrab.Move -> {
                        val geo = input.geo
                        val t = input.transformNow()
                        val unitX = geo.imageW * geo.pixelSize(t)
                        val unitY = geo.imageH * geo.pixelSize(t)
                        input.tools.covers.moveBy(grab.index, (p.x - last.x) / unitX, (p.y - last.y) / unitY)
                    }
                    is CoverGrab.Corner -> input.tools.covers.resize(grab.index, grab.anchor, coverNorm(p, input.geo, input.transformNow()))
                    else -> Unit
                }
                last = p
            }
        }
        event.changes.forEach { if (it.positionChanged()) it.consume() }
    } while (event.changes.any { it.pressed })
    val input = latest.value
    if (live) input.tools.covers.end()
    if (multi || moved) return
    when (grab) {
        CoverGrab.Delete -> input.tools.covers.deleteSelected()
        CoverGrab.Duplicate -> input.tools.covers.duplicateSelected()
        else -> Unit
    }
}

internal fun DrawScope.drawCoverSelection(input: OverlayInput, t: Transform, accent: Color) {
    val d = input.display
    val sel = d.coverSelection ?: return
    val geo = input.geo
    if (d.liveCover == d.selectedCover) drawMarkPreview(sel, geo, t)
    val box = coverScreenRect(sel, geo, t) ?: return
    val dash = PathEffect.dashPathEffect(floatArrayOf(10f * density, 7f * density))
    val size = Size(box.width, box.height)
    drawRect(Color.Black.copy(alpha = 0.45f), box.topLeft, size, style = Stroke(2.6f * density, pathEffect = dash))
    drawRect(Color.White.copy(alpha = 0.95f), box.topLeft, size, style = Stroke(1.4f * density, pathEffect = dash))
    if (sel is CoverMark) {
        listOf(box.topLeft, box.topRight, box.bottomRight, box.bottomLeft).forEach { p ->
            drawCircle(Color.White, HANDLE_RADIUS_DP * density, p)
            drawCircle(accent, HANDLE_RADIUS_DP * density, p, style = Stroke(2.5f * density))
        }
    }
    drawDeleteHandle(deleteAnchor(box.topLeft, box.bottomRight, geo, density), density)
    drawDuplicateHandle(duplicateAnchor(box.topLeft, box.bottomRight, geo, density), density, accent)
}
