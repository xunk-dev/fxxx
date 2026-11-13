package com.frameout.app.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.style.TextOverflow
import com.frameout.app.R
import com.frameout.core.util.HomeStrings
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.haptics.FrameHaptic
import com.frameout.designsystem.haptics.rememberFrameHaptics
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameTheme

@Composable
internal fun CollageArt() = Ltr {
    val t by loop(0f, 1f, 3200)
    Row(
        Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp)),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        HomeImage("home_img_turtle", Modifier.weight(1.25f - 0.5f * t).fillMaxHeight(), zoom = 1.1f, ox = 0.62f, oy = 0.38f)
        Column(Modifier.weight(0.75f + 0.5f * t).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            HomeImage("home_img_birds", Modifier.weight(1.1f - 0.3f * t).fillMaxWidth(), zoom = 2.1f, ox = 0.67f, oy = 0.26f)
            HomeImage("home_img_cat", Modifier.weight(0.9f + 0.3f * t).fillMaxWidth(), zoom = 2.2f, ox = 0.54f, oy = 0.43f)
        }
    }
}

@Composable
internal fun CompareArt() = Ltr {
    val split by loop(0.18f, 0.82f, 2600)
    val dim = remember { ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.15f) }) }
    Box(
        Modifier.fillMaxSize().drawWithContent {
            drawContent()
            val x = size.width * split
            val w = 2.dp.toPx()
            drawLine(Color.White, Offset(x, 0f), Offset(x, size.height), w)
            val c = Offset(x, size.height * 0.4f)
            drawCircle(Color.White, 15.dp.toPx(), c)
            val k = 4.dp.toPx(); val g = 3.dp.toPx(); val ink = Color(0xFF111111)
            drawLine(ink, Offset(c.x - g, c.y - k), Offset(c.x - g - k, c.y), 2.dp.toPx(), StrokeCap.Round)
            drawLine(ink, Offset(c.x - g - k, c.y), Offset(c.x - g, c.y + k), 2.dp.toPx(), StrokeCap.Round)
            drawLine(ink, Offset(c.x + g, c.y - k), Offset(c.x + g + k, c.y), 2.dp.toPx(), StrokeCap.Round)
            drawLine(ink, Offset(c.x + g + k, c.y), Offset(c.x + g, c.y + k), 2.dp.toPx(), StrokeCap.Round)
        }
    ) {
        HomeImage("home_img_trees", Modifier.fillMaxSize(), 1f, 0.5f, 0.5f, dim)
        Box(Modifier.fillMaxSize().drawWithContent {
            clipRect(left = size.width * split) { this@drawWithContent.drawContent() }
        }) { HomeImage("home_img_trees", Modifier.fillMaxSize()) }
    }
}

@Composable
internal fun EditArt() = Ltr {
    val phase by loop(0f, 4f, 8000, reverse = false)
    val sat = when {
        phase < 1f -> 1f + 0.6f * phase
        phase < 2f -> 1.6f - 1.6f * (phase - 1f)
        phase < 3f -> 1.9f * (phase - 2f)
        else -> 1.9f - 0.9f * (phase - 3f)
    }
    val cf by loop(0.10f, 0.20f, 2800)
    val th by loop(0.1f, 0.9f, 2400)
    val filter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(sat) })
    Box(
        Modifier.fillMaxSize().drawWithContent {
            drawContent()
            val l = size.width * cf; val t = size.height * cf
            val r = size.width * (1f - cf); val b = size.height * (1f - cf * 1.7f)
            val dimC = Color.Black.copy(alpha = 0.35f)
            drawRect(dimC, Offset.Zero, Size(size.width, t))
            drawRect(dimC, Offset(0f, b), Size(size.width, size.height - b))
            drawRect(dimC, Offset(0f, t), Size(l, b - t))
            drawRect(dimC, Offset(r, t), Size(size.width - r, b - t))
            drawRect(Color.White.copy(alpha = 0.6f), Offset(l, t), Size(r - l, b - t), style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx()))
            val a = 14.dp.toPx(); val s = 3.dp.toPx()
            fun corner(cx: Float, cy: Float, dx: Float, dy: Float) {
                val p = Path().apply { moveTo(cx + dx * a, cy); lineTo(cx, cy); lineTo(cx, cy + dy * a) }
                drawPath(p, Color.White, style = androidx.compose.ui.graphics.drawscope.Stroke(s, cap = StrokeCap.Round))
            }
            corner(l, t, 1f, 1f); corner(r, t, -1f, 1f); corner(l, b, 1f, -1f); corner(r, b, -1f, -1f)
            val y = size.height - 16.dp.toPx()
            val x0 = size.width * 0.14f; val x1 = size.width * 0.86f
            drawLine(Color.White.copy(alpha = 0.35f), Offset(x0, y), Offset(x1, y), 4.dp.toPx(), StrokeCap.Round)
            drawCircle(Color.White, 7.dp.toPx(), Offset(x0 + (x1 - x0) * th, y))
        }
    ) { HomeImage("home_img_kitten", Modifier.fillMaxSize(), filter = filter) }
}

@Composable
private fun Chips(vararg labels: String, modifier: Modifier = Modifier, active: Int = -1) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        labels.forEachIndexed { i, label ->
            val on = active < 0 || i == active
            Box(Modifier.clip(CircleShape).background(if (on) Color.White else Color.Black.copy(alpha = 0.55f)).padding(horizontal = 6.dp, vertical = 3.dp)) {
                BasicText(label, style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (on) Color(0xFF0B4A3F) else Color.White))
            }
        }
    }
}

@Composable
private fun VideoFace(active: Int) {
    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp))) {
        HomeImage("home_img_turtle", Modifier.fillMaxSize(), zoom = 1.6f, ox = 0.62f, oy = 0.4f)
        Canvas(Modifier.align(Alignment.Center).size(38.dp)) {
            drawCircle(Color.Black.copy(alpha = 0.6f))
            val p = Path().apply {
                moveTo(size.width * 0.4f, size.height * 0.3f); lineTo(size.width * 0.72f, size.height * 0.5f)
                lineTo(size.width * 0.4f, size.height * 0.7f); close()
            }
            drawPath(p, Color.White)
        }
        if (active == 1) {
            Row(Modifier.align(Alignment.TopStart).padding(6.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.55f)).padding(horizontal = 6.dp, vertical = 3.dp)) {
                BasicText(com.frameout.core.util.FrameStrings.mediaLive().uppercase(LocalLocale.current.platformLocale), style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White))
            }
        }
        Chips("MP4", "Live", "GIF", "WebP", modifier = Modifier.align(Alignment.BottomCenter).padding(6.dp), active = active)
    }
}

internal class Crop(val img: String, val zoom: Float, val ox: Float, val oy: Float, val mode: Int)
private const val CAT = "home_img_cat"
private const val TURTLE = "home_img_turtle"
private const val KITTEN = "home_img_kitten"
private const val BIRDS = "home_img_birds"
private const val TREES = "home_img_trees"
internal val SheetCrops = listOf(
    Crop(TURTLE, 1f, .5f, .5f, 0), Crop(TREES, 2f, .2f, .55f, 0), Crop(CAT, 2.6f, .54f, .43f, 0),
    Crop(BIRDS, 2.1f, .67f, .26f, 0), Crop(KITTEN, 2.4f, .5f, .5f, 0), Crop(TURTLE, 3f, .18f, .25f, 1),
    Crop(TREES, 1.4f, .7f, .5f, 2), Crop(CAT, 3.3f, .71f, .31f, 0), Crop(KITTEN, 3f, .22f, .9f, 1)
)

@Composable
private fun SheetFace() {
    val bw = remember { ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) }
    val vivid = remember { ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(1.7f) }) }
    Column(Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)).background(Color(0xFFF4F6F5)).padding(7.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Box(Modifier.height(5.dp).fillMaxWidth(0.45f).clip(CircleShape).background(Mint))
        for (row in 0 until 3) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                for (col in 0 until 3) {
                    val c = SheetCrops[row * 3 + col]
                    HomeImage(
                        c.img, Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(2.dp)),
                        c.zoom, c.ox, c.oy, when (c.mode) { 1 -> bw; 2 -> vivid; else -> null }
                    )
                }
            }
        }
    }
}

@Composable
private fun ImagesFace() {
    Box(Modifier.fillMaxSize()) {
        val tiles = listOf(Triple(-9f, -0.52f, Crop(CAT, 1f, .5f, .6f, 0)), Triple(0f, 0f, Crop(TURTLE, 2.4f, .62f, .38f, 0)), Triple(9f, 0.52f, Crop(BIRDS, 2.1f, .67f, .26f, 0)))
        tiles.forEach { (rot, dx, c) ->
            HomeImage(
                c.img,
                Modifier.align(Alignment.Center).fillMaxHeight(0.8f).width(54.dp)
                    .graphicsLayer { rotationZ = rot; translationX = size.width * dx }
                    .shadow(6.dp, RoundedCornerShape(8.dp))
                    .border(2.dp, Color.White, RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp)),
                c.zoom, c.ox, c.oy
            )
        }
        Chips("ZIP", "PDF", modifier = Modifier.align(Alignment.BottomEnd).padding(2.dp))
    }
}

@Composable
internal fun ExportFaces(phase: Float, modifier: Modifier) {
    val density = LocalDensity.current.density
    Box(modifier) {
        for (i in 0 until 3) {
            val l = ((phase - i) % 3f + 3f) % 3f
            val p = when { l >= 1f -> 0f; l < 0.12f -> l / 0.12f; l > 0.88f -> (1f - l) / 0.12f; else -> 1f }
            if (p > 0.01f) {
                val rot = (1f - p) * (if (l < 0.5f) -80f else 80f)
                Box(Modifier.fillMaxSize().graphicsLayer { alpha = p; rotationX = rot; cameraDistance = 12f * density }) {
                    when (i) { 0 -> VideoFace(((phase % 3f) * 4f).toInt().coerceIn(0, 3)); 1 -> SheetFace(); else -> ImagesFace() }
                }
            }
        }
    }
}

