package com.frameout.app.billing

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Shape
import com.frameout.designsystem.theme.FrameShapes
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import com.frameout.core.pro.ProTier

private const val LOCKED_ALPHA = 0.82f

@Composable
fun BoxScope.ProCorner(locked: Boolean, phase: Int = 0, shape: Shape = FrameShapes.sm) {
    TierFold(ProTier.Pro, locked, shape, phase)
}

fun Modifier.proDim(locked: Boolean): Modifier = if (locked) alpha(LOCKED_ALPHA) else this

@Composable
fun BoxScope.TierCorner(tier: ProTier, locked: Boolean, phase: Int = 0, shape: Shape = FrameShapes.sm, ring: Boolean = true) {
    TierFold(tier, locked, shape, phase, TierMarkKind.Tile, ring)
}

@Composable
fun BoxScope.TierLockCorner(tier: ProTier, locked: Boolean, shape: Shape = FrameShapes.sm) {
    TierFold(tier, locked, shape, 0, TierMarkKind.Chip)
}
