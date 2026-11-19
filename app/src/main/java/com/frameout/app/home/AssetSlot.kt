package com.frameout.app.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.frameout.designsystem.theme.FrameTheme

@Composable
internal fun AssetSlot(
    name: String,
    fallbackIcon: Int,
    modifier: Modifier = Modifier,
    tint: Color = FrameTheme.colors.primary,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    val glass = remember(name) { context.resources.getIdentifier("glass_$name", "drawable", context.packageName) }
    if (glass != 0) {
        GlassArtwork(glass, FrameTheme.colors.onDarkAccent, FrameTheme.accent.c, modifier, contentScale)
        return
    }
    val id = remember(name) { context.resources.getIdentifier(name, "drawable", context.packageName) }
    if (id != 0) {
        val xml = remember(id) { isXmlDrawable(context, id) }
        if (xml) {
            GlassArtwork(id, FrameTheme.colors.onDarkAccent, FrameTheme.accent.c, modifier, contentScale)
        } else {
            Image(painterResource(id), contentDescription = null, modifier = modifier, contentScale = contentScale)
        }
        return
    }
    val colors = FrameTheme.colors
    Box(
        modifier.background(Brush.linearGradient(listOf(tint.copy(alpha = 0.34f), colors.containerHigh, tint.copy(alpha = 0.12f)))),
        Alignment.Center
    ) {
        Image(
            painterResource(fallbackIcon),
            contentDescription = null,
            modifier = Modifier.size(52.dp).alpha(0.85f),
            colorFilter = ColorFilter.tint(tint)
        )
    }
}

@Composable
internal fun AssetSlotFill(name: String, fallbackIcon: Int) {
    AssetSlot(name, fallbackIcon, Modifier.fillMaxSize())
}
