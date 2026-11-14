package com.frameout.app.billing

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.frameout.core.pro.ProTier
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme

enum class TierMarkKind { Strip, Tile, Chip }

private val AdA = Color(0xFF10B981)
private val AdB = Color(0xFF06B6D4)
private val StripWidth = 3.dp
private val RingWidth = 1.5.dp
private const val WashAlpha = 0.12f
private const val TileWashAlpha = 0.08f

@Composable
internal fun tierA(tier: ProTier): Color {
    val theme = FrameTheme.colors
    return if (tier == ProTier.Ad) AdA else if (theme.neutral) ProGold.Light else theme.accentA
}

@Composable
internal fun tierB(tier: ProTier): Color {
    val theme = FrameTheme.colors
    return if (tier == ProTier.Ad) AdB else if (theme.neutral) ProGold.Deep else theme.accentB
}

@Composable
fun BoxScope.TierFold(
    tier: ProTier,
    locked: Boolean = true,
    shape: Shape = FrameShapes.sm,
    @Suppress("UNUSED_PARAMETER") phase: Int = 0,
    kind: TierMarkKind = TierMarkKind.Tile,
    ring: Boolean = true
) {
    if (!locked || tier == ProTier.Free) return
    when (kind) {
        TierMarkKind.Strip -> androidx.compose.foundation.layout.Box(Modifier.matchParentSize().tierStrip(tier, true, shape))
        TierMarkKind.Tile -> {
            if (ring) androidx.compose.foundation.layout.Box(Modifier.matchParentSize().tierRing(tier, shape))
            TierPip(tier, Modifier.align(Alignment.TopEnd).padding(4.dp), 14.dp)
        }
        TierMarkKind.Chip -> TierPip(tier, Modifier.align(Alignment.TopEnd).padding(2.dp), 12.dp)
    }
}

@Composable
fun Modifier.tierStrip(tier: ProTier, locked: Boolean = true, shape: Shape = RectangleShape): Modifier {
    if (!locked || tier == ProTier.Free) return this
    val a = tierA(tier)
    val b = tierB(tier)
    return drawWithContent {
        drawContent()
        val rtl = layoutDirection == LayoutDirection.Rtl
        val w = size.width
        val clip = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawWithContent)) }
        clipPath(clip) {
            val dir = if (rtl) -1f else 1f
            val x0 = if (rtl) w else 0f
            drawRect(Brush.horizontalGradient(listOf(a.copy(alpha = WashAlpha), Color.Transparent), startX = x0, endX = x0 + dir * w * 0.7f))
            val bar = StripWidth.toPx()
            drawRect(
                Brush.verticalGradient(listOf(a, b)),
                topLeft = Offset(if (rtl) w - bar else 0f, 0f),
                size = Size(bar, size.height)
            )
        }
    }
}

@Composable
private fun Modifier.tierRing(tier: ProTier, shape: Shape): Modifier {
    val a = tierA(tier)
    val b = tierB(tier)
    return drawWithContent {
        drawContent()
        val outline = shape.createOutline(size, layoutDirection, this)
        val clip = Path().apply { addOutline(outline) }
        clipPath(clip) { drawRect(a.copy(alpha = TileWashAlpha)) }
        val sw = RingWidth.toPx()
        inset(sw / 2f) {
            val inner = shape.createOutline(this.size, layoutDirection, this)
            drawOutline(inner, Brush.linearGradient(listOf(a, b), Offset.Zero, Offset(this.size.width, this.size.height)), style = Stroke(sw))
        }
    }
}

internal val LocalInProGroup = compositionLocalOf { false }

@Composable
fun ProGroup(locked: Boolean, title: String? = null, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    if (!locked) {
        Column(modifier, content = content)
        return
    }
    val a = tierA(ProTier.Pro)
    val shape = FrameShapes.md
    Column(
        modifier
            .padding(8.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(a.copy(alpha = 0.07f))
            .border(1.dp, a.copy(alpha = 0.40f), shape)
    ) {
        Row(
            Modifier.padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 4.dp),
            Arrangement.spacedBy(8.dp),
            Alignment.CenterVertically
        ) {
            if (title != null) FrameText(title, style = FrameTheme.typography.label, color = FrameTheme.colors.primary, maxLines = 1)
            ProBadge()
        }
        CompositionLocalProvider(LocalInProGroup provides true) { content() }
    }
}
