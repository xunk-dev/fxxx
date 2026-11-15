package com.frameout.app.onboarding

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import com.frameout.designsystem.glass.frameGlow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.frameout.app.FrameOutApp
import com.frameout.app.applyAccent
import com.frameout.app.billing.accentBrush
import com.frameout.app.splash.BrandMark
import com.frameout.core.pro.ProCatalog
import com.frameout.core.pro.WelcomeGrant
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.SettingStrings
import com.frameout.designsystem.a11y.frameSelectable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.components.FrameWordmark
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.motion.LocalPageActive
import com.frameout.designsystem.motion.frameSpring
import com.frameout.designsystem.motion.rememberThemeWaveSource
import com.frameout.designsystem.theme.FrameAccent
import com.frameout.designsystem.theme.FrameAccents
import com.frameout.designsystem.theme.FrameTheme

internal const val WelcomeHeightFactor = 1f
internal val WelcomeExtraHeight = 0.dp

private const val ORB_COLUMNS = 4
private val OrbGap = 14.dp

@Composable
internal fun WelcomeHeader(width: Dp, modifier: Modifier = Modifier) {
    val tile = (width * 0.30f).coerceIn(72.dp, 112.dp)
    Column(
        modifier.width(width),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WelcomeGreeting(Modifier.padding(bottom = 10.dp))
        if (LocalPageActive.current) BrandMark(size = tile) else Spacer(Modifier.size(tile))
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            FrameWordmark(height = (width * 0.095f).coerceIn(24.dp, 32.dp))
        }
    }
}

@Composable
private fun WelcomeGreeting(modifier: Modifier = Modifier) {
    val colors = FrameTheme.colors
    val line = colors.primary.copy(alpha = 0.7f)
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(28.dp).height(1.dp).background(Brush.horizontalGradient(listOf(Color.Transparent, line))))
        FrameText(
            FrameStrings.onboardingWelcomeGreeting(),
            FrameTheme.typography.titleSmall,
            colors.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Box(Modifier.width(28.dp).height(1.dp).background(Brush.horizontalGradient(listOf(line, Color.Transparent))))
    }
}

@Composable
internal fun WelcomePicker(width: Dp, modifier: Modifier = Modifier) {
    val app = LocalContext.current.applicationContext as FrameOutApp
    val prefAccent by app.container.prefs.accent.collectAsState()
    val locked by app.container.entitlements.welcomeLock.collectAsState()
    val selected = locked ?: prefAccent
    Column(
        modifier.width(width),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AccentOrbs(width, selected, locked != null) {
            WelcomeChoice.picked = it.key
            app.container.prefs.setAccent(it.key)
        }
        val primary = FrameTheme.colors.primary
        Row(
            Modifier
                .clip(CircleShape)
                .background(primary.copy(alpha = 0.16f))
                .padding(horizontal = 16.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(8.dp).background(primary, CircleShape))
            FrameText(SettingStrings.accentLabel(selected), FrameTheme.typography.titleSmall, FrameTheme.colors.onSurface, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun AccentOrbs(width: Dp, selected: String, locked: Boolean, onPick: (FrameAccent) -> Unit) {
    val orb = ((width - OrbGap * (ORB_COLUMNS - 1)) / ORB_COLUMNS).coerceIn(34.dp, 56.dp)
    Column(verticalArrangement = Arrangement.spacedBy(OrbGap + 2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        FrameAccents.All.chunked(ORB_COLUMNS).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(OrbGap)) {
                row.forEach { WelcomeOrb(it, orb, it.key == selected, locked, onPick) }
            }
        }
    }
}

@Composable
private fun WelcomeOrb(accent: FrameAccent, size: Dp, on: Boolean, locked: Boolean, onPick: (FrameAccent) -> Unit) {
    val wave = rememberThemeWaveSource()
    val grow by animateFloatAsState(if (on) 1f else 0f, frameSpring(), label = "welcomeOrb")
    val label = SettingStrings.accentLabel(accent.key)
    val check = if (accent.previewB.luminance() > 0.5f) Color.Black else Color.White
    Box(
        Modifier
            .size(size)
            .then(wave.modifier)
            .graphicsLayer {
                val s = 1f + 0.16f * grow
                scaleX = s
                scaleY = s
                alpha = if (locked && !on) 0.35f else 1f
            }
            .frameGlow(accent.b, { 0.55f * grow }, 14.dp)
            .background(accentBrush(accent), CircleShape)
            .semantics { contentDescription = label }
            .frameSelectable(on, Role.RadioButton) {
                if (!on && !locked) {
                    wave.fire(accent.b)
                    onPick(accent)
                }
            },
        Alignment.Center
    ) {
        if (on) Image(painterResource(FrameIcons.Check), null, Modifier.size(size * 0.42f), colorFilter = ColorFilter.tint(check))
    }
}

@Composable
internal fun WelcomeNote(modifier: Modifier = Modifier) {
    val colors = FrameTheme.colors
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(FrameIcons.StarFilled), null, Modifier.size(14.dp), colorFilter = ColorFilter.tint(colors.primary))
        FrameText(FrameStrings.onboardingWelcomeNote(), FrameTheme.typography.bodySmall.copy(fontSize = 13.sp), colors.onSurface.copy(alpha = 0.78f), textAlign = TextAlign.Center)
    }
}

internal object WelcomeChoice {
    @Volatile var picked: String? = null
}

internal fun commitWelcomeColor(context: Context) {
    val app = context.applicationContext as FrameOutApp
    val entitlements = app.container.entitlements
    val grant = entitlements.welcome
    if (grant.decided) return
    entitlements.welcomeLock.value?.let {
        app.applyAccent(it)
        return
    }
    val picked = WelcomeChoice.picked
    if (picked == null || ProCatalog.accentIsFree(picked)) {
        grant.defer()
        app.applyAccent(picked ?: WelcomeGrant.FALLBACK)
        return
    }
    grant.commit(picked, entitlements.welcomeSpent())
    app.applyAccent(grant.key.value ?: picked)
    entitlements.noteWelcomeCommitted()
}
