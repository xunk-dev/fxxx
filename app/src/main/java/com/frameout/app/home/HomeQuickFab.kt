package com.frameout.app.home

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.frameout.app.library.FabPhase
import com.frameout.app.library.percentOf
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.HomeStrings
import com.frameout.designsystem.dock.ChargeState
import com.frameout.designsystem.dock.FrameChargeFab
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameTheme

private class QuickFabLook(val charge: ChargeState, val icon: Int, val label: String)

private fun quickLook(strip: StripModel?): QuickFabLook = when (strip?.fab?.phase) {
    null, FabPhase.Add -> QuickFabLook(ChargeState.Idle, FrameIconsExtra.Bolt, HomeStrings.quickTitle())
    FabPhase.Pause -> QuickFabLook(ChargeState.Running, FrameIconsExtra.Bolt, FrameStrings.extracting())
    FabPhase.Resume -> QuickFabLook(ChargeState.Paused, FrameIconsExtra.Bolt, FrameStrings.paused())
    FabPhase.Success -> QuickFabLook(ChargeState.Success, FrameIcons.Check, FrameStrings.completed())
}

@Composable
internal fun HomeQuickFab(strip: StripModel?, onStart: () -> Unit, onOpen: (Long) -> Unit) {
    val look = quickLook(strip)
    val idle = look.charge == ChargeState.Idle
    Crossfade(targetState = idle, label = "quickFab") { isIdle ->
        if (isIdle) {
            QuickGlassFab(look.label, onStart)
        } else {
            val progress = strip?.fab?.progress
            FrameChargeFab(
                state = look.charge,
                icon = look.icon,
                contentDescription = look.label,
                onClick = { strip?.let { onOpen(it.projectId) } },
                progress = progress,
                percentText = progress?.let { "${percentOf(it)}%" }
            )
        }
    }
}

@Composable
private fun QuickGlassFab(description: String, onClick: () -> Unit) {
    val tone = FrameTheme.colors.accentA
    val top = lerp(tone, Color.White, 0.10f)
    val bottom = lerp(tone, Color.Black, 0.16f)
    val ink = boltInk(tone)
    Box(
        Modifier
            .size(60.dp)
            .homePressable(onClick)
            .drawBehind {
                val center = Offset(size.width / 2f, size.height * 0.68f)
                val radius = size.width * 0.95f
                drawCircle(Brush.radialGradient(listOf(tone.copy(alpha = 0.50f), Color.Transparent), center, radius), radius, center)
            }
            .clip(CircleShape)
            .background(Brush.verticalGradient(listOf(top, bottom)))
            .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.42f), Color.White.copy(alpha = 0.06f))), CircleShape)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        FlatBolt(Modifier.size(28.dp), ink)
    }
}

private fun boltInk(tone: Color): Color =
    if (tone.luminance() > 0.30f) lerp(tone, Color.Black, 0.82f) else lerp(tone, Color.White, 0.94f)

@Composable
private fun FlatBolt(modifier: Modifier, ink: Color) {
    val path = remember { PathParser().parsePathString("M13.4 2.2 L5 13.6 H10.5 L9.4 21.8 L19 10 H13.6 Z").toPath() }
    Canvas(modifier) {
        scale(size.width / 24f, Offset.Zero) {
            drawPath(path, ink)
            drawPath(path, ink, style = Stroke(1.4f, join = StrokeJoin.Round))
        }
    }
}
