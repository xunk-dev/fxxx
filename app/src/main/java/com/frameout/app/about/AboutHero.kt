package com.frameout.app.about

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.frameout.app.BuildConfig
import com.frameout.app.splash.BrandGlyph
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.ModeStrings
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.components.FrameWordmark
import com.frameout.designsystem.glass.GlassSurface
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import kotlinx.coroutines.delay

private val GlowHeight = 200.dp
private val GlyphHeight = 108.dp

@Composable
internal fun AboutHero() {
    val reduce = LocalSystemA11y.current.reduceMotion
    val lock = remember { Animatable(if (reduce) 1f else 0f) }
    val textIn = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduce) lock.animateTo(1f, spring(dampingRatio = 0.68f, stiffness = Spring.StiffnessLow))
    }
    LaunchedEffect(Unit) {
        if (reduce) return@LaunchedEffect
        delay(280)
        textIn.animateTo(1f, tween(560, easing = FastOutSlowInEasing))
    }
    Column(
        Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HeroGlyph(lock.value)
        HeroTexts(textIn.value)
    }
}

@Composable
private fun HeroGlyph(progress: Float) {
    val colors = FrameTheme.colors
    val strength = if (colors.isDark) 1f else 0.6f
    val k = progress.coerceIn(0f, 1f) * strength
    Box(
        Modifier
            .fillMaxWidth()
            .height(GlowHeight)
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        listOf(colors.accentA.copy(alpha = 0.42f * k), colors.accentB.copy(alpha = 0.14f * k), Color.Transparent),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = size.width * 0.5f
                    )
                )
            },
        contentAlignment = Alignment.Center
    ) {
        BrandGlyph(
            GlyphHeight,
            Modifier.graphicsLayer {
                val p = progress.coerceIn(0f, 1.1f)
                scaleX = 0.78f + 0.22f * p
                scaleY = 0.78f + 0.22f * p
                alpha = progress.coerceIn(0f, 1f)
            }
        )
    }
}

@Composable
private fun HeroTexts(enter: Float) {
    val colors = FrameTheme.colors
    val e = enter.coerceIn(0f, 1f)
    Column(
        Modifier.fillMaxWidth().graphicsLayer {
            alpha = e
            translationY = (1f - e) * 14.dp.toPx()
        },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FrameWordmark(height = 30.dp, description = FrameStrings.appName())
        FrameText(
            ModeStrings.aboutTagline(),
            style = FrameTheme.typography.body,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 10.dp, start = 24.dp, end = 24.dp)
        )
        GlassSurface(shape = FrameShapes.pill, modifier = Modifier.padding(top = 14.dp)) {
            FrameText(
                ModeStrings.aboutVersion(BuildConfig.VERSION_NAME),
                style = FrameTheme.typography.monoSmall,
                color = colors.primary,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }
    }
}
