package com.frameout.app.billing

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.frameout.app.icon.IconRaster
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.frameout.core.pro.ProFeature
import com.frameout.core.pro.ProStrings
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.theme.FrameTheme

private val IconKeys = listOf("indigo_dark", "sunset_dark", "emerald_light", "rose_amoled", "gold_dark")
private const val CYCLE_MS = 7500
private const val ICON_PX = 192

private fun focusOf(p: Float, index: Int): Float {
    val local = p * IconKeys.size - index
    val k = (1f - kotlin.math.abs(local - 0.5f) * 2f).coerceIn(0f, 1f)
    return k * k * (3f - 2f * k)
}

@Composable
internal fun IconsShowcase() {
    val colors = FrameTheme.colors
    val context = LocalContext.current
    val icons = remember { IconKeys.map { IconRaster.render(context, IconRaster.iconRes(context, it), ICON_PX)?.asImageBitmap() } }
    val cycle = rememberCycle(LocalSystemA11y.current.reduceMotion, CYCLE_MS, 0.3f)
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            icons.forEachIndexed { index, icon -> if (icon != null) IconTile(icon, index, cycle) }
        }
        FrameText(ProStrings.featureName(ProFeature.AppIcons), FrameTheme.typography.titleSmall, colors.onSurface)
    }
}

@Composable
private fun IconTile(icon: ImageBitmap, index: Int, cycle: State<Float>) {
    val shape = RoundedCornerShape(12.dp)
    Image(
        icon,
        null,
        Modifier
            .size(44.dp)
            .graphicsLayer {
                val k = focusOf(cycle.value, index)
                val s = 0.9f + 0.36f * k
                scaleX = s
                scaleY = s
                alpha = 0.72f + 0.28f * k
                translationY = -6.dp.toPx() * k
            }
            .clip(shape)
            .border(1.dp, Color.White.copy(alpha = 0.12f + 0.25f * focusOf(cycle.value, index)), shape),
        contentScale = ContentScale.Crop
    )
}
