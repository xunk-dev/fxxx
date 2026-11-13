package com.frameout.app.trim

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.frameout.app.extract.formatExtractTime
import com.frameout.core.util.ModeStrings
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.TrimStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.FrameRollingText
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.glass.frameGlow
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameCardBrush
import com.frameout.designsystem.theme.frameCardEdge
import com.frameout.designsystem.theme.frameHairline
import com.frameout.engine.extract.ExtractMode
import com.frameout.engine.extract.ExtractModeCodec

internal fun planText(mode: ExtractMode): String = when (mode) {
    ExtractMode.EveryFrame -> TrimStrings.planAll()
    is ExtractMode.EveryNFrames -> TrimStrings.planEveryN(mode.n)
    is ExtractMode.EveryNMillis -> TrimStrings.planEveryTime(timeText(mode.millis))
    is ExtractMode.FixedCount -> TrimStrings.planFixed(mode.count)
    ExtractMode.KeyframesOnly -> TrimStrings.planKeyframes()
    ExtractMode.SceneChange -> TrimStrings.planScene()
    ExtractMode.Slides -> TrimStrings.planSlides()
    is ExtractMode.Content -> contentPlanText(mode)
}

private fun contentPlanText(mode: ExtractMode.Content): String =
    if (mode.spec.tags.isEmpty()) TrimStrings.planEveryTime(timeText(com.frameout.core.quality.content.ContentSpec.SAMPLE_STEP_MS))
    else TrimStrings.planContent(com.frameout.core.quality.content.ContentCatalog.summary(mode.spec.tags, FrameStrings.lang()))

private fun timeText(millis: Long): String =
    if (millis % 1000L == 0L) formatExtractTime(millis) else "$millis ${ModeStrings.millis()}"

@Composable
internal fun PlanButton(env: TrimEnv, modifier: Modifier) {
    val stored by env.prefs.extractMode.collectAsState()
    val colors = FrameTheme.colors
    val mode = ExtractModeCodec.decode(stored)
    val frames = env.frameEstimate(stored)
    val megabytes = estimateMbFromFrames(frames)
    val shape = FrameShapes.lg
    Box(
        modifier
            .height(56.dp)
            .frameGlow(colors.accentA, { if (colors.isDark) 0.45f else 0.2f }, 8.dp, { 0.43f })
            .clip(shape)
            .background(frameCardBrush(colors.accentA, 0.12f))
            .border(1.dp, frameCardEdge(colors.accentA, 0.5f), shape)
            .frameClickable(onClickLabel = TrimStrings.planChange()) { env.onSheet(TrimSheet.Plan) }
    ) {
        PlanButtonRow(mode, frames, megabytes, estimateKindFor(env.prefs, mode))
    }
}

@Composable
private fun PlanButtonRow(mode: ExtractMode, frames: Int, megabytes: Float, kind: EstimateKind) {
    val colors = FrameTheme.colors
    Row(
        Modifier.fillMaxSize().padding(horizontal = 10.dp),
        Arrangement.spacedBy(8.dp),
        Alignment.CenterVertically
    ) {
        PlanModeTile(mode, 36.dp)
        Column(Modifier.weight(1f), Arrangement.spacedBy(0.dp, Alignment.CenterVertically)) {
            FrameText(
                planCount(frames, kind) + " " + planFramesUnit(frames),
                style = FrameTheme.typography.titleSmall.copy(textDirection = TextDirection.Content),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            FrameText(
                planMegabytes(megabytes) + " " + planSizeUnit(megabytes),
                style = FrameTheme.typography.label.copy(textDirection = TextDirection.Content),
                color = colors.primary,
                maxLines = 1
            )
        }
        Image(painterResource(FrameIconsExtra.Chevron), null, Modifier.size(18.dp), colorFilter = ColorFilter.tint(colors.primary))
    }
}

@Composable
internal fun PlanWarning(env: TrimEnv) {
    if (!env.autoSave) return
    val stored by env.prefs.extractMode.collectAsState()
    val frames = env.frameEstimate(stored)
    FrameText(TrimStrings.galleryWarn(frames), style = FrameTheme.typography.bodySmall, color = FrameTheme.colors.error, modifier = Modifier.padding(horizontal = 4.dp))
}

@Composable
internal fun PlanSummary(env: TrimEnv, tappable: Boolean) {
    val stored by env.prefs.extractMode.collectAsState()
    val colors = FrameTheme.colors
    val mode = ExtractModeCodec.decode(stored)
    val frames = env.frameEstimate(stored)
    val megabytes = estimateMbFromFrames(frames)
    val kind = estimateKindFor(env.prefs, mode)
    val click = if (tappable) Modifier.frameClickable { env.onSheet(TrimSheet.Plan) } else Modifier
    val shape = FrameShapes.lg
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(frameCardBrush(colors.accentA, 0.10f))
            .border(1.dp, frameCardEdge(colors.accentA, 0.4f), shape)
            .then(click)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        Arrangement.spacedBy(12.dp),
        Alignment.CenterVertically
    ) {
        PlanFigure(FrameIconsExtra.Frames, planCount(frames, kind), planFramesUnit(frames), Modifier.weight(1f))
        Box(Modifier.size(width = 1.dp, height = 24.dp).background(frameHairline()))
        PlanFigure(FrameIconsExtra.Storage, planMegabytes(megabytes), planSizeUnit(megabytes), Modifier.weight(1f))
        if (tappable) PlanChangeButton()
    }
}

@Composable
private fun PlanFigure(icon: Int, value: String, unit: String, modifier: Modifier) {
    val colors = FrameTheme.colors
    Row(modifier, Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), Alignment.CenterVertically) {
        Image(painterResource(icon), null, Modifier.size(16.dp), colorFilter = ColorFilter.tint(colors.primary))
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            FrameRollingText(value, FrameTheme.typography.titleSmall, colors.onSurface)
        }
        FrameText(unit, style = FrameTheme.typography.label, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun PlanModeTile(mode: ExtractMode, size: Dp) {
    val colors = FrameTheme.colors
    val shape = FrameShapes.sm
    Box(
        Modifier
            .size(size)
            .clip(shape)
            .background(Brush.linearGradient(listOf(colors.accentA.copy(alpha = 0.34f), colors.accentB.copy(alpha = 0.14f))))
            .border(1.dp, colors.accentA.copy(alpha = 0.35f), shape),
        Alignment.Center
    ) {
        PlanStrip(mode, Modifier.fillMaxSize().padding(horizontal = 5.dp, vertical = 6.dp), cells = 6, perforations = false)
    }
}

@Composable
internal fun PlanChangeButton() {
    val colors = FrameTheme.colors
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(colors.accentA.copy(alpha = 0.16f))
            .border(1.dp, colors.accentA.copy(alpha = 0.30f), CircleShape)
            .semantics { contentDescription = TrimStrings.planChange() },
        Alignment.Center
    ) {
        Image(painterResource(FrameIconsExtra.Adjust), null, Modifier.size(20.dp), colorFilter = ColorFilter.tint(colors.primary))
    }
}
