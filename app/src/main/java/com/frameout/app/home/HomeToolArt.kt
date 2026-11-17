package com.frameout.app.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.frameout.app.R
import com.frameout.designsystem.theme.FrameTheme

internal enum class HomeArt(
    @DrawableRes val res: Int,
    val bobMs: Int,
    val aspect: Float,
    val leftInset: Float,
    val rightInset: Float,
    val tilt: Float = 0f
) {
    Edit(R.drawable.home_gmap_edit, 4200, 1.111f, 0.119f, 0.056f, -7f),
    Images(R.drawable.home_gmap_images, 4700, 1.083f, 0.100f, 0.027f, 6f),
    Video(R.drawable.home_gmap_video, 4400, 1.081f, 0.146f, 0.090f, -5f),
    Collage(R.drawable.home_gmap_collage, 4900, 1.121f, 0.125f, 0.081f, 7f),
    Compare(R.drawable.home_gmap_compare, 4600, 1.028f, 0.121f, 0.054f)
}

@Composable
internal fun ToolArt(art: HomeArt, tone: Color, dark: Boolean, modifier: Modifier = Modifier) {
    val colors = FrameTheme.colors
    val accentB = colors.accentB
    val neutral = colors.neutral
    val ramp = remember(tone, accentB, dark, neutral) { toolIconRamp(tone, accentB, dark, neutral) }
    val filter = remember(ramp) { gradientMapFilter(ramp) }
    Box(
        modifier
            .drawBehind {
                val r = size.maxDimension * 0.78f
                val center = Offset(size.width * 0.5f, size.height * 0.55f)
                drawCircle(
                    Brush.radialGradient(listOf(ramp.body.copy(alpha = if (dark) 0.34f else 0.24f), Color.Transparent), center, r),
                    r,
                    center
                )
                val partnerCenter = Offset(size.width * 0.72f, size.height * 0.30f)
                val partnerR = size.maxDimension * 0.46f
                drawCircle(
                    Brush.radialGradient(listOf(ramp.deep.copy(alpha = if (dark) 0.16f else 0.12f), Color.Transparent), partnerCenter, partnerR),
                    partnerR,
                    partnerCenter
                )
            }
            .liquidFloat(art.tilt, art.bobMs),
        Alignment.Center
    ) {
        Image(
            painterResource(art.res),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
            colorFilter = filter
        )
    }
}
