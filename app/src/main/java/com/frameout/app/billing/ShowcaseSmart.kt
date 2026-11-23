package com.frameout.app.billing

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.frameout.core.pro.ProShowcase
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.theme.FrameTheme
import kotlinx.coroutines.delay

private const val SCENES = 6
private const val HOLD_MS = 3800L
private const val BUILD_MS = 1500
private val StageH = 150.dp

internal val SmartMint = Color(0xFF4BE3A3)
internal val SmartMintInk = Color(0xFF06251A)
internal val SmartCoral = Color(0xFFFF8D9A)
internal val SmartCoralInk = Color(0xFF3A0A14)
internal val SmartLilac = Color(0xFFC9C6FF)
internal val SmartLilacInk = Color(0xFF1B1250)
internal val SmartCardShape = RoundedCornerShape(10.dp)

internal fun smartRamp(p: Float, from: Float, to: Float): Float = ((p - from) / (to - from)).coerceIn(0f, 1f)

@Composable
internal fun SmartShowcase() {
    val reduce = LocalSystemA11y.current.reduceMotion
    val names = remember { ProShowcase.smartNames() }
    var scene by remember { mutableIntStateOf(0) }
    LaunchedEffect(scene, reduce) {
        if (!reduce) {
            delay(HOLD_MS)
            scene = (scene + 1) % SCENES
        }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.fillMaxWidth().height(StageH), Alignment.Center) {
            Crossfade(scene, animationSpec = tween(260), label = "smartScene") { s -> SceneHost(s, reduce) }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.height(20.dp), Alignment.Center) {
                Crossfade(scene, animationSpec = tween(220), label = "smartCaption") { i ->
                    val text = names.getOrNull(i)
                    if (text != null) {
                        FrameText(
                            text,
                            style = FrameTheme.typography.label,
                            color = FrameTheme.colors.primary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            Row(Modifier.padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(SCENES) { i -> Dot(on = i == scene) { scene = i } }
            }
            FrameText(
                ProShowcase.smartCount(),
                style = FrameTheme.typography.titleSmall.copy(textDirection = TextDirection.Content),
                color = FrameTheme.colors.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun Dot(on: Boolean, onClick: () -> Unit) {
    val w by animateDpAsState(if (on) 18.dp else 6.dp, tween(220), label = "dotW")
    Box(Modifier.size(width = 26.dp, height = 20.dp).clickable(onClick = onClick), Alignment.Center) {
        Box(
            Modifier
                .size(w, 6.dp)
                .clip(CircleShape)
                .background(if (on) FrameTheme.colors.primary else FrameTheme.colors.onSurface.copy(alpha = 0.25f))
        )
    }
}

@Composable
private fun SceneHost(scene: Int, reduce: Boolean) {
    val progress = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduce) progress.animateTo(1f, tween(BUILD_MS, delayMillis = 450, easing = FastOutSlowInEasing))
    }
    val p = progress.value
    when (scene) {
        0 -> SmartBlurryScene(p)
        1 -> SmartDuplicateScene(p)
        2 -> SmartPeopleScene(p)
        3 -> SmartSceneChangeScene(p)
        4 -> SmartSlidesScene(p)
        else -> SmartSharpestScene(p)
    }
}

@Composable
internal fun SmartPic(
    res: Int,
    w: Dp,
    h: Dp,
    modifier: Modifier = Modifier,
    dim: Float = 0f,
    lift: Float = 0f,
    blur: Dp = 0.dp,
    ring: Float = 0f,
    shape: Shape = SmartCardShape,
    overlay: @Composable BoxScope.() -> Unit = {}
) {
    Box(
        modifier
            .size(w, h)
            .graphicsLayer {
                alpha = 1f - 0.62f * dim
                translationY = -6.dp.toPx() * lift
                val s = 1f + 0.06f * lift
                scaleX = s
                scaleY = s
            }
    ) {
        Image(
            painter = painterResource(res),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .then(if (blur > 0.dp) Modifier.blur(blur, BlurredEdgeTreatment.Rectangle) else Modifier)
                .clip(shape)
                .border(1.dp, Color.White.copy(alpha = 0.22f), shape)
        )
        if (ring > 0f) Box(Modifier.fillMaxSize().border(2.dp, SmartMint.copy(alpha = ring), shape))
        overlay()
    }
}

@Composable
internal fun SmartPill(
    text: String,
    bg: Color,
    fg: Color,
    amount: Float,
    modifier: Modifier = Modifier,
    icon: Int? = null
) {
    if (amount <= 0.01f) return
    Row(
        modifier
            .graphicsLayer {
                alpha = amount
                val s = 0.6f + 0.4f * amount
                scaleX = s
                scaleY = s
            }
            .height(20.dp)
            .clip(CircleShape)
            .background(bg)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (icon != null) Image(painterResource(icon), null, Modifier.size(11.dp), colorFilter = ColorFilter.tint(fg))
        if (text.isNotEmpty()) FrameText(text, style = FrameTheme.typography.label, color = fg)
    }
}

@Composable
internal fun BoxScope.SmartVerdict(text: String, good: Boolean, amount: Float) {
    SmartPill(
        text,
        bg = if (good) SmartMint else SmartCoral,
        fg = if (good) SmartMintInk else SmartCoralInk,
        amount = amount,
        icon = if (good) FrameIcons.Check else null,
        modifier = Modifier.align(Alignment.BottomCenter).offset(y = 10.dp)
    )
}
