package com.frameout.app.onboarding

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.frameout.designsystem.a11y.LocalSystemA11y

internal object OnbColors {
    val palette: OnbPalette
        @Composable @ReadOnlyComposable get() = LocalOnb.current
    val Indigo: Color
        @Composable @ReadOnlyComposable get() = LocalOnb.current.Indigo
    val Violet: Color
        @Composable @ReadOnlyComposable get() = LocalOnb.current.Violet
    val Cyan: Color
        @Composable @ReadOnlyComposable get() = LocalOnb.current.Cyan
    val CyanSoft: Color
        @Composable @ReadOnlyComposable get() = LocalOnb.current.CyanSoft
    val Ink: Color
        @Composable @ReadOnlyComposable get() = LocalOnb.current.Ink
    val Night: Color
        @Composable @ReadOnlyComposable get() = LocalOnb.current.Night
    val Ring: Color
        @Composable @ReadOnlyComposable get() = LocalOnb.current.Ring
}

@Composable
internal fun rememberLoop(
    durationMs: Int,
    still: Float,
    reverse: Boolean,
    label: String,
    ease: Boolean = false
): State<Float> {
    val reduceMotion = LocalSystemA11y.current.reduceMotion
    return if (reduceMotion) {
        remember(still) { mutableFloatStateOf(still) }
    } else {
        rememberInfiniteTransition(label = label).animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMs, easing = if (ease) FastOutSlowInEasing else LinearEasing),
                repeatMode = if (reverse) RepeatMode.Reverse else RepeatMode.Restart
            ),
            label = label
        )
    }
}
