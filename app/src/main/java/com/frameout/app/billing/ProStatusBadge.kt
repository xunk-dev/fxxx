package com.frameout.app.billing

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.app.FrameOutApp
import com.frameout.core.pro.ProFeature
import com.frameout.core.pro.ProStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameTheme

private val BadgeShape = RoundedCornerShape(50)

@Composable
fun ProStatusBadge(modifier: Modifier = Modifier, interactive: Boolean = true) {
    val entitlements = (LocalContext.current.applicationContext as FrameOutApp).container.entitlements
    if (!entitlements.enforced) return
    val pro by entitlements.isPro.collectAsState()
    var center by remember { mutableStateOf(false) }
    val manageable = pro && RevenueCatSync.isActive && entitlements.plan.isNotEmpty()
    val onClick: (() -> Unit)? = when {
        !interactive -> null
        !pro -> ({ entitlements.showPaywall(ProFeature.General) })
        manageable -> ({ center = true })
        else -> null
    }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Crossfade(targetState = pro, modifier = modifier, animationSpec = tween(400), label = "proBadge") { isPro ->
            val label = if (isPro) ProStrings.active() else ProStrings.settingsFree()
            Box(
                Modifier
                    .defaultMinSize(minHeight = 40.dp)
                    .then(if (onClick != null) Modifier.frameClickable(role = Role.Button, onClick = onClick) else Modifier)
                    .semantics(mergeDescendants = true) { contentDescription = label },
                contentAlignment = Alignment.Center
            ) {
                if (isPro) PaidBadge() else FreeBadge()
            }
        }
    }
    if (center) CustomerCenterHost { center = false }
}

@Composable
private fun FreeBadge() {
    val shimmer = rememberShimmer(3400)
    val ink = proOnBrush()
    Row(
        Modifier
            .height(28.dp)
            .clip(BadgeShape)
            .background(proBrush(), BadgeShape)
            .border(1.dp, proRim(0.32f), BadgeShape)
            .shimmerSweep(shimmer)
            .padding(start = 9.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(painterResource(FrameIcons.StarFilled), null, Modifier.size(12.dp), colorFilter = ColorFilter.tint(ink))
        FrameText(
            ProStrings.badge(),
            style = FrameTheme.typography.label.copy(fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.8.sp),
            color = ink,
            modifier = Modifier.padding(start = 5.dp),
            maxLines = 1
        )
        Image(painterResource(FrameIconsExtra.Chevron), null, Modifier.size(14.dp), colorFilter = ColorFilter.tint(ink))
    }
}

@Composable
private fun PaidBadge() {
    val shimmer = rememberShimmer(4200, 600)
    Row(
        Modifier
            .height(28.dp)
            .clip(BadgeShape)
            .background(Brush.linearGradient(listOf(ProGold.Light, ProGold.Deep)), BadgeShape)
            .border(1.dp, Color.White.copy(alpha = 0.5f), BadgeShape)
            .shimmerSweep(shimmer, 0.55f)
            .padding(start = 4.dp, end = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(20.dp).background(ProGold.Ink.copy(alpha = 0.9f), CircleShape), Alignment.Center) {
            Image(painterResource(FrameIcons.StarFilled), null, Modifier.size(11.dp), colorFilter = ColorFilter.tint(ProGold.Light))
        }
        FrameText(
            ProStrings.badge(),
            style = FrameTheme.typography.label.copy(fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.8.sp),
            color = ProGold.Ink,
            modifier = Modifier.padding(start = 6.dp),
            maxLines = 1
        )
    }
}
