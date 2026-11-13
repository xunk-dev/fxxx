package com.frameout.app.billing

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameHairline
import kotlin.math.sin

private const val CYCLE_MS = 5600
private const val APPEAR_SPAN = 0.08f
private const val SWEEP_FROM = 0.28f
private const val SWEEP_SPAN = 0.34f
private const val BADGE_FROM = 0.60f
private const val BADGE_SPAN = 0.20f
private const val CHECK_FROM = 0.64f
private const val CHECK_SPAN = 0.18f
private const val FADE_FROM = 0.9f
private const val STILL_PHASE = 0.85f
private const val SWEEP_START_DP = 30f
private const val SWEEP_END_DP = 122f
private const val TWO_PI = 6.2831855f
private val AdAmber = Color(0xFFFFB02E)
private val AdRed = Color(0xFFFF5F5F)

@Composable
private fun rememberAdPhase(reduce: Boolean): State<Float> {
    if (reduce) return remember { mutableFloatStateOf(STILL_PHASE) }
    val transition = rememberInfiniteTransition(label = "noAds")
    return transition.animateFloat(0f, 1f, infiniteRepeatable(tween(CYCLE_MS, easing = LinearEasing)), label = "noAdsValue")
}

private fun smooth(x: Float): Float {
    val t = x.coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

private fun backOut(x: Float): Float {
    val t = x.coerceIn(0f, 1f) - 1f
    return 1f + 2.7f * t * t * t + 1.7f * t * t
}

private fun appear(phase: Float): Float = smooth(phase / APPEAR_SPAN)

private fun sweepK(phase: Float): Float = smooth((phase - SWEEP_FROM) / SWEEP_SPAN)

private fun badgeK(phase: Float): Float = ((phase - BADGE_FROM) / BADGE_SPAN).coerceIn(0f, 1f)

private fun tail(phase: Float): Float = 1f - smooth((phase - FADE_FROM) / (1f - FADE_FROM))

private fun sweepDp(phase: Float): Float = SWEEP_START_DP + (SWEEP_END_DP - SWEEP_START_DP) * sweepK(phase)

@Composable
internal fun NoAdsShowcase() {
    val reduce = LocalSystemA11y.current.reduceMotion
    val phase = rememberAdPhase(reduce)
    val colors = FrameTheme.colors
    val shape = RoundedCornerShape(18.dp)
    Box(Modifier.size(width = 150.dp, height = 122.dp), Alignment.Center) {
        Column(
            Modifier
                .size(width = 78.dp, height = 122.dp)
                .background(colors.onSurface.copy(alpha = 0.07f), shape)
                .border(1.5.dp, frameHairline(), shape)
                .padding(8.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)
        ) {
            Box(Modifier.fillMaxWidth(0.5f).height(5.dp).background(colors.onSurface.copy(alpha = 0.25f), CircleShape))
            Box(Modifier.fillMaxWidth().height(40.dp).background(Brush.linearGradient(listOf(colors.accentC, colors.accentA, colors.accentB)), RoundedCornerShape(8.dp)))
            Box(Modifier.fillMaxWidth().height(5.dp).background(colors.onSurface.copy(alpha = 0.18f), CircleShape))
            Box(Modifier.fillMaxWidth(0.7f).height(5.dp).background(colors.onSurface.copy(alpha = 0.18f), CircleShape))
            Box(Modifier.fillMaxWidth(0.85f).height(5.dp).background(colors.onSurface.copy(alpha = 0.18f), CircleShape))
        }
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = appear(phase.value) }
                .drawWithContent {
                    val left = sweepDp(phase.value).dp.toPx()
                    clipRect(left = left) { this@drawWithContent.drawContent() }
                }
        ) {
            BannerAd()
            FullAd(phase)
        }
        SweepBar(phase)
        Shield(phase)
    }
}

@Composable
private fun BoxScope.BannerAd() {
    Box(
        Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 6.dp)
            .size(width = 64.dp, height = 20.dp)
            .background(AdAmber, RoundedCornerShape(5.dp)),
        Alignment.Center
    ) { AdLabel(Color(0xFF3A2400)) }
}

@Composable
private fun BoxScope.FullAd(phase: State<Float>) {
    Box(
        Modifier
            .align(Alignment.Center)
            .size(width = 58.dp, height = 78.dp)
            .graphicsLayer {
                val p = phase.value
                rotationZ = if (p < SWEEP_FROM) sin(p / SWEEP_FROM * TWO_PI * 2f) * 1.5f else 0f
            }
            .background(Brush.verticalGradient(listOf(AdAmber, AdRed)), RoundedCornerShape(10.dp))
            .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
        Alignment.Center
    ) {
        AdLabel(Color.White)
        Box(Modifier.align(Alignment.TopEnd).padding(4.dp).size(12.dp).background(Color.Black.copy(alpha = 0.35f), CircleShape))
    }
}

@Composable
private fun SweepBar(phase: State<Float>) {
    val colors = FrameTheme.colors
    Canvas(Modifier.fillMaxSize()) {
        val p = phase.value
        val k = sweepK(p)
        if (k <= 0f || k >= 1f) return@Canvas
        val edge = (minOf(k, 1f - k) / 0.12f).coerceIn(0f, 1f)
        val x = sweepDp(p).dp.toPx()
        val band = 16.dp.toPx()
        drawRect(
            Brush.horizontalGradient(listOf(Color.Transparent, colors.accentC.copy(alpha = 0.35f * edge)), startX = x - band, endX = x),
            Offset(x - band, 0f),
            Size(band, size.height)
        )
        drawLine(Color.White.copy(alpha = edge), Offset(x, 0f), Offset(x, size.height), 1.5.dp.toPx(), StrokeCap.Round)
    }
}

@Composable
private fun BoxScope.Shield(phase: State<Float>) {
    val colors = FrameTheme.colors
    Box(
        Modifier
            .align(Alignment.BottomEnd)
            .padding(bottom = 2.dp)
            .size(38.dp)
            .graphicsLayer {
                val b = badgeK(phase.value)
                val scale = backOut(b)
                scaleX = scale
                scaleY = scale
                alpha = (b * 3f).coerceIn(0f, 1f) * tail(phase.value)
            }
            .background(proBrush(), CircleShape)
            .border(2.dp, colors.surface, CircleShape),
        Alignment.Center
    ) {
        AnimatedCheck(smooth((phase.value - CHECK_FROM) / CHECK_SPAN), proOnBrush(), Modifier.size(18.dp))
    }
}

@Composable
private fun AdLabel(color: Color) {
    FrameText(com.frameout.core.pro.ProStrings.adBadge(), FrameTheme.typography.label.copy(fontSize = 11.sp, fontWeight = FontWeight.ExtraBold), color)
}
