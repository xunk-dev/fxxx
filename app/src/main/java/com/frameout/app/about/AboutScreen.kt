package com.frameout.app.about

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.frameout.app.PreferredFrameTheme
import com.frameout.app.rating.StoreLauncher
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.ModeStrings
import com.frameout.core.util.RatingStrings
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.components.FrameTopBar
import com.frameout.designsystem.glass.GlassSurface
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameHairline
import kotlinx.coroutines.delay

private val YoutubeRed = Color(0xFFFF3B30)

@Composable
fun AboutScreen(onBack: () -> Unit) {
    PreferredFrameTheme {
        val scroll = rememberScrollState()
        val reduce = LocalSystemA11y.current.reduceMotion
        var showLicenses by rememberSaveable { mutableStateOf(false) }
        val body = remember { Animatable(if (reduce) 1f else 0f) }
        LaunchedEffect(Unit) {
            if (reduce) return@LaunchedEffect
            delay(420)
            body.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
        }
        LaunchedEffect(showLicenses) { scroll.scrollTo(0) }
        BackHandler(enabled = showLicenses) { showLicenses = false }
        AboutLayout(
            scroll = scroll,
            showLicenses = showLicenses,
            body = body,
            onBack = onBack,
            onShowLicenses = { showLicenses = true },
            onHideLicenses = { showLicenses = false }
        )
    }
}

@Composable
private fun AboutLayout(
    scroll: ScrollState,
    showLicenses: Boolean,
    body: Animatable<Float, AnimationVector1D>,
    onBack: () -> Unit,
    onShowLicenses: () -> Unit,
    onHideLicenses: () -> Unit
) {
    val colors = FrameTheme.colors
    Box(Modifier.fillMaxSize().background(colors.background), Alignment.TopCenter) {
        if (!showLicenses) Box(Modifier.fillMaxWidth().height(420.dp).aboutAurora())
        Column(
            Modifier
                .fillMaxHeight()
                .widthIn(max = 720.dp)
                .fillMaxWidth()
                .statusBarsPadding()
                .verticalScroll(scroll)
        ) {
            FrameTopBar(
                title = if (showLicenses) ModeStrings.aboutLicensesTitle() else ModeStrings.about(),
                onBack = if (showLicenses) onHideLicenses else onBack,
                backContentDescription = FrameStrings.back(),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            if (showLicenses) {
                Column(Modifier.padding(horizontal = 16.dp)) { LicensesPage() }
                Spacer(Modifier.navigationBarsPadding().height(24.dp))
            } else {
                AboutHero()
                AboutSheet(body, onLicenses = onShowLicenses)
            }
        }
    }
}

private val SheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)

@Composable
private fun AboutSheet(body: Animatable<Float, AnimationVector1D>, onLicenses: () -> Unit) {
    val colors = FrameTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 20.dp)
            .graphicsLayer {
                alpha = body.value
                translationY = (1f - body.value) * 40.dp.toPx()
            }
            .clip(SheetShape)
            .background(colors.container)
            .border(1.dp, frameHairline(), SheetShape)
            .padding(horizontal = 16.dp)
            .padding(top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        PlainInfo(ModeStrings.aboutOfflineTitle(), ModeStrings.aboutOfflineBody(), FrameIconsExtra.Storage, colors.success)
        if (com.frameout.app.BuildConfig.PLAY_BILLING) {
            InfoTile(ModeStrings.aboutProTitle(), ModeStrings.aboutProBody(), FrameIconsExtra.Sparkle, colors.primary, glow = colors.primary)
        }
        ConnectRow()
        LinksCard(onLicenses)
        com.frameout.app.remote.PromoSlot(Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        Spacer(Modifier.navigationBarsPadding().height(16.dp))
    }
}

@Composable
private fun PlainInfo(title: String, body: String, icon: Int, tint: Color) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        IconBadge(icon, tint)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            FrameText(title, style = FrameTheme.typography.titleSmall)
            FrameText(body, style = FrameTheme.typography.bodySmall, color = FrameTheme.colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun InfoTile(title: String, body: String, icon: Int, tint: Color, glow: Color = Color.Unspecified) {
    GlassSurface(shape = FrameShapes.xl, glow = glow, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            IconBadge(icon, tint)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FrameText(title, style = FrameTheme.typography.titleSmall)
                FrameText(body, style = FrameTheme.typography.bodySmall, color = FrameTheme.colors.onSurfaceVariant)
            }
        }
    }
}

private class ConnectItem(val icon: Int, val tint: Color, val title: String, val onClick: () -> Unit)

@Composable
private fun ConnectRow() {
    val colors = FrameTheme.colors
    val context = LocalContext.current
    val items = buildList {
        add(ConnectItem(FrameIcons.StarFilled, colors.warning, RatingStrings.rowTitle()) { StoreLauncher.openStore(context) })
        if (AboutConfig.hasEmail) {
            add(ConnectItem(FrameIconsExtra.Mail, colors.primary, ModeStrings.aboutEmailTitle()) { AboutLinks.writeEmail(context) })
        }
        if (AboutConfig.hasYoutube) {
            add(ConnectItem(FrameIconsExtra.Youtube, YoutubeRed, ModeStrings.aboutYoutubeTitle()) { AboutLinks.openYoutube(context) })
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
        items.forEach { ConnectCircle(it) }
    }
}

@Composable
private fun ConnectCircle(item: ConnectItem) {
    Column(
        Modifier.width(96.dp).frameClickable(onClick = item.onClick).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            Modifier.size(64.dp).clip(CircleShape).background(item.tint.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Image(painterResource(item.icon), null, Modifier.size(28.dp), colorFilter = ColorFilter.tint(item.tint))
        }
        FrameText(
            item.title,
            style = FrameTheme.typography.label,
            color = FrameTheme.colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

@Composable
private fun LinksCard(onLicenses: () -> Unit) {
    GlassSurface(shape = FrameShapes.xl, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth()) {
            if (hasLegalLinks()) {
                LegalRows()
                AboutDivider()
            }
            LicensesRow(onLicenses)
        }
    }
}

@Composable
private fun LicensesRow(onClick: () -> Unit) {
    val colors = FrameTheme.colors
        Row(
            Modifier.fillMaxWidth().frameClickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            IconBadge(FrameIcons.Info, colors.accentC)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                FrameText(ModeStrings.aboutLicensesTitle(), style = FrameTheme.typography.titleSmall)
                FrameText(
                    "Apache 2.0 · OFL 1.1 · LGPL · BSD",
                    style = FrameTheme.typography.monoSmall.copy(textDirection = TextDirection.Ltr),
                    color = colors.onSurfaceVariant
                )
            }
            Image(painterResource(FrameIconsExtra.Chevron), null, Modifier.size(18.dp), colorFilter = ColorFilter.tint(colors.onSurfaceVariant))
        }
}

@Composable
private fun LicensesPage() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
        AboutCard(ModeStrings.aboutLicensesTitle(), ModeStrings.aboutLicensesBody(), FrameIcons.Info, FrameTheme.colors.accentC)
        Column { CodecPackLicenseSection() }
    }
}

@Composable
internal fun IconBadge(icon: Int, tint: Color) {
    Box(
        Modifier.size(48.dp).clip(FrameShapes.sm).background(tint.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        Image(painterResource(icon), null, Modifier.size(26.dp), colorFilter = ColorFilter.tint(tint))
    }
}
