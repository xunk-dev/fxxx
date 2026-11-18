package com.frameout.app.home

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import com.frameout.app.R
import kotlin.math.roundToInt

@Immutable
private class ProIconSpec(
    val body: ColorFilter,
    val rim: ColorFilter,
    val halo: ColorFilter
)

private fun proIconSpec(ramp: IconRamp, dark: Boolean): ProIconSpec = ProIconSpec(
    body = gradientMapFilter(ramp),
    rim = glowFilter(ramp.body, if (dark) 0.78f else 0.48f),
    halo = glowFilter(ramp.deep, if (dark) 0.50f else 0.30f)
)

private fun DrawScope.drawProAmbient(ramp: IconRamp, ambient: Float) {
    val c = Offset(size.width * 0.5f, size.height * 0.53f)
    val r = size.width * 0.66f
    drawCircle(
        Brush.radialGradient(
            0f to ramp.body.copy(alpha = ambient),
            0.5f to ramp.body.copy(alpha = ambient * 0.25f),
            1f to Color.Transparent,
            center = c,
            radius = r
        ),
        r,
        c
    )
    val c2 = Offset(size.width * 0.80f, size.height * 0.82f)
    val r2 = size.width * 0.34f
    drawCircle(
        Brush.radialGradient(listOf(ramp.deep.copy(alpha = ambient * 0.9f), Color.Transparent), c2, r2),
        r2,
        c2
    )
}

private fun DrawScope.drawProBody(bitmap: ImageBitmap, spec: ProIconSpec) {
    val dst = IntSize(size.width.roundToInt(), size.height.roundToInt())
    drawImage(bitmap, dstSize = dst, colorFilter = spec.body, filterQuality = FilterQuality.High)
}

@Composable
private fun BoxScope.ProGlowLayers(bitmap: ImageBitmap, size: Dp, spec: ProIconSpec) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    Image(
        bitmap,
        contentDescription = null,
        modifier = Modifier.fillMaxSize().blur(size * 0.11f),
        contentScale = ContentScale.Fit,
        colorFilter = spec.halo
    )
    Image(
        bitmap,
        contentDescription = null,
        modifier = Modifier.fillMaxSize().blur(size * 0.035f),
        contentScale = ContentScale.Fit,
        colorFilter = spec.rim
    )
}

@Composable
internal fun ProArt(look: HomeLook, size: Dp, modifier: Modifier) {
    val ramp = look.pro.iconRamp
    val dark = look.dark
    val spec = remember(ramp, dark) { proIconSpec(ramp, dark) }
    val bitmap = ImageBitmap.imageResource(R.drawable.home_gmap_pro)
    val ambient = if (dark) 0.30f else 0.14f
    Box(modifier.requiredSize(size).drawBehind { drawProAmbient(ramp, ambient) }) {
        ProGlowLayers(bitmap, size, spec)
        Canvas(Modifier.fillMaxSize().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
            drawProBody(bitmap, spec)
        }
    }
}
