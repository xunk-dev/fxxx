package com.frameout.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

private const val BrandDeepen = 0.34f

internal val GraphiteBrand = listOf(Color(0xFF2E2E2E), Color(0xFF0C0C0C))
internal val GraphiteFab = listOf(Color(0xFF3C3C3C), Color(0xFF1E1E1E), Color(0xFF080808))

internal val GraphiteBrandDark = listOf(Color(0xFF484848), Color(0xFF262626))
internal val GraphiteFabDark = listOf(Color(0xFF2E2E2E), Color(0xFF191919), Color(0xFF0B0B0B))

internal val GraphiteProgressDark = listOf(Color(0xFFF2F2F2), Color(0xFFBDBDBD))
internal val GraphiteProgressLight = listOf(Color(0xFF2B2B2B), Color(0xFF6B6B6B))

object FrameGradients {
    val Brand: Brush
        @Composable get() {
            val c = FrameTheme.colors
            return remember(c.accentA, c.accentB, c.neutral, c.isDark) {
                if (c.neutral) Brush.horizontalGradient(if (c.isDark) GraphiteBrandDark else GraphiteBrand)
                else Brush.horizontalGradient(listOf(lerp(c.accentA, Color.Black, BrandDeepen), lerp(c.accentB, Color.Black, BrandDeepen)))
            }
        }

    val Progress: Brush
        @Composable get() {
            val c = FrameTheme.colors
            return remember(c.accentA, c.accentC, c.neutral, c.isDark) {
                when {
                    c.neutral -> Brush.horizontalGradient(if (c.isDark) GraphiteProgressDark else GraphiteProgressLight)
                    else -> Brush.horizontalGradient(listOf(c.accentA, c.accentC))
                }
            }
        }

    val Active: Brush
        @Composable get() {
            val c = FrameTheme.colors
            return remember(c.accentA, c.accentB, c.neutral, c.isDark) {
                if (c.neutral && c.isDark) Brush.horizontalGradient(GraphiteProgressDark)
                else if (c.neutral) Brush.horizontalGradient(GraphiteBrand)
                else Brush.horizontalGradient(listOf(lerp(c.accentA, Color.Black, BrandDeepen), lerp(c.accentB, Color.Black, BrandDeepen)))
            }
        }

    val Fab: Brush
        @Composable get() {
            val c = FrameTheme.colors
            return remember(c.accentA, c.accentB, c.accentC, c.neutral, c.isDark) {
                if (c.neutral) Brush.linearGradient(if (c.isDark) GraphiteFabDark else GraphiteFab) else Brush.linearGradient(listOf(c.accentC, c.accentA, c.accentB))
            }
        }
}

@Composable
fun frameOnActive(): Color {
    val c = FrameTheme.colors
    return if (c.neutral && c.isDark) Color(0xFF0A0A0A) else Color.White
}

@Composable
fun frameHairline(): Color {
    val c = FrameTheme.colors
    return when {
        c.neutral && c.isDark -> Color.White.copy(alpha = 0.18f)
        c.neutral -> Color.Black.copy(alpha = 0.16f)
        c.isDark -> Color.White.copy(alpha = 0.13f)
        else -> Color.Black.copy(alpha = 0.08f)
    }
}

fun Modifier.frameAmbientGlow(): Modifier = composed {
    val colors = FrameTheme.colors
    if (colors.neutral) return@composed Modifier.frameMonoAmbient(colors.isDark)
    val strength = if (colors.isDark) 1f else 0.55f
    Modifier.drawWithCache {
        val radius = size.width * 0.85f
        fun blob(color: Color, alpha: Float, center: Offset): Pair<Brush, Offset> =
            Brush.radialGradient(listOf(color.copy(alpha = alpha * strength), Color.Transparent), center, radius) to center
        val blobs = listOf(
            blob(colors.accentA, 0.30f, Offset(size.width, 0f)),
            blob(colors.accentC, 0.20f, Offset(0f, size.height * 0.35f)),
            blob(colors.accentB, 0.24f, Offset(size.width, size.height * 0.9f))
        )
        onDrawBehind { blobs.forEach { (brush, center) -> drawCircle(brush, radius, center) } }
    }
}

fun Modifier.frameMonoAmbient(dark: Boolean): Modifier = drawWithCache {
    val w = size.width
    val h = size.height
    val spot: Brush
    val bounce: Brush
    if (dark) {
        spot = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.09f), Color.White.copy(alpha = 0.025f), Color.Transparent), Offset(w * 0.5f, -h * 0.04f), w * 1.05f)
        bounce = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.045f), Color.Transparent), Offset(w * 0.95f, h), w * 0.85f)
    } else {
        spot = Brush.radialGradient(listOf(Color.Black.copy(alpha = 0.055f), Color.Transparent), Offset(w * 0.5f, h * 1.04f), w * 1.1f)
        bounce = Brush.radialGradient(listOf(Color.Black.copy(alpha = 0.025f), Color.Transparent), Offset(0f, h * 0.1f), w * 0.8f)
    }
    onDrawBehind {
        drawRect(spot)
        drawRect(bounce)
    }
}

private const val CardTopAlpha = 0.66f
private const val CardBottomAlpha = 0.52f

@Composable
fun frameCardBrush(tint: Color = Color.Transparent, tintAmount: Float = 0f): Brush {
    val colors = FrameTheme.colors
    val top: Color
    val bottom: Color
    if (colors.neutral) {
        top = if (colors.isDark) lerp(colors.container, Color.White, 0.06f) else colors.surface
        bottom = colors.container
    } else if (colors.isDark) {
        top = lerp(colors.container, Color.White, 0.05f).copy(alpha = CardTopAlpha)
        bottom = colors.container.copy(alpha = CardBottomAlpha)
    } else {
        top = colors.surface
        bottom = lerp(colors.surface, colors.background, 0.65f)
    }
    val amount = tintAmount.coerceIn(0f, 1f)
    return Brush.linearGradient(listOf(lerp(top, tint.copy(alpha = top.alpha), amount), bottom))
}

@Composable
fun frameCardEdge(accent: Color = Color.Unspecified, accentAlpha: Float = 0f): Brush {
    val dark = FrameTheme.colors.isDark
    val mono = FrameTheme.colors.neutral
    val hi = if (dark) Color.White.copy(alpha = if (mono) 0.36f else 0.14f) else Color.Black.copy(alpha = if (mono) 0.22f else 0.09f)
    val lo = if (dark) Color.White.copy(alpha = if (mono) 0.07f else 0.06f) else Color.Black.copy(alpha = if (mono) 0.09f else 0.05f)
    return if (accent == Color.Unspecified) Brush.verticalGradient(listOf(hi, lo))
    else Brush.verticalGradient(listOf(accent.copy(alpha = accentAlpha), accent.copy(alpha = accentAlpha * 0.35f)))
}
