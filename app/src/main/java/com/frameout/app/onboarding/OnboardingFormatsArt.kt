package com.frameout.app.onboarding

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin

@Composable
internal fun FmtMp4Art(bmp: ImageBitmap, t: State<Float>, fill: Modifier) {
    Image(bmp, null, fill.graphicsLayer {
        val p = t.value
        val s = fmtLerp(1.04f, 1.2f, p)
        scaleX = s; scaleY = s
        translationX = size.width * fmtLerp(-0.04f, 0.04f, p)
        translationY = size.height * fmtLerp(0.03f, -0.03f, p)
    }, contentScale = ContentScale.Crop)
}

@Composable
internal fun FmtMkvArt(bmp: ImageBitmap, t: State<Float>, fill: Modifier) {
    Box(fill) {
        Image(bmp, null, fill.graphicsLayer { val s = fmtLerp(1.04f, 1.16f, t.value); scaleX = s; scaleY = s }, contentScale = ContentScale.Crop)
        Image(bmp, null, fill.graphicsLayer {
            alpha = 0.4f; scaleX = 1.12f; scaleY = 1.12f; translationX = size.width * fmtLerp(-0.07f, 0.07f, t.value)
        }, contentScale = ContentScale.Crop, colorFilter = ColorFilter.tint(Color(0xFF00E5FF), BMODE))
        Image(bmp, null, fill.graphicsLayer {
            alpha = 0.4f; scaleX = 1.12f; scaleY = 1.12f; translationX = size.width * fmtLerp(0.07f, -0.07f, t.value)
        }, contentScale = ContentScale.Crop, colorFilter = ColorFilter.tint(Color(0xFFFF4DD2), BMODE))
    }
}

@Composable
internal fun FmtAviArt(bmp: ImageBitmap, t: State<Float>, fill: Modifier) {
    Box(fill) {
        Image(bmp, null, fill.graphicsLayer {
            val j = if ((t.value * 12f).toInt() % 2 == 0) 0f else 1.5.dp.toPx()
            scaleX = 1.12f; scaleY = 1.12f; translationX = j; translationY = -j * 0.5f
        }, contentScale = ContentScale.Crop, colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(1.3f) }))
        Canvas(fill) {
            val step = 3.dp.toPx()
            var y = 0f
            while (y < size.height) { drawRect(Color.Black.copy(alpha = 0.28f), Offset(0f, y), Size(size.width, 1.dp.toPx())); y += step }
            val bandY = (t.value * 1.3f - 0.3f) * size.height
            drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.18f), Color.Transparent), bandY, bandY + size.height * 0.3f), Offset(0f, bandY), Size(size.width, size.height * 0.3f))
        }
    }
}

@Composable
internal fun FmtMovArt(bmp: ImageBitmap, t: State<Float>, fill: Modifier) {
    Box(fill) {
        Image(bmp, null, fill.graphicsLayer {
            val s = fmtLerp(1.32f, 1.06f, t.value); scaleX = s; scaleY = s; translationY = size.height * fmtLerp(-0.04f, 0.03f, t.value)
        }, contentScale = ContentScale.Crop)
        Canvas(fill) {
            val bar = size.height * 0.13f
            drawRect(Color.Black, Offset.Zero, Size(size.width, bar))
            drawRect(Color.Black, Offset(0f, size.height - bar), Size(size.width, bar))
        }
    }
}

@Composable
internal fun FmtWebmArt(bmp: ImageBitmap, t: State<Float>, fill: Modifier) {
    Image(bmp, null, fill.graphicsLayer {
        transformOrigin = TransformOrigin(0.5f, 0.92f)
        rotationZ = fmtLerp(-4f, 4f, t.value)
        scaleX = 1.16f; scaleY = 1.16f
    }, contentScale = ContentScale.Crop)
}

@Composable
internal fun FmtGifArt(bmp: ImageBitmap, t: State<Float>, fill: Modifier) {
    Box(fill) {
        Image(bmp, null, fill.graphicsLayer {
            val fr = (t.value * 6f).toInt().coerceIn(0, 5)
            val sx = floatArrayOf(1.14f, 1.1f, 0.9f, 0.7f, 0.9f, 1.1f)[fr]
            scaleX = sx; scaleY = 1.14f
            rotationZ = floatArrayOf(0f, -3f, -5f, 0f, 4f, 2f)[fr]
            translationY = size.height * floatArrayOf(0f, -0.02f, -0.03f, -0.02f, -0.01f, 0.01f)[fr]
        }, contentScale = ContentScale.Crop, colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(1.4f) }))
        Canvas(fill) {
            val s = 4.dp.toPx()
            var y = s / 2f
            while (y < size.height) {
                var x = s / 2f
                while (x < size.width) { drawCircle(Color.Black.copy(alpha = 0.22f), 0.7.dp.toPx(), Offset(x, y)); x += s }
                y += s
            }
        }
    }
}

@Composable
internal fun FmtLowArt(bmp: ImageBitmap, t: State<Float>, fill: Modifier) {
    val small = rememberScaled(bmp, 28)
    Image(small, null, fill.graphicsLayer {
        val s = 1.1f + ((t.value * 4f).toInt().coerceIn(0, 3)) * 0.025f
        scaleX = s; scaleY = s
    }, contentScale = ContentScale.Crop, filterQuality = FilterQuality.None)
}

@Composable
internal fun FmtLiveArt(bmp: ImageBitmap, t: State<Float>, fill: Modifier) {
    Box(fill) {
        Image(bmp, null, fill.graphicsLayer {
            val p = t.value
            val bump = if (p < 0.38f || p > 0.8f) 0f else sin((p - 0.38f) / 0.42f * PI.toFloat())
            val s = 1.04f + 0.12f * bump
            scaleX = s; scaleY = s; rotationZ = 0.8f * bump
            translationX = size.width * 0.02f * bump
        }, contentScale = ContentScale.Crop)
        Canvas(fill) {
            val on = t.value in 0.38f..0.8f
            val c = if (on) Color(0xFFFFD60A) else Color.White.copy(alpha = 0.7f)
            val ctr = Offset(15.dp.toPx(), 15.dp.toPx())
            drawCircle(c, 8.dp.toPx(), ctr, style = Stroke(1.5.dp.toPx()))
            drawCircle(c, 2.6.dp.toPx(), ctr)
        }
    }
}

@Composable
internal fun FmtHevcArt(bmp: ImageBitmap, t: State<Float>, fill: Modifier) {
    val levels = intArrayOf(24, 36, 52, 76, 110, 160, 240)
    val p = t.value
    val idx = if (p >= 0.7f) -1 else ((1f - (1f - p / 0.7f).pow(2)) * levels.size).toInt().coerceIn(0, levels.size - 1)
    if (idx < 0) {
        Image(bmp, null, fill, contentScale = ContentScale.Crop)
    } else {
        Image(rememberScaled(bmp, levels[idx]), null, fill, contentScale = ContentScale.Crop, filterQuality = FilterQuality.None)
    }
}

private val BMODE = androidx.compose.ui.graphics.BlendMode.Modulate

@Composable
private fun rememberScaled(src: ImageBitmap, w: Int): ImageBitmap = remember(src, w) {
    val a = src.asAndroidBitmap()
    Bitmap.createScaledBitmap(a, w, max(1, w * a.height / a.width), false).asImageBitmap()
}
