package com.frameout.app.settings

import com.frameout.core.pro.ProTier
import com.frameout.app.billing.TierPip
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.frameout.app.BuildConfig
import com.frameout.app.billing.ProGate
import com.frameout.app.billing.proBrush
import com.frameout.app.billing.proOnBrush
import com.frameout.app.billing.rememberProGate
import com.frameout.core.ads.AdStrings
import com.frameout.core.pro.ProCatalog
import com.frameout.core.pro.ProFeature
import com.frameout.core.pro.ProStrings
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.a11y.frameSelectable
import com.frameout.designsystem.components.FrameScrollRow
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.motion.rememberThemeWaveSource
import com.frameout.designsystem.theme.FrameAccent
import com.frameout.designsystem.theme.FrameAccents
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.resolveFrameAccent

private const val SWATCH_DP = 40
private const val COLUMNS = 4

private enum class Lock { Open, Ad, Pro }

private fun lockOf(key: String, gate: ProGate): Lock = when {
    !gate.enforced || gate.isPro || ProCatalog.accentIsFree(key, gate.welcomeKey) -> Lock.Open
    gate.welcomeOpen && ProCatalog.welcomePickable(key) -> Lock.Open
    ProCatalog.accentIsProOnly(key) || !BuildConfig.ADS_ENABLED -> Lock.Pro
    gate.rewarded(ProFeature.Accents) -> Lock.Open
    else -> Lock.Ad
}

@Composable
internal fun AccentPicker(value: String, onChange: (String) -> Unit) {
    val context = LocalContext.current
    val options = remember {
        buildList {
            addAll(FrameAccents.All)
            if (FrameAccents.dynamicSupported()) add(resolveFrameAccent(FrameAccents.DYNAMIC, context))
        }
    }
    val gate = rememberProGate()
    val guarded: (String, () -> Unit) -> Unit = { key, applied ->
        val apply = { applied(); onChange(key) }
        when (lockOf(key, gate)) {
            Lock.Open -> when {
                !gate.needsWelcomeClaim(key) -> apply()
                gate.tryWelcomePick(key) -> apply()
                else -> gate.showProFor(ProFeature.Accents, key)
            }
            Lock.Pro -> gate.showProFor(ProFeature.Accents, key)
            Lock.Ad -> gate.run(ProFeature.Accents, key, accentKey = key, action = apply)
        }
    }
    val ordered = options.map { it to lockOf(it.key, gate) }.sortedBy { if (it.second == Lock.Open) 0 else 1 }
    FrameScrollRow(spacing = 10.dp) {
        ordered.forEach { (accent, lock) -> AccentSwatch(accent, accent.key == value, guarded, lock) }
    }
}

@Composable
private fun AccentSwatch(accent: FrameAccent, selected: Boolean, onChange: (String, () -> Unit) -> Unit, lock: Lock) {
    val wave = rememberThemeWaveSource()
    val name = FrameStrings.settingValue(accent.key)
    val state = when (lock) {
        Lock.Open -> ""
        Lock.Ad -> ", " + AdStrings.rewardWatch()
        Lock.Pro -> ", " + ProStrings.badge()
    }
    val fill = remember(accent) { Brush.linearGradient(listOf(accent.previewA, accent.previewB, accent.previewC)) }
    val ring = if (selected) FrameTheme.colors.onSurface else Color.Transparent
    val checkTint = if (accent.previewB.luminance() > 0.5f) Color.Black else Color.White
    val rim = if (accent.previewB.luminance() < 0.1f) FrameTheme.colors.onSurface.copy(alpha = 0.45f) else Color.Transparent
    Box(Modifier, Alignment.Center) {
        Box(
            Modifier
                .size(SWATCH_DP.dp)
                .then(wave.modifier)
                .clip(CircleShape)
                .semantics { contentDescription = name + state }
                .frameSelectable(selected, Role.RadioButton) {
                    onChange(accent.key) { if (!selected) wave.fire(accent.b) }
                }
                .border(2.dp, ring, CircleShape)
                .padding(5.dp)
                .clip(CircleShape)
                .background(fill)
                .border(1.dp, rim, CircleShape),
            Alignment.Center
        ) {
            if (selected) {
                Image(painterResource(FrameIcons.Check), null, Modifier.size(16.dp), colorFilter = ColorFilter.tint(checkTint))
            }
        }
        if (lock != Lock.Open) TierPip(if (lock == Lock.Pro) ProTier.Pro else ProTier.Ad, Modifier.align(Alignment.BottomEnd))
    }
}
