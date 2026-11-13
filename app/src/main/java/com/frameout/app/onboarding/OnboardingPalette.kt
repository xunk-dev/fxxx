package com.frameout.app.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.motion.FlowEase
import com.frameout.designsystem.theme.FrameAccent
import com.frameout.designsystem.theme.FrameAccents
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.ContrastGuard

private const val FADE_MS = 240
private const val SOFT_SATURATION = 0.71f
private const val SOFT_VALUE = 0.96f
private const val RING_SATURATION = 0.75f

@Immutable
internal data class OnbPalette(
    val Indigo: Color,
    val Violet: Color,
    val Cyan: Color,
    val CyanSoft: Color,
    val Ink: Color,
    val Night: Color,
    val Mid: Color,
    val Deep: Color,
    val Ring: Color,
    val OnSoft: Color
)

internal val LocalOnb = compositionLocalOf { onbPaletteOf(FrameAccents.Indigo) }

private fun brighten(color: Color, saturation: Float, value: Float): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    return Color.hsv(hsv[0], (hsv[1] * saturation).coerceIn(0f, 1f), value)
}

internal fun onbPaletteOf(accent: FrameAccent): OnbPalette {
    val soft = brighten(accent.c, SOFT_SATURATION, SOFT_VALUE)
    return OnbPalette(
        Indigo = accent.a,
        Violet = accent.b,
        Cyan = accent.c,
        CyanSoft = soft,
        Ink = lerp(accent.a, Color.Black, 0.87f),
        Night = lerp(accent.a, Color.Black, 0.77f),
        Mid = lerp(accent.a, Color.Black, 0.72f),
        Deep = lerp(accent.a, Color.Black, 0.80f),
        Ring = brighten(accent.a, RING_SATURATION, 1f),
        OnSoft = ContrastGuard.readableOn(soft, listOf(lerp(accent.c, Color.Black, 0.78f)))
    )
}

private fun lerpPalette(from: OnbPalette, to: OnbPalette, t: Float) = OnbPalette(
    Indigo = lerp(from.Indigo, to.Indigo, t),
    Violet = lerp(from.Violet, to.Violet, t),
    Cyan = lerp(from.Cyan, to.Cyan, t),
    CyanSoft = lerp(from.CyanSoft, to.CyanSoft, t),
    Ink = lerp(from.Ink, to.Ink, t),
    Night = lerp(from.Night, to.Night, t),
    Mid = lerp(from.Mid, to.Mid, t),
    Deep = lerp(from.Deep, to.Deep, t),
    Ring = lerp(from.Ring, to.Ring, t),
    OnSoft = lerp(from.OnSoft, to.OnSoft, t)
)

@Composable
private fun rememberOnbPalette(): OnbPalette {
    val accent = FrameTheme.accent
    val reduceMotion = LocalSystemA11y.current.reduceMotion
    val target = remember(accent) { onbPaletteOf(accent) }
    val progress = remember { Animatable(1f) }
    var from by remember { mutableStateOf(target) }
    var to by remember { mutableStateOf(target) }
    LaunchedEffect(target) {
        if (target == to) return@LaunchedEffect
        from = lerpPalette(from, to, progress.value)
        to = target
        progress.snapTo(0f)
        if (reduceMotion) progress.snapTo(1f) else progress.animateTo(1f, tween(FADE_MS, easing = FlowEase.InOut))
    }
    return lerpPalette(from, to, progress.value)
}

@Composable
internal fun OnbThemed(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalOnb provides rememberOnbPalette(),
        content = content
    )
}
