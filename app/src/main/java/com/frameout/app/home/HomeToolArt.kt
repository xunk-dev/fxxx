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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.frameout.app.R
import com.frameout.designsystem.theme.FrameTheme
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

internal enum class HomeArt(
    @DrawableRes val res: Int,
    val baseHue: Float,
    val tint: Float,
    val bobMs: Int,
    val aspect: Float,
    val leftInset: Float,
    val rightInset: Float,
    val tilt: Float = 0f
) {
    Edit(R.drawable.home_tool_edit, 251f, 1f, 4200, 1.111f, 0.119f, 0.056f, -7f),
    Images(R.drawable.home_tool_images, 246f, 1f, 4700, 1.083f, 0.100f, 0.027f, 6f),
    Video(R.drawable.home_tool_video, 248f, 1f, 4400, 1.081f, 0.146f, 0.090f, -5f),
    Collage(R.drawable.home_tool_collage, 248f, 1f, 4900, 1.121f, 0.125f, 0.081f, 7f),
    Compare(R.drawable.home_tool_compare, 248f, 1f, 4600, 1.028f, 0.121f, 0.054f)
}

private const val TONE_HUE_PULL = 1f

private fun hueOf(c: Color): Float {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(c.toArgb(), hsv)
    return hsv[0]
}

private fun hueMatrix(deg: Float): ColorMatrix {
    val r = Math.toRadians(deg.toDouble())
    val c = cos(r).toFloat()
    val s = sin(r).toFloat()
    return ColorMatrix(
        floatArrayOf(
            0.213f + c * 0.787f - s * 0.213f, 0.715f - c * 0.715f - s * 0.715f, 0.072f - c * 0.072f + s * 0.928f, 0f, 0f,
            0.213f - c * 0.213f + s * 0.143f, 0.715f + c * 0.285f + s * 0.140f, 0.072f - c * 0.072f - s * 0.283f, 0f, 0f,
            0.213f - c * 0.213f - s * 0.787f, 0.715f - c * 0.715f + s * 0.715f, 0.072f + c * 0.928f + s * 0.072f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    )
}

@Composable
private fun rememberArtFilter(art: HomeArt, tone: Color): ColorFilter? {
    val c = FrameTheme.colors
    return remember(art, c.primary, c.neutral, tone) {
        if (c.neutral) {
            ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.12f) })
        } else {
            val primaryHue = hueOf(c.primary)
            val target = primaryHue + hueDelta(primaryHue, hueOf(tone)) * TONE_HUE_PULL
            val raw = target - art.baseHue
            val wrapped = ((raw + 540f) % 360f) - 180f
            val delta = wrapped * art.tint
            if (abs(delta) < 1f) null else ColorFilter.colorMatrix(hueMatrix(delta))
        }
    }
}

@Composable
internal fun ToolArt(art: HomeArt, tone: Color, dark: Boolean, modifier: Modifier = Modifier) {
    val filter = rememberArtFilter(art, tone)
    Box(
        modifier
            .drawBehind {
                val r = size.maxDimension * 0.78f
                val center = Offset(size.width * 0.5f, size.height * 0.55f)
                drawCircle(
                    Brush.radialGradient(listOf(tone.copy(alpha = if (dark) 0.46f else 0.26f), Color.Transparent), center, r),
                    r,
                    center
                )
                val partnerCenter = Offset(size.width * 0.72f, size.height * 0.30f)
                val partnerR = size.maxDimension * 0.46f
                drawCircle(
                    Brush.radialGradient(listOf(analog(tone, 34f).copy(alpha = if (dark) 0.26f else 0.14f), Color.Transparent), partnerCenter, partnerR),
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
