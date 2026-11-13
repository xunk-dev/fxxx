package com.frameout.app.home

import android.content.Context
import android.os.Build
import android.util.TypedValue
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.fillMaxSize
import com.frameout.designsystem.theme.FrameTheme

internal fun glassFilter(primary: Color, secondary: Color): ColorFilter {
    val p = floatArrayOf(primary.red, primary.green, primary.blue)
    val s = floatArrayOf(secondary.red, secondary.green, secondary.blue)
    val m = FloatArray(20)
    for (c in 0..2) {
        val c0 = s[c] * 0.30f + p[c] * 0.55f
        val c1 = p[c] * 0.85f + s[c] * 0.15f
        val c2 = 1f
        val cm = c1 - (c0 + c2) / 2f
        m[c * 5 + 0] = p[c] * 1.35f
        m[c * 5 + 1] = c2 - c0
        m[c * 5 + 2] = cm
        m[c * 5 + 4] = c0 * 255f
    }
    m[15] = 1.2f
    m[16] = 0.92f
    m[18] = 0.16f
    return ColorFilter.colorMatrix(ColorMatrix(m))
}

@Composable
internal fun glassStageOrNull(): Brush? {
    if (FrameTheme.colors.isDark) return null
    val c0 = FrameTheme.accent.darkContainer
    return Brush.linearGradient(listOf(c0, lerp(c0, Color(0xFF050808), 0.65f)))
}

internal fun glowFilter(color: Color, strength: Float): ColorFilter {
    val m = FloatArray(20)
    m[4] = color.red * 255f
    m[9] = color.green * 255f
    m[14] = color.blue * 255f
    m[18] = strength
    return ColorFilter.colorMatrix(ColorMatrix(m))
}

@Composable
internal fun GlassArtwork(
    resId: Int,
    primary: Color,
    secondary: Color,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val glow = lerp(primary, secondary, 0.5f)
    val filter = remember(primary, secondary) { glassFilter(primary, secondary) }
    BoxWithConstraints(modifier) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Image(
                painterResource(resId), null,
                Modifier.fillMaxSize().blur(maxWidth * 0.045f),
                contentScale = contentScale,
                colorFilter = remember(glow) { glowFilter(glow, 0.55f) }
            )
        }
        Image(painterResource(resId), null, Modifier.fillMaxSize(), contentScale = contentScale, colorFilter = filter)
    }
}

internal fun isXmlDrawable(context: Context, resId: Int): Boolean = runCatching {
    val tv = TypedValue()
    context.resources.getValue(resId, tv, true)
    tv.string?.endsWith(".xml") == true
}.getOrDefault(false)
