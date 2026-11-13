package com.frameout.app.trim

import android.os.Build
import android.view.LayoutInflater
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.frameout.app.FrameOutApp
import com.frameout.app.R
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.engine.player.PlayerController
import com.frameout.engine.player.PlayerUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
internal fun PlayerSurface(
    controller: PlayerController,
    aspect: Float = 0f,
    modifier: Modifier = Modifier.fillMaxWidth(),
    onTap: (() -> Unit)? = null,
    showMute: Boolean = true,
    frameScrub: FrameScrubState? = null,
    overlay: @Composable BoxScope.() -> Unit = {}
) {
    var zoom by remember { mutableStateOf(ZoomState()) }
    var box by remember { mutableStateOf(IntSize.Zero) }
    val scope = rememberCoroutineScope()
    var resetJob by remember { mutableStateOf<Job?>(null) }
    val animateReset: () -> Unit = {
        resetJob?.cancel()
        val from = zoom
        resetJob = scope.launch {
            animate(0f, 1f, animationSpec = tween(SURFACE_RESET_MS, easing = FastOutSlowInEasing)) { t, _ -> zoom = from.relaxed(t) }
        }
    }
    val ui by controller.state.collectAsState()
    val clipFrame by controller.frame.collectAsState()
    val still by controller.still.collectAsState()
    val hdrSource by controller.hdrSource.collectAsState()
    val activity = LocalActivity.current
    val hdrContext = LocalContext.current
    val displayHdr = remember(hdrContext) { com.frameout.engine.player.HdrDisplay.supported(hdrContext) }
    val coverage = hdrContext.resources.displayMetrics.let {
        com.frameout.engine.player.HdrDisplayPolicy.coverage(box.width.toLong() * box.height, it.widthPixels.toLong() * it.heightPixels)
    }
    val nativeHdr = hdrSource && com.frameout.engine.player.HdrDisplayPolicy.useNativeSurface(Build.VERSION.SDK_INT, displayHdr, coverage)
    DisposableEffect(nativeHdr, activity) {
        val window = activity?.window
        val cap = com.frameout.engine.player.HdrDisplayPolicy.headroomCap(Build.VERSION.SDK_INT)
        if (nativeHdr && window != null && cap != null && Build.VERSION.SDK_INT >= 35) {
            val previous = window.desiredHdrHeadroom
            window.desiredHdrHeadroom = cap
            onDispose { window.desiredHdrHeadroom = previous }
        } else {
            onDispose { }
        }
    }
    LaunchedEffect(nativeHdr) { if (nativeHdr) { resetJob?.cancel(); zoom = ZoomState() } }
    LaunchedEffect(controller, activity) { controller.setWindowShowsHdr(surfaceWindowShowsHdr(activity)) }
    val appPrefs = (LocalContext.current.applicationContext as FrameOutApp).container.prefs
    LaunchedEffect(controller) { controller.setMuted(appPrefs.playerMuted.value) }
    val ratio = if (aspect > 0f) aspect.coerceIn(0.4f, 3f) else 16f / 9f
    Box(modifier, Alignment.Center) {
        Box(
            Modifier
                .aspectRatio(ratio)
                .clip(FrameShapes.md)
                .background(Color.Black)
                .onSizeChanged { box = it }
                .pointerInput(Unit) { detectTapGestures(onTap = { onTap?.invoke() }) }
                .let {
                    if (nativeHdr) it else it.pointerInput(Unit) {
                        awaitEachGesture {
                            pinchZoomGesture({ zoom }, { Size(box.width.toFloat(), box.height.toFloat()) }) {
                                resetJob?.cancel()
                                zoom = it
                            }
                        }
                    }
                }
                .let { if (frameScrub != null) it.frameScrubDrag(controller, frameScrub) { zoom.zoomed && !nativeHdr } else it }
        ) {
            PlayerLayers(controller, ui, clipFrame, still, if (nativeHdr) ZoomState() else zoom, nativeHdr, showMute, animateReset)
            overlay()
        }
    }
}

@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
@Composable
private fun BoxScope.PlayerLayers(controller: PlayerController, ui: PlayerUiState, clipFrame: android.graphics.Bitmap?, still: android.graphics.Bitmap?, zoom: ZoomState, nativeHdr: Boolean, showMute: Boolean, onResetZoom: () -> Unit) {
    val zoomLayer: androidx.compose.ui.graphics.GraphicsLayerScope.() -> Unit = {
        scaleX = zoom.scale
        scaleY = zoom.scale
        translationX = zoom.offset.x
        translationY = zoom.offset.y
    }
    if (ui.animated) {
        clipFrame?.let { bitmap ->
            val image = remember(bitmap) { bitmap.asImageBitmap() }
            Image(image, contentDescription = null, modifier = Modifier.fillMaxSize().graphicsLayer(block = zoomLayer), contentScale = ContentScale.Fit)
        }
    } else {
        key(nativeHdr) {
            AndroidView(
                factory = { ctx ->
                    val layout = if (nativeHdr) R.layout.player_surface else R.layout.player_texture
                    (LayoutInflater.from(ctx).inflate(layout, null) as PlayerView).apply {
                        player = controller.attach()
                        setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    }
                },
                modifier = Modifier.fillMaxSize().graphicsLayer(block = zoomLayer)
            )
        }
    }
    if (!ui.animated) {
        still?.let { bitmap ->
            val image = remember(bitmap) { bitmap.asImageBitmap() }
            Image(image, contentDescription = null, modifier = Modifier.fillMaxSize().graphicsLayer(block = zoomLayer), contentScale = ContentScale.Fit)
        }
    }
    if (ui.buffering && !ui.animated) com.frameout.designsystem.components.FrameSpinner(Modifier.align(Alignment.Center), size = 40.dp)
    val shownScale = remember { floatArrayOf(ZoomState.MIN_SCALE) }
    if (zoom.zoomed) shownScale[0] = zoom.scale
    AnimatedVisibility(
        visible = zoom.zoomed,
        modifier = Modifier.align(Alignment.TopEnd),
        enter = fadeIn(tween(160)) + scaleIn(tween(200, easing = FastOutSlowInEasing), initialScale = 0.85f, transformOrigin = TransformOrigin(1f, 0f)),
        exit = fadeOut(tween(140)) + scaleOut(tween(160), targetScale = 0.9f, transformOrigin = TransformOrigin(1f, 0f))
    ) { SurfaceZoomBadge(shownScale[0], onResetZoom) }
}
