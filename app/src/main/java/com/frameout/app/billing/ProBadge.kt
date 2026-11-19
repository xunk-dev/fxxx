package com.frameout.app.billing

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import com.frameout.core.pro.ProTier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.core.pro.ProStrings
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.theme.FrameTheme

private val BadgeShape = RoundedCornerShape(9.dp)

@Composable
fun ProBadge(modifier: Modifier = Modifier, phase: Int = 0, compact: Boolean = false) {
    val shimmer = rememberShimmer(3400, (phase % 5) * 450)
    val ink = proOnBrush()
    val rim = proRim(0.22f)
    Row(
        modifier
            .height(18.dp)
            .clip(BadgeShape)
            .background(proBrush(), BadgeShape)
            .border(0.5.dp, rim, BadgeShape)
            .shimmerSweep(shimmer)
            .padding(start = if (compact) 4.dp else 5.dp, end = if (compact) 4.dp else 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(painterResource(FrameIcons.StarFilled), null, Modifier.size(10.dp), colorFilter = ColorFilter.tint(ink))
        if (!compact) {
            FrameText(
                ProStrings.badge(),
                style = FrameTheme.typography.label.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
                color = ink,
                modifier = Modifier.padding(start = 3.dp),
                maxLines = 1
            )
        }
    }
}

internal val AdBrush = Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF06B6D4)))
internal val AdInk = Color(0xFF06281F)

@Composable
internal fun PlayGlyph(size: androidx.compose.ui.unit.Dp, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val path = Path().apply {
            moveTo(w * 0.28f, h * 0.18f)
            lineTo(w * 0.84f, h * 0.5f)
            lineTo(w * 0.28f, h * 0.82f)
            close()
        }
        drawPath(path, tint)
    }
}

@Composable
fun AdBadge(modifier: Modifier = Modifier, compact: Boolean = false) {
    Row(
        modifier
            .height(18.dp)
            .clip(BadgeShape)
            .background(AdBrush, BadgeShape)
            .border(0.5.dp, Color.White.copy(alpha = 0.22f), BadgeShape)
            .padding(start = if (compact) 4.dp else 5.dp, end = if (compact) 4.dp else 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlayGlyph(10.dp, AdInk)
        if (!compact) {
            FrameText(
                ProStrings.adBadge(),
                style = FrameTheme.typography.label.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
                color = AdInk,
                modifier = Modifier.padding(start = 3.dp),
                maxLines = 1
            )
        }
    }
}

@Composable
fun TierBadge(tier: ProTier, modifier: Modifier = Modifier, phase: Int = 0, compact: Boolean = false) {
    when (tier) {
        ProTier.Free -> Unit
        ProTier.Ad -> AdBadge(modifier, compact)
        ProTier.Pro -> ProBadge(modifier, phase, compact)
    }
}

@Composable
fun TierPip(tier: ProTier, modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 14.dp) {
    if (tier == ProTier.Free) return
    val brush = if (tier == ProTier.Ad) AdBrush else proBrush()
    val ink = if (tier == ProTier.Ad) AdInk else proOnBrush()
    val glyph = size * 0.57f
    Box(
        modifier.size(size).clip(RoundedCornerShape(50)).background(brush, RoundedCornerShape(50)).border(0.5.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(50)),
        Alignment.Center
    ) {
        if (tier == ProTier.Ad) PlayGlyph(glyph, ink)
        else Image(painterResource(FrameIcons.StarFilled), null, Modifier.size(glyph), colorFilter = ColorFilter.tint(ink))
    }
}

private val TagShape = RoundedCornerShape(7.dp)

@Composable
fun TierTag(tier: ProTier, modifier: Modifier = Modifier) {
    if (tier == ProTier.Free) return
    val ad = tier == ProTier.Ad
    val tint = if (ad) tierA(tier) else FrameTheme.colors.primary
    Row(
        modifier.height(20.dp).clip(TagShape).border(1.dp, tint.copy(alpha = 0.55f), TagShape).padding(horizontal = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (ad) PlayGlyph(10.dp, tint)
        else Image(painterResource(FrameIcons.StarFilled), null, Modifier.size(10.dp), colorFilter = ColorFilter.tint(tint))
        FrameText(
            if (ad) ProStrings.adBadge() else ProStrings.badge(),
            style = FrameTheme.typography.label.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
            color = tint,
            modifier = Modifier.padding(start = 3.dp),
            maxLines = 1
        )
    }
}

@Composable
fun TierStar(tier: ProTier, modifier: Modifier = Modifier) {
    if (tier == ProTier.Free) return
    Image(painterResource(FrameIcons.StarFilled), null, modifier.size(12.dp), colorFilter = ColorFilter.tint(if (tier == ProTier.Ad) tierA(tier) else FrameTheme.colors.primary))
}
