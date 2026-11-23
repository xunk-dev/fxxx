package com.frameout.app.billing

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.core.pro.ProStrings
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.fieldFill

@Composable
private fun goldText(): Color = if (FrameTheme.colors.isDark) ProGold.Light else ProGold.Text

@Composable
internal fun ProActiveCard(onManage: (() -> Unit)?, modifier: Modifier = Modifier) {
    val shape = FrameShapes.xl
    val rim = Brush.linearGradient(listOf(ProGold.Deep.copy(alpha = 0.85f), ProGold.Light.copy(alpha = 0.30f), ProGold.Deep.copy(alpha = 0.65f)))
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(FrameTheme.colors.fieldFill)
            .drawBehind {
                val center = Offset(size.width * 0.9f, size.height * 0.5f)
                val glow = listOf(ProGold.Deep.copy(alpha = 0.26f), Color.Transparent)
                drawCircle(Brush.radialGradient(glow, center, size.width * 0.6f), size.width * 0.6f, center)
            }
            .border(1.dp, rim, shape)
            .then(if (onManage != null) Modifier.frameClickable(onClickLabel = ProStrings.manage(), role = Role.Button, onClick = onManage) else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Medallion()
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            FrameText(ProStrings.settingsTitle(), FrameTheme.typography.title.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold), maxLines = 1)
            StatusPill()
        }
        if (onManage != null) {
            Image(painterResource(FrameIconsExtra.Chevron), null, Modifier.size(18.dp), colorFilter = ColorFilter.tint(goldText()))
        }
    }
}

@Composable
private fun Medallion() {
    val shimmer = rememberShimmer(4200, 400)
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(ProGold.Light, ProGold.Deep)), CircleShape)
            .border(1.5.dp, Color.White.copy(alpha = 0.5f), CircleShape)
            .shimmerSweep(shimmer, 0.55f),
        Alignment.Center
    ) {
        Box(Modifier.size(31.dp).background(ProGold.Ink.copy(alpha = 0.9f), CircleShape), Alignment.Center) {
            Image(painterResource(FrameIcons.StarFilled), null, Modifier.size(18.dp), colorFilter = ColorFilter.tint(ProGold.Light))
        }
    }
}

@Composable
private fun StatusPill() {
    val gold = goldText()
    Row(
        Modifier
            .background(ProGold.Deep.copy(alpha = 0.16f), FrameShapes.pill)
            .border(1.dp, ProGold.Deep.copy(alpha = 0.55f), FrameShapes.pill)
            .padding(start = 7.dp, end = 11.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Image(painterResource(FrameIcons.Check), null, Modifier.size(13.dp), colorFilter = ColorFilter.tint(gold))
        FrameText(ProStrings.active(), FrameTheme.typography.label.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold), gold, maxLines = 1)
    }
}
