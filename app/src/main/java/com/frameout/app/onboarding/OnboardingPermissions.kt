package com.frameout.app.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.app.oem.OemSettings
import com.frameout.app.oem.OemStepsCard
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameHairline

private enum class Glyph { Media, Bell, Bolt }

@Composable
internal fun PermissionPanel(perms: ReadyPermissions, modifier: Modifier = Modifier) {
    val colors = FrameTheme.colors
    val shape = FrameShapes.lg
    val pending = !perms.media || !perms.notifications || !perms.battery
    AnimatedVisibility(pending, modifier, exit = shrinkVertically() + fadeOut()) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.container.copy(alpha = 0.9f))
                .border(1.dp, frameHairline(), shape)
        ) {
            PendingRow(!perms.media, false, Glyph.Media, FrameStrings.onboardingPermMedia(), FrameStrings.onboardingPermMediaDesc(), perms.requestMedia)
            PendingRow(!perms.notifications, !perms.media, Glyph.Bell, FrameStrings.onboardingPermNotif(), FrameStrings.onboardingPermNotifDesc(), perms.requestNotifications)
            PendingRow(!perms.battery, !perms.media || !perms.notifications, Glyph.Bolt, FrameStrings.onboardingPermBattery(), FrameStrings.onboardingPermBatteryDesc(), perms.requestBattery) {
                if (OemSettings.isCustom(perms.oem)) OemStepsCard(perms.oem, Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp))
            }
        }
    }
}

@Composable
private fun PendingRow(
    visible: Boolean,
    divider: Boolean,
    glyph: Glyph,
    title: String,
    description: String,
    onAllow: () -> Unit,
    extra: @Composable () -> Unit = {}
) {
    AnimatedVisibility(visible, exit = shrinkVertically() + fadeOut()) {
        Column {
            if (divider) Box(Modifier.fillMaxWidth().height(1.dp).background(frameHairline()))
            PermissionRow(glyph, title, description, onAllow)
            extra()
        }
    }
}

@Composable
private fun PermissionRow(glyph: Glyph, title: String, description: String, onAllow: () -> Unit) {
    val colors = FrameTheme.colors
    val tile = FrameShapes.md
    Row(
        Modifier.fillMaxWidth().padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(tile)
                .background(colors.containerHigh.copy(alpha = 0.6f))
                .border(1.dp, frameHairline(), tile),
            contentAlignment = Alignment.Center
        ) {
            GlyphIcon(glyph, colors.onSurfaceVariant, Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            FrameText(title, style = FrameTheme.typography.titleSmall.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold))
            FrameText(
                description,
                style = FrameTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp),
                color = colors.onSurfaceVariant,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }
        AllowButton(onAllow)
    }
}

@Composable
private fun AllowButton(onAllow: () -> Unit) {
    val colors = FrameTheme.colors
    FrameText(
        FrameStrings.notifAllow(),
        style = FrameTheme.typography.titleSmall.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
        maxLines = 1,
        modifier = Modifier
            .clip(FrameShapes.pill)
            .frameClickable(onClick = onAllow)
            .background(colors.containerHigh)
            .border(1.dp, frameHairline(), FrameShapes.pill)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    )
}

@Composable
private fun GlyphIcon(glyph: Glyph, tint: Color, modifier: Modifier) {
    when (glyph) {
        Glyph.Media -> Image(painterResource(FrameIcons.Library), null, modifier, colorFilter = ColorFilter.tint(tint))
        Glyph.Bell -> Canvas(modifier) { drawBell(tint) }
        Glyph.Bolt -> Canvas(modifier) { drawBolt(tint) }
    }
}

private fun DrawScope.drawBell(color: Color) {
    val w = size.width
    val h = size.height
    val body = Path().apply {
        moveTo(w * 0.18f, h * 0.74f)
        cubicTo(w * 0.30f, h * 0.64f, w * 0.30f, h * 0.52f, w * 0.32f, h * 0.42f)
        cubicTo(w * 0.34f, h * 0.26f, w * 0.66f, h * 0.26f, w * 0.68f, h * 0.42f)
        cubicTo(w * 0.70f, h * 0.52f, w * 0.70f, h * 0.64f, w * 0.82f, h * 0.74f)
        close()
    }
    drawPath(body, color)
    drawCircle(color, radius = w * 0.07f, center = Offset(w * 0.5f, h * 0.85f))
    drawCircle(color, radius = w * 0.05f, center = Offset(w * 0.5f, h * 0.20f))
}

private fun DrawScope.drawBolt(color: Color) {
    val w = size.width
    val h = size.height
    val bolt = Path().apply {
        moveTo(w * 0.58f, h * 0.06f)
        lineTo(w * 0.24f, h * 0.56f)
        lineTo(w * 0.47f, h * 0.56f)
        lineTo(w * 0.40f, h * 0.94f)
        lineTo(w * 0.76f, h * 0.42f)
        lineTo(w * 0.53f, h * 0.42f)
        close()
    }
    drawPath(bolt, color)
}
