package com.frameout.app.trim

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.frameout.app.FrameOutApp
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.engine.player.PlayerController
import java.util.Locale
import kotlin.math.abs

@Composable
internal fun MuteKey(muted: Boolean, controller: PlayerController, modifier: Modifier = Modifier) {
    val colors = FrameTheme.colors
    val prefs = (LocalContext.current.applicationContext as FrameOutApp).container.prefs
    val label = if (muted) FrameStrings.unmute() else FrameStrings.mute()
    Box(
        modifier
            .size(width = 44.dp, height = 40.dp)
            .clip(FrameShapes.pill)
            .frameClickable(onClick = {
                prefs.setPlayerMuted(!muted)
                controller.setMuted(!muted)
            })
            .semantics { contentDescription = label },
        Alignment.Center
    ) {
        Box(
            Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (muted) colors.primary.copy(alpha = 0.18f) else Color.Transparent),
            Alignment.Center
        ) {
            Image(
                painterResource(if (muted) FrameIconsExtra.VolumeOff else FrameIconsExtra.Volume),
                null,
                Modifier.size(20.dp),
                colorFilter = ColorFilter.tint(if (muted) colors.primary else colors.onSurfaceVariant)
            )
        }
    }
}

internal const val SURFACE_RESET_MS = 280

internal fun surfaceWindowShowsHdr(activity: Activity?): Boolean =
    Build.VERSION.SDK_INT >= 26 && activity?.window?.colorMode == ActivityInfo.COLOR_MODE_HDR

internal suspend fun androidx.compose.ui.input.pointer.AwaitPointerEventScope.pinchZoomGesture(
    current: () -> ZoomState,
    size: () -> Size,
    apply: (ZoomState) -> Unit
) {
    awaitFirstDown(requireUnconsumed = false)
    val slop = viewConfiguration.touchSlop
    var zoomMotion = 0f
    var panMotion = 0f
    var engaged = false
    do {
        val event = awaitPointerEvent()
        if (event.changes.any { it.isConsumed }) return
        val fingers = event.changes.count { it.pressed }
        if (fingers >= 2 || current().zoomed) {
            val zoomChange = event.calculateZoom()
            val pan = event.calculatePan()
            if (!engaged) {
                zoomMotion += abs(1f - zoomChange) * event.calculateCentroidSize(useCurrent = false)
                panMotion += pan.getDistance()
                if (fingers >= 2 && zoomMotion > slop || panMotion > slop) engaged = true
            }
            if (engaged) {
                if (zoomChange != 1f || pan != Offset.Zero) {
                    apply(current().transform(event.calculateCentroid(useCurrent = false), pan, zoomChange, size()))
                }
                event.changes.forEach { if (it.positionChanged()) it.consume() }
            }
        } else {
            engaged = false
            zoomMotion = 0f
            panMotion = 0f
        }
    } while (event.changes.any { it.pressed })
}

@Composable
internal fun SurfaceZoomBadge(scale: Float, onReset: () -> Unit) {
    val label = String.format(Locale.US, "%.1f\u00d7", scale)
    val description = FrameStrings.zoomReset(label)
    val shape = FrameShapes.pill
    Row(
        Modifier
            .padding(10.dp)
            .heightIn(min = 36.dp)
            .clip(shape)
            .background(Color(0xB8101216), shape)
            .border(1.dp, Color.White.copy(alpha = 0.16f), shape)
            .frameClickable(onClickLabel = description, onClick = onReset)
            .semantics { contentDescription = description }
            .padding(start = 10.dp, end = 4.dp),
        Arrangement.spacedBy(6.dp),
        Alignment.CenterVertically
    ) {
        Image(painterResource(FrameIconsExtra.Zoom), null, Modifier.size(16.dp), colorFilter = ColorFilter.tint(Color.White.copy(alpha = 0.9f)))
        FrameText(label, style = FrameTheme.typography.label, color = Color.White)
        Box(Modifier.size(28.dp).background(Color.White.copy(alpha = 0.14f), CircleShape), Alignment.Center) {
            Image(painterResource(FrameIconsExtra.Fit), null, Modifier.size(15.dp), colorFilter = ColorFilter.tint(Color.White))
        }
    }
}
