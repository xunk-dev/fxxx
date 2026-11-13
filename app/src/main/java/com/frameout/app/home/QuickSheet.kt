package com.frameout.app.home

import android.net.Uri
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.produceState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.frameout.app.FrameOutApp
import com.frameout.app.PreferredFrameTheme
import com.frameout.app.trim.CaptureStatus
import com.frameout.app.trim.CaptureStatusState
import com.frameout.app.trim.FrameScrubBadge
import com.frameout.app.trim.FrameScrubHint
import com.frameout.app.trim.LoopPreviewEffect
import com.frameout.app.trim.PlayerSurface
import com.frameout.app.trim.RangeEditor
import com.frameout.app.trim.RangeEditorSpec
import com.frameout.app.trim.SnapshotFxState
import com.frameout.app.trim.SnapshotOverlay
import com.frameout.app.trim.TimelineBar
import com.frameout.app.trim.TrimLimits
import com.frameout.app.trim.TrimSheetHost
import com.frameout.app.trim.TrimWindowEffect
import com.frameout.app.trim.rememberScrubHint
import com.frameout.app.trim.rememberTimelineThumbs
import com.frameout.core.media.MotionPhotoExtractor
import com.frameout.core.util.HomeStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.theme.FrameGradients
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.fieldFill
import com.frameout.designsystem.theme.frameHairline
import com.frameout.engine.player.PlayerController
import com.frameout.engine.timeline.TrimRange
import com.frameout.engine.timeline.formatTimestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val IDLE_POLL_MS = 200L
private val PreviewMaxHeight = 300.dp
private val CompactPreviewMaxHeight = 190.dp

@Composable
internal fun QuickSheet(
    uri: Uri,
    onClose: () -> Unit,
    onStart: (Uri, TrimRange, Long, Long) -> Unit,
    title: String = HomeStrings.quickTitle(),
    confirmLabel: String = HomeStrings.quickStart(),
    hint: String = HomeStrings.quickHint(),
    maxMs: Long = 0L,
    progress: Int? = null,
    latchStart: Boolean = true,
    snapshot: Boolean = false,
    compact: Boolean = false,
    loop: Boolean = false
) {
    val context = LocalContext.current
    val controller = remember(uri) { PlayerController(context) }
    val player by controller.state.collectAsState()
    val position by controller.position.collectAsState()
    val source by produceState<Uri?>(null, uri) {
        value = withContext(Dispatchers.IO) { runCatching { MotionPhotoExtractor.resolveVideoSource(context, uri) }.getOrDefault(uri) }
    }
    val thumbs = rememberTimelineThumbs(context, source, player.durationMs)
    var range by remember(uri) { mutableStateOf(TrimRange()) }
    val duration = player.durationMs
    LaunchedEffect(duration) { if (duration > 0L && range.endMs == 0L) range = TrimLimits.fit(TrimRange.full(duration), maxMs) }
    val bounded by remember(duration) { derivedStateOf { range.clamped(duration) } }
    var starting by remember(uri) { mutableStateOf(false) }
    val fx = remember(uri) { SnapshotFxState() }
    var status by remember(uri) { mutableStateOf<CaptureStatusState?>(null) }
    val prefs = remember { (context.applicationContext as FrameOutApp).container.prefs }
    val scrub = rememberScrubHint(prefs)
    val onRange: (TrimRange) -> Unit = { range = TrimLimits.enforce(range, it, maxMs) }

    LaunchedEffect(source) { source?.let(controller::setUri) }
    LaunchedEffect(controller) {
        while (true) {
            controller.poll()
            if (controller.playing) withFrameNanos { } else delay(IDLE_POLL_MS)
        }
    }
    TrimWindowEffect(controller, range, duration)
    LoopPreviewEffect(controller, loop, range, duration)
    val owner = LocalActivity.current as? LifecycleOwner
    DisposableEffect(owner, controller) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) controller.pause() }
        owner?.lifecycle?.addObserver(observer)
        onDispose { owner?.lifecycle?.removeObserver(observer) }
    }
    DisposableEffect(controller) { onDispose { controller.release() } }

    PreferredFrameTheme {
        Box(Modifier.fillMaxSize()) {
            TrimSheetHost(true, title, onClose) {
                Box(Modifier.fillMaxWidth().heightIn(max = if (compact) CompactPreviewMaxHeight else PreviewMaxHeight).clip(FrameShapes.md).background(FrameTheme.colors.container)) {
                    PlayerSurface(controller, aspect = player.videoAspect, modifier = Modifier.fillMaxWidth(), showMute = false, frameScrub = scrub) {
                        FrameScrubBadge(scrub, controller)
                        FrameScrubHint(scrub, atTop = snapshot)
                    }
                    if (snapshot) {
                        SnapshotOverlay(fx)
                        QuickSnapshotButton(controller, uri, fx, { status = it }, Modifier.align(Alignment.BottomCenter).padding(10.dp))
                    }
                }
                TimelineBar(
                    duration, position, bounded, true, controller::seekTo, onRange, thumbs,
                    modifier = Modifier.fillMaxWidth(), onScrub = controller::scrub, slideWindow = maxMs > 0L,
                    onDragSeek = controller::scrubTo
                )
                if (!compact) {
                    RangeEditor(RangeEditorSpec(duration, bounded, maxMs, onRange, controller::seekTo, controller::positionNow))
                    Ltr { Stat(HomeStrings.quickLength(), formatTimestamp(bounded.spanMs()), Modifier.fillMaxWidth()) }
                    FrameText(hint, style = FrameTheme.typography.bodySmall, color = FrameTheme.colors.onSurfaceVariant)
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(FrameShapes.pill)
                        .background(FrameGradients.Brand)
                        .alpha(if (starting || progress != null) 0.6f else 1f)
                        .frameClickable(enabled = duration > 0L && !starting && progress == null) {
                            if (latchStart) starting = true
                            controller.pause()
                            val final = bounded.copy(useFull = bounded.startMs <= 0L && bounded.endMs >= duration)
                            onStart(uri, final, controller.positionNow(), duration)
                        },
                    Alignment.Center
                ) {
                    FrameText(if (progress != null) HomeStrings.exportSaving(progress) else if (starting) HomeStrings.quickStarted() else confirmLabel, style = FrameTheme.typography.titleSmall, color = androidx.compose.ui.graphics.Color.White)
                }
            }
            CaptureStatus(status, Modifier.align(Alignment.TopCenter)) { status = null }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    val colors = FrameTheme.colors
    Column(
        modifier.clip(FrameShapes.md).background(colors.fieldFill).border(1.dp, frameHairline(), FrameShapes.md).padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        FrameText(label, style = FrameTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 1)
        FrameText(value, style = FrameTheme.typography.monoSmall, color = colors.onSurface, maxLines = 1)
    }
}
