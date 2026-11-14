package com.frameout.app.viewer

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.frameout.core.edit.EditMark
import com.frameout.core.edit.Transform
import com.frameout.core.edit.ViewGeometry
import com.frameout.core.util.EditStrings
import com.frameout.designsystem.theme.FrameTheme

internal class OverlayInput(
    val geo: ViewGeometry,
    val bitmap: Bitmap,
    val tool: EditTool,
    val draft: EditDraft,
    val display: EditDraft,
    val transformNow: () -> Transform,
    val onMark: (EditMark) -> Unit,
    val tools: ViewerToolEditor,
    val onTransform: (Transform) -> Unit
)

@Composable
internal fun MarkOverlay(input: OverlayInput, transform: Transform, modifier: Modifier = Modifier) {
    val latest = rememberUpdatedState(input)
    val session = remember { MarkSession() }
    val label = if (input.tool == EditTool.Draw) EditStrings.drawArea() else EditStrings.coverArea()
    val density = LocalDensity.current.density
    val accent = FrameTheme.colors.primary
    Canvas(
        modifier
            .fillMaxSize()
            .semantics { contentDescription = label }
            .pointerInput(Unit) { awaitEachGesture { markGesture(latest, session, density) } }
    ) { drawSession(session, input, transform, accent) }
}

private suspend fun AwaitPointerEventScope.markGesture(latest: State<OverlayInput>, session: MarkSession, density: Float) {
    val down = awaitFirstDown(requireUnconsumed = false)
    val cover = latest.value.tool == EditTool.Cover
    val grab = if (cover) pickCoverGrab(latest.value, down.position, density) else CoverGrab.Create
    if (grab !is CoverGrab.Create) {
        coverEditGesture(latest, grab, down.position, density)
        return
    }
    val rectangle = cover && latest.value.draft.shape == CoverShape.Rect
    var multi = false
    var moved = false
    session.begin(down.position)
    do {
        val event = awaitPointerEvent()
        val pressed = event.changes.filter { it.pressed }
        if (pressed.size >= 2) {
            if (!multi) session.cancel()
            multi = true
            applyTransform(event, latest.value)
        } else if (!multi && pressed.size == 1) {
            if ((pressed[0].position - down.position).getDistance() > COVER_SLOP_DP * density) moved = true
            session.move(pressed[0].position, rectangle)
        }
        event.changes.forEach { if (it.positionChanged()) it.consume() }
    } while (event.changes.any { it.pressed })
    if (multi) return
    val input = latest.value
    if (cover && !moved) {
        val hit = hitCover(input, down.position, density)
        input.tools.covers.select(hit)
        if (hit >= 0) {
            session.cancel()
            return
        }
    }
    session.finish(buildMark(session.points, input.geo, input.transformNow(), input.tool, input.draft), input.bitmap, input.onMark)
}

private fun applyTransform(event: PointerEvent, input: OverlayInput) {
    val centroid = event.calculateCentroid(useCurrent = true)
    val pan = event.calculatePan()
    val next = input.geo.gesture(
        input.transformNow(),
        event.calculateZoom(),
        centroid.x - input.geo.viewW / 2f,
        centroid.y - input.geo.viewH / 2f,
        pan.x,
        pan.y
    )
    input.onTransform(next)
}

private fun DrawScope.drawSession(session: MarkSession, input: OverlayInput, transform: Transform, accent: Color) {
    val geo = input.geo
    session.held?.takeIf { it.bitmap === input.bitmap }?.let { drawMarkPreview(it.mark, geo, transform) }
    val live = buildMark(session.points, geo, transform, input.tool, input.draft)
    live?.let { drawMarkPreview(it, geo, transform) }
    if (input.tool == EditTool.Cover && session.points.isEmpty()) drawCoverSelection(input, transform, accent)
    val cursor = session.cursor ?: return
    val pen = input.tool == EditTool.Draw || input.draft.shape == CoverShape.Pen
    if (!pen) return
    val fraction = if (input.tool == EditTool.Draw) input.draft.size else input.draft.coverSize
    drawBrushRing(cursor, brushDiameterOnScreen(fraction, geo, transform))
}
