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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
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

private val PRO_ICON_LUMA = floatArrayOf(0.213f, 0.715f, 0.072f)

private const val PRO_BODY_START = 0.56f
private const val PRO_BODY_RANGE = 0.42f

@Immutable
private class ProIconSpec(
    val body: ColorFilter,
    val rim: ColorFilter,
    val halo: ColorFilter
)

private fun proIconSpec(pro: ProLook, dark: Boolean): ProIconSpec {
    val lo = floatArrayOf(pro.iconDeep.red, pro.iconDeep.green, pro.iconDeep.blue)
    val hi = floatArrayOf(pro.iconHi.red, pro.iconHi.green, pro.iconHi.blue)
    val body = FloatArray(20)
    for (c in 0..2) {
        val span = hi[c] - lo[c]
        for (i in 0..2) body[c * 5 + i] = PRO_ICON_LUMA[i] * span / PRO_BODY_RANGE
        body[c * 5 + 4] = (lo[c] - span * PRO_BODY_START / PRO_BODY_RANGE) * 255f
    }
    body[18] = 1f
    return ProIconSpec(
        body = ColorFilter.colorMatrix(ColorMatrix(body)),
        rim = glowFilter(pro.iconGlow, if (dark) 0.78f else 0.48f),
        halo = glowFilter(pro.accentGlow, if (dark) 0.50f else 0.30f)
    )
}

private fun DrawScope.drawProAmbient(pro: ProLook, ambient: Float) {
    val c = Offset(size.width * 0.5f, size.height * 0.53f)
    val r = size.width * 0.66f
    drawCircle(
        Brush.radialGradient(
            0f to pro.iconGlow.copy(alpha = ambient),
            0.5f to pro.iconGlow.copy(alpha = ambient * 0.25f),
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
        Brush.radialGradient(listOf(pro.accentGlow.copy(alpha = ambient * 0.9f), Color.Transparent), c2, r2),
        r2,
        c2
    )
}

private fun DrawScope.drawProTint(pro: ProLook, dark: Boolean) {
    drawRect(
        Brush.linearGradient(
            listOf(Color.Transparent, pro.iconB.copy(alpha = 0.34f)),
            Offset.Zero,
            Offset(size.width, size.height)
        ),
        blendMode = BlendMode.SrcAtop
    )
    val badge = Offset(size.width * 0.80f, size.height * 0.80f)
    val badgeR = size.width * 0.26f
    drawCircle(
        Brush.radialGradient(listOf(pro.iconC.copy(alpha = 0.55f), Color.Transparent), badge, badgeR),
        badgeR,
        badge,
        blendMode = BlendMode.SrcAtop
    )
    val shine = Offset(size.width * 0.25f, size.height * 0.15f)
    val shineR = size.width * 0.45f
    drawCircle(
        Brush.radialGradient(listOf(Color.White.copy(alpha = if (dark) 0.30f else 0.36f), Color.Transparent), shine, shineR),
        shineR,
        shine,
        blendMode = BlendMode.SrcAtop
    )
    drawRect(
        Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.12f))),
        blendMode = BlendMode.SrcAtop
    )
}

private fun DrawScope.drawProBody(bitmap: ImageBitmap, spec: ProIconSpec, pro: ProLook, dark: Boolean) {
    val dst = IntSize(size.width.roundToInt(), size.height.roundToInt())
    drawImage(bitmap, dstSize = dst, colorFilter = spec.body, filterQuality = FilterQuality.High)
    drawProTint(pro, dark)
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
    val pro = look.pro
    val dark = look.dark
    val spec = remember(pro, dark) { proIconSpec(pro, dark) }
    val bitmap = ImageBitmap.imageResource(R.drawable.home_pro_icon_graphite)
    val ambient = if (dark) 0.30f else 0.14f
    Box(modifier.requiredSize(size).drawBehind { drawProAmbient(pro, ambient) }) {
        ProGlowLayers(bitmap, size, spec)
        Canvas(Modifier.fillMaxSize().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
            drawProBody(bitmap, spec, pro, dark)
        }
    }
}
