package com.frameout.app.onboarding

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.app.R
import com.frameout.core.R as CoreR
import com.frameout.core.pro.ProShowcase
import com.frameout.core.util.SmoothScale
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.glass.frameGlow
import com.frameout.designsystem.theme.FrameTheme
import kotlin.math.max

internal const val PickHeightFactor = 1.16f
internal val PickExtraHeight = 0.dp

private const val SceneCount = 5
private const val SceneMs = 3300
private val SmPics = intArrayOf(
    R.drawable.pick_bmw_1, R.drawable.pick_bmw_1, R.drawable.pick_bmw_2, R.drawable.pick_bmw_3, R.drawable.pick_person,
    R.drawable.pick_bmw_5, R.drawable.pick_bmw_6, R.drawable.pick_bmw_7, R.drawable.pick_bmw_8
)
private val SmBlurTiles = setOf(2, 6, 7)
private const val SmDupTile = 1
private const val SmPeopleTile = 4
private const val SmBestTile = 5

private fun sceneOf(p: Float) = (p * SceneCount).toInt().coerceIn(0, SceneCount - 1)

private fun strengthOf(p: Float): Float {
    val s = p * SceneCount
    val q = s - s.toInt().coerceIn(0, SceneCount - 1)
    return ease(seg(q, 0.18f, 0.4f)) * (1f - ease(seg(q, 0.88f, 1f)))
}

private fun rejected(scene: Int, i: Int) = when (scene) {
    0 -> i in SmBlurTiles
    1 -> i == SmDupTile
    2 -> i != SmPeopleTile
    3 -> i % 3 != 0
    else -> i != SmBestTile
}

@Composable
internal fun PickStage(width: Dp, modifier: Modifier = Modifier) {
    val colors = FrameTheme.colors
    val phase = rememberLoop(durationMs = SceneCount * SceneMs, still = 0.5f, reverse = false, label = "pickSmart")
    val names = remember { ProShowcase.smartNames() }
    val keepFirst = remember { ProShowcase.smartKeepFirst() }
    val scene = remember { derivedStateOf { sceneOf(phase.value) } }.value
    val shape = RoundedCornerShape(26.dp)
    Column(modifier.pickStageStyle(width, colors.primary, shape), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Pill(stringResource(CoreR.string.onboarding_pick_local), colors.success)
            Pill(stringResource(CoreR.string.pro_name_smart), Color.White.copy(alpha = 0.85f))
        }
        SmartGrid(phase, names)
        PickCaption(scene, if (scene == 3) keepFirst else names.getOrElse(if (scene == 4) 5 else scene) { "" })
        SceneDots(scene, colors.primary)
    }
}

private fun Modifier.pickStageStyle(width: Dp, primary: Color, shape: Shape): Modifier = this
    .width(width)
    .frameGlow(primary, { 0.4f }, 24.dp, { 0.1f })
    .clip(shape)
    .background(Brush.linearGradient(listOf(primary.copy(alpha = 0.3f), Color.Black.copy(alpha = 0.4f), primary.copy(alpha = 0.16f))))
    .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.55f), primary.copy(alpha = 0.22f))), shape)
    .drawWithContent {
        drawContent()
        drawRect(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.24f), Color.Transparent), Offset.Zero, Offset(size.width * 0.9f, size.height * 0.45f)))
    }
    .padding(12.dp)

@Composable
private fun SmartGrid(phase: State<Float>, names: List<String>) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(3) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(3) { col -> SmartTile(row * 3 + col, phase, names, Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun PickCaption(scene: Int, text: String) {
    Crossfade(text, animationSpec = tween(260), label = "pickCaption", modifier = Modifier.fillMaxWidth().height(26.dp)) { t ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            FrameText(
                t,
                style = FrameTheme.typography.body.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White),
                color = Color.White, textAlign = TextAlign.Center, maxLines = 1
            )
        }
    }
}

@Composable
private fun SceneDots(scene: Int, primary: Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
        repeat(SceneCount) { d ->
            Box(
                Modifier.height(5.dp).width(if (d == scene) 22.dp else 5.dp).clip(CircleShape)
                    .background(if (d == scene) primary else Color.White.copy(alpha = 0.25f))
            )
        }
    }
}

@Composable
private fun Pill(text: String, color: Color) {
    Row(
        Modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.35f)).border(1.dp, color.copy(alpha = 0.45f), CircleShape).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(Modifier.width(6.dp).height(6.dp).clip(CircleShape).background(color))
        FrameText(text, style = FrameTheme.typography.label.copy(fontSize = 11.sp, color = Color.White), color = Color.White, maxLines = 1)
    }
}

@Composable
private fun SmartTile(i: Int, phase: State<Float>, names: List<String>, modifier: Modifier) {
    val colors = FrameTheme.colors
    val shape = RoundedCornerShape(13.dp)
    val img = ImageBitmap.imageResource(SmPics[i])
    val blurred = if (i in SmBlurTiles) remember(img) { blurredCopy(img) } else null
    fun on(scene: Int, p: Float) = if (sceneOf(p) == scene) strengthOf(p) else 0f
    Box(
        modifier
            .aspectRatio(1.3f)
            .graphicsLayer {
                val p = phase.value
                val s = sceneOf(p)
                val e = strengthOf(p)
                val rej = if (rejected(s, i)) e else 0f
                alpha = 1f - 0.6f * rej
                val win = if (!rejected(s, i)) e else 0f
                val grow = if (s == 4 || s == 2) 0.06f * win else 0f
                scaleX = 1f + grow; scaleY = 1f + grow
            }
            .clip(shape)
            .border(1.dp, Color.White.copy(alpha = 0.28f), shape)
    ) {
        Image(img, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        if (blurred != null) {
            Image(blurred, null, Modifier.fillMaxSize().graphicsLayer { alpha = on(0, phase.value) }, contentScale = ContentScale.Crop)
        }
        Canvas(Modifier.fillMaxSize()) { drawSmartMarks(i, sceneOf(phase.value), strengthOf(phase.value), colors.success) }
        TileTag(i in SmBlurTiles, ProShowcase.smartBlurry(), Color(0xFFE5484D)) { on(0, phase.value) }
        TileTag(i == SmDupTile, ProShowcase.smartDup(2), Color(0xFFF5A524)) { on(1, phase.value) }
        TileTag(i == SmPeopleTile, names.getOrElse(2) { "" }, colors.success) { on(2, phase.value) }
        TileTag(i % 3 == 0, "${i / 3 + 1}", colors.success) { on(3, phase.value) }
        TileTag(i == SmBestTile, ProShowcase.smartBest(), colors.success) { on(4, phase.value) }
    }
}

private fun DrawScope.drawSmartMarks(i: Int, s: Int, e: Float, success: Color) {
    if (s == 3 && i % 3 == 0) {
        drawRoundRect(success.copy(alpha = e), cornerRadius = CornerRadius(13.dp.toPx()), style = Stroke(2.dp.toPx()))
    }
    if (s == 4 && i == SmBestTile) {
        drawRoundRect(success.copy(alpha = e), cornerRadius = CornerRadius(13.dp.toPx()), style = Stroke(2.5.dp.toPx()))
    }
    if (s == 2 && i == SmPeopleTile) {
        val w = size.width * 0.24f; val h = size.height * 0.74f
        val x0 = size.width * 0.215f; val y0 = size.height * 0.15f
        val c = success.copy(alpha = e); val k = w * 0.3f; val st = 2.dp.toPx()
        for ((cx, cy, dx, dy) in listOf(floatArrayOf(x0, y0, 1f, 1f), floatArrayOf(x0 + w, y0, -1f, 1f), floatArrayOf(x0, y0 + h, 1f, -1f), floatArrayOf(x0 + w, y0 + h, -1f, -1f))) {
            drawLine(c, Offset(cx, cy), Offset(cx + dx * k, cy), st)
            drawLine(c, Offset(cx, cy), Offset(cx, cy + dy * k), st)
        }
    }
}

@Composable
private fun BoxScope.TileTag(show: Boolean, text: String, color: Color, alpha: () -> Float) {
    if (!show) return
    Box(
        Modifier.align(Alignment.BottomStart).padding(4.dp)
            .graphicsLayer { this.alpha = alpha() }
            .clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = 0.92f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        FrameText(text, style = FrameTheme.typography.label.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color.White), color = Color.White, maxLines = 1)
    }
}

private fun blurredCopy(src: ImageBitmap): ImageBitmap {
    val bitmap = src.asAndroidBitmap()
    val small = SmoothScale.scaled(bitmap, max(1, bitmap.width / 14), max(1, bitmap.height / 14), true)
    return SmoothScale.scaled(small, bitmap.width, bitmap.height, true).asImageBitmap()
}
