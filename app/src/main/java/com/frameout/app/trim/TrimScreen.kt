package com.frameout.app.trim

import com.frameout.designsystem.theme.frameAmbientGlow
import com.frameout.designsystem.theme.frameRailPadding
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.frameout.app.FrameOutApp
import com.frameout.app.billing.rememberProGate
import com.frameout.core.pro.ProFeature
import com.frameout.app.PreferredFrameTheme
import com.frameout.app.VolumeKeyNavigation
import com.frameout.core.storage.FrameCaptureSaver
import com.frameout.designsystem.theme.FrameTheme
import androidx.compose.runtime.produceState
import com.frameout.core.media.MotionPhotoExtractor
import com.frameout.engine.player.PlayerController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.frameout.engine.timeline.TrimRange

@Composable
fun TrimScreen(
    uri: Uri,
    onBack: () -> Unit,
    onExtract: (Uri, TrimRange, Boolean) -> Unit,
    batchEdit: Boolean = false,
    initialRange: TrimRange? = null
) {
    val context = LocalContext.current
    val controller = remember { PlayerController(context) }
    val playerState by controller.state.collectAsState()
    val prefs = remember { (context.applicationContext as FrameOutApp).container.prefs }
    val ui = remember {
        val first = TrimOutput.Frames
        TrimUiState(prefs.autoSave.value, first).also { state -> if (initialRange != null && !initialRange.useFull) state.range = TrimLimits.fit(initialRange, first.maxMs) }
    }
    val shots = remember { ShotsState() }
    val back = rememberGuardedBack(onBack, ui.busy, ui::holdBack)
    val saver = remember { FrameCaptureSaver(context, prefs, (context.applicationContext as FrameOutApp).container.savedImages) }
    val savers = remember { OutputSavers(context) }
    val gate = rememberProGate()
    val scope = rememberCoroutineScope()
    val fx = remember { SnapshotFxState() }
    val source by produceState<Uri?>(null, uri) {
        value = withContext(Dispatchers.IO) { runCatching { MotionPhotoExtractor.resolveVideoSource(context, uri) }.getOrDefault(uri) }
    }
    var repaired by remember(uri) { mutableStateOf<Uri?>(null) }
    LaunchedEffect(controller) { controller.onSourceRepaired = { repaired = it } }
    val working = repaired ?: uri
    val thumbs = rememberTimelineThumbs(context, repaired ?: source, playerState.durationMs)
    TrimPlayerEffects(uri, source, controller, playerState, prefs)
    TrimWindowEffect(controller, ui.range, playerState.durationMs)
    LoopPreviewEffect(controller, ui.output.loopsSilently, ui.range, playerState.durationMs)
    VolumeKeyNavigation { forward -> controller.pause(); controller.stepFrame(forward) }
    LaunchedEffect(playerState.durationMs) { if (playerState.durationMs > 0 && ui.range.endMs == 0L) ui.showFull(playerState.durationMs) }
    val env = TrimEnv(
        uri, controller, playerState, prefs, shots, saver, scope, ui.range, ui.output, ui.autoSave, ui.busy,
        onRange = ui::change,
        onOutput = { next -> ui.selectOutput(next, controller.positionNow()) },
        onSheet = { ui.sheet = it },
        onAutoSave = { ui.toggleAutoSave(prefs) },
        onStatus = { ui.status = it },
        source = source ?: uri,
        thumbs = thumbs,
        batchEdit = batchEdit,
        fx = fx,
        onPrimary = {
            val range = ui.range.clamped(playerState.durationMs).let { it.copy(useFull = it.startMs <= 0L && it.endMs >= playerState.durationMs) }
            if (batchEdit || ui.output == TrimOutput.Frames) onExtract(working, range, ui.autoSave)
            else gate.use(ProFeature.AdvancedExport, ui.output.tier, autoRun = true) { startOutputSave(ui, scope, savers, working, range, controller.positionNow(), playerState.durationMs) }
        }
    )
    PreferredFrameTheme { TrimLayout(env, ui, back) }
}

@Composable
private fun TrimLayout(env: TrimEnv, ui: TrimUiState, onBack: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().background(FrameTheme.colors.background).frameAmbientGlow()) {
        TrimBody(env, onBack, maxWidth > maxHeight, maxHeight)
        PlanSheet(env, ui.sheet == TrimSheet.Plan)
        OutputSheet(env, ui.sheet == TrimSheet.Output)
        CaptureStatus(ui.status, Modifier.align(Alignment.TopCenter)) { ui.status = null }
    }
}

@Composable
private fun TrimBody(env: TrimEnv, onBack: () -> Unit, landscape: Boolean, maxHeight: Dp) {
    Column(Modifier.fillMaxSize().imePadding().frameRailPadding()) {
        TrimHeader(
            env.controller, env.player.durationMs, outputIcon(env.output), env.output != TrimOutput.Frames, !env.batchEdit,
            onBack, onOutput = { env.onSheet(TrimSheet.Output) }
        )
        HdrNotice(env.uri, env.prefs)
        if (landscape) {
            Row(Modifier.weight(1f).fillMaxWidth()) {
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    PlayerArea(env.controller, env.player.videoAspect, Modifier.weight(1f).fillMaxWidth(), env.fx)
                    ShotStrip(env)
                }
                TrimPanel(env, maxHeight, Modifier.weight(1f).fillMaxHeight().padding(end = 12.dp, bottom = 12.dp), landscape = true)
            }
        } else {
            TrimStage(env, Modifier.weight(1f).fillMaxWidth())
            ShotStrip(env)
            TrimPanel(env, panelCap(env, maxHeight), Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun panelCap(env: TrimEnv, maxHeight: Dp): Dp {
    val navigationBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val warning = if (env.output == TrimOutput.Frames && env.autoSave) PANEL_WARNING else 0.dp
    val floor = PANEL_FIXED + PANEL_RANGE_EDITOR + warning + navigationBar
    return maxOf(maxHeight * PANEL_SHARE, floor)
}

private const val PANEL_SHARE = 0.54f

private val PANEL_FIXED = 300.dp

private val PANEL_RANGE_EDITOR = 96.dp

private val PANEL_WARNING = 44.dp
