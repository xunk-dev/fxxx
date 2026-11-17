package com.frameout.app.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.frameout.app.BuildConfig
import com.frameout.app.settings.openLegalLink
import com.frameout.core.pro.ProStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.glass.GlassSurface
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameHairline

private class LegalItem(val title: String, val url: String)

internal fun hasLegalLinks(): Boolean = BuildConfig.TERMS_URL.isNotBlank() || BuildConfig.PRIVACY_URL.isNotBlank()

@Composable
internal fun LegalRows() {
    val context = LocalContext.current
    val items = buildList {
        if (BuildConfig.TERMS_URL.isNotBlank()) add(LegalItem(ProStrings.terms(), BuildConfig.TERMS_URL))
        if (BuildConfig.PRIVACY_URL.isNotBlank()) add(LegalItem(ProStrings.privacy(), BuildConfig.PRIVACY_URL))
    }
    Column(Modifier.fillMaxWidth()) {
        items.forEachIndexed { index, item ->
            if (index > 0) AboutDivider()
            LegalRow(item) { openLegalLink(context, item.url) }
        }
    }
}

@Composable
internal fun AboutDivider() {
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(1.dp).background(frameHairline()))
}

@Composable
private fun LegalRow(item: LegalItem, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    Row(
        Modifier.fillMaxWidth().frameClickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        IconBadge(FrameIconsExtra.Link, colors.primary)
        FrameText(item.title, style = FrameTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        Image(painterResource(FrameIconsExtra.Chevron), null, Modifier.size(18.dp), colorFilter = ColorFilter.tint(colors.onSurfaceVariant))
    }
}

@Composable
internal fun AboutCard(title: String, body: String, icon: Int? = null, tint: Color = Color.Unspecified) {
    GlassSurface(shape = FrameShapes.xl, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                if (icon != null) IconBadge(icon, if (tint == Color.Unspecified) FrameTheme.colors.primary else tint)
                FrameText(title, style = FrameTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            }
            FrameText(body, style = FrameTheme.typography.bodySmall, color = FrameTheme.colors.onSurfaceVariant)
        }
    }
}
