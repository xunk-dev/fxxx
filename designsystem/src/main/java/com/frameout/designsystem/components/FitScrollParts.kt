package com.frameout.designsystem.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private const val EDGE_FADE_DP = 8
private const val MAX_FADE = 0.5f

@Composable
internal fun rememberMarqueeScroll(text: AnnotatedString): ScrollState {
    val scroll = rememberScrollState()
    LaunchedEffect(text, scroll.maxValue) {
        val max = scroll.maxValue
        if (max <= 0) return@LaunchedEffect
        val duration = (max * MARQUEE_MS_PER_PX).coerceAtLeast(MARQUEE_MIN_MS)
        while (true) {
            delay(MARQUEE_PAUSE_MS)
            scroll.animateScrollTo(max, tween(duration, easing = LinearEasing))
            delay(MARQUEE_PAUSE_MS)
            scroll.animateScrollTo(0, tween(duration, easing = LinearEasing))
        }
    }
    return scroll
}

@Composable
internal fun Modifier.edgeFade(): Modifier {
    val fadePx = with(LocalDensity.current) { EDGE_FADE_DP.dp.toPx() }
    return this
        .clipToBounds()
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val f = (fadePx / size.width).coerceIn(0f, MAX_FADE)
            drawRect(
                Brush.horizontalGradient(
                    0f to Color.Transparent,
                    f to Color.Black,
                    1f - f to Color.Black,
                    1f to Color.Transparent
                ),
                blendMode = BlendMode.DstIn
            )
        }
}
