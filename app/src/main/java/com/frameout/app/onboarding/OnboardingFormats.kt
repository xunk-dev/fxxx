package com.frameout.app.onboarding

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.app.R
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.glass.frameGlow
import com.frameout.designsystem.motion.LocalPageActive
import com.frameout.designsystem.theme.FrameTheme
import kotlin.math.PI
import kotlin.math.sin

internal const val FormatsHeightFactor = 1.4f
internal val FormatsExtraHeight = 0.dp

internal val OnboardingModeKinds = listOf("every_frame", "every_n", "every_time", "keyframes", "fixed", "scene", "slides", "content")

private val Gap = 10.dp

private enum class Kind { Mp4, Mkv, Avi, Mov, Webm, Gif, Low, Live, Hevc }

private class Fmt(val name: String, val hue: Float, val img: Int, val kind: Kind, val ms: Int, val reverse: Boolean, val ease: Boolean)

private val Formats = listOf(
    Fmt("MP4", 355f, R.drawable.home_img_turtle, Kind.Mp4, 8000, true, true),
    Fmt("MKV", 22f, R.drawable.smart_tree, Kind.Mkv, 9000, true, true),
    Fmt("AVI", 45f, R.drawable.smart_bird, Kind.Avi, 2600, false, false),
    Fmt("MOV", 150f, R.drawable.home_img_kitten, Kind.Mov, 7000, true, true),
    Fmt("WebM", 200f, R.drawable.smart_flower, Kind.Webm, 3200, true, true),
    Fmt("GIF", 265f, R.drawable.smart_butterfly, Kind.Gif, 900, false, false),
    Fmt("3GP", 320f, R.drawable.home_img_kitten, Kind.Low, 500, false, false),
    Fmt("Live", 168f, R.drawable.home_img_cat, Kind.Live, 4500, false, true),
    Fmt("HEVC", 95f, R.drawable.home_img_turtle, Kind.Hevc, 4400, false, false)
)

@Composable
internal fun FormatsStage(width: Dp, modifier: Modifier = Modifier) {
    val images = Formats.map { it.img }.distinct().associateWith { ImageBitmap.imageResource(it) }
    val float = rememberLoop(durationMs = 4600, still = 0f, reverse = false, label = "formatsFloat")
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(modifier.width(width), verticalArrangement = Arrangement.spacedBy(Gap)) {
            for (row in 0 until 3) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Gap)) {
                    for (col in 0 until 3) {
                        val i = row * 3 + col
                        FormatCard(Formats[i], images.getValue(Formats[i].img), i, float, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun FormatCard(f: Fmt, bmp: ImageBitmap, index: Int, float: State<Float>, modifier: Modifier) {
    val hue = Color.hsv(f.hue, 0.62f, 1f)
    val shape = RoundedCornerShape(18.dp)
    val active = LocalPageActive.current
    val appear by animateFloatAsState(if (active) 1f else 0f, tween(520, delayMillis = if (active) 90 + index * 60 else 0), label = "card")
    val t = rememberLoop(f.ms, if (f.kind == Kind.Hevc) 0.9f else 0.3f, f.reverse, "fmt_${f.name}", f.ease)
    Column(
        modifier.formatCardStyle(hue, shape, appear, float.value, index),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.Black)) {
            FormatArt(f.kind, bmp, t)
        }
        FrameText(
            f.name,
            style = FrameTheme.typography.body.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                shadow = Shadow(hue, Offset.Zero, 14f)
            ),
            color = Color.White,
            modifier = Modifier.fillMaxWidth().padding(bottom = 3.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

private fun Modifier.formatCardStyle(hue: Color, shape: Shape, appear: Float, floatValue: Float, index: Int): Modifier = this
    .aspectRatio(3f / 4.1f)
    .graphicsLayer {
        alpha = appear
        scaleX = 0.9f + 0.1f * appear
        scaleY = 0.9f + 0.1f * appear
        translationY = (1f - appear) * 26.dp.toPx() + sin((floatValue + index / 9f) * 2f * PI.toFloat()) * 3.dp.toPx()
    }
    .frameGlow(hue, { 0.45f }, 14.dp, { 0.12f })
    .clip(shape)
    .background(Brush.linearGradient(listOf(hue.copy(alpha = 0.36f), hue.copy(alpha = 0.07f), hue.copy(alpha = 0.22f))))
    .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.7f), hue.copy(alpha = 0.3f))), shape)
    .drawWithContent {
        drawContent()
        drawRect(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.38f), Color.Transparent), Offset.Zero, Offset(size.width * 0.9f, size.height * 0.55f)))
    }
    .padding(5.dp)

internal fun fmtLerp(a: Float, b: Float, t: Float) = a + (b - a) * t

@Composable
private fun FormatArt(kind: Kind, bmp: ImageBitmap, t: State<Float>) {
    val fill = Modifier.fillMaxSize()
    when (kind) {
        Kind.Mp4 -> FmtMp4Art(bmp, t, fill)
        Kind.Mkv -> FmtMkvArt(bmp, t, fill)
        Kind.Avi -> FmtAviArt(bmp, t, fill)
        Kind.Mov -> FmtMovArt(bmp, t, fill)
        Kind.Webm -> FmtWebmArt(bmp, t, fill)
        Kind.Gif -> FmtGifArt(bmp, t, fill)
        Kind.Low -> FmtLowArt(bmp, t, fill)
        Kind.Live -> FmtLiveArt(bmp, t, fill)
        Kind.Hevc -> FmtHevcArt(bmp, t, fill)
    }
}
