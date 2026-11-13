package com.frameout.app.home

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.Stroke
import com.frameout.designsystem.a11y.LocalSystemA11y
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.app.R
import com.frameout.core.util.HomeStrings
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameTheme
import kotlin.math.roundToInt

private fun Modifier.proSurface(pro: ProLook, flow: State<Float>): Modifier = drawBehind {
    val rtl = layoutDirection == LayoutDirection.Rtl
    drawRect(flowBrush(pro.stops.reversed(), flow.value))
    if (pro.jewel) {
        val bloomCenter = Offset(size.width * (if (rtl) 0.20f else 0.80f), size.height * 0.5f)
        val bloomRadius = size.width * 0.55f
        drawCircle(
            Brush.radialGradient(
                0f to pro.bloom.copy(alpha = 0.34f),
                0.5f to pro.bloom.copy(alpha = 0.10f),
                1f to pro.bloom.copy(alpha = 0f),
                center = bloomCenter,
                radius = bloomRadius
            ),
            bloomRadius,
            bloomCenter
        )
        drawRect(
            Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0f)),
                0f,
                size.height * 0.45f
            )
        )
        val warmCenter = Offset(size.width * 0.62f, size.height)
        val warmRadius = size.width * 0.46f
        drawCircle(
            Brush.radialGradient(listOf(pro.accentGlow.copy(alpha = 0.22f), pro.accentGlow.copy(alpha = 0f)), warmCenter, warmRadius),
            warmRadius,
            warmCenter
        )
        val depthCenter = Offset(if (rtl) size.width * 1.02f else -size.width * 0.02f, size.height)
        val depthRadius = size.width * 0.62f
        drawCircle(
            Brush.radialGradient(
                listOf(pro.vignette.copy(alpha = 0.55f), pro.vignette.copy(alpha = 0f)),
                depthCenter,
                depthRadius
            ),
            depthRadius,
            depthCenter
        )
    } else {
        drawRect(Brush.verticalGradient(listOf(pro.glow.copy(alpha = pro.glow.alpha * 0.55f), Color.Transparent), 0f, size.height * 0.5f))
        val c = Offset(size.width * (if (rtl) 0.20f else 0.80f), size.height * 0.5f)
        val r = size.width * 0.5f
        drawCircle(Brush.radialGradient(listOf(pro.glow, Color.Transparent), c, r), r, c)
    }
}

@Composable
private fun BoxScope.ProCopy(pro: ProLook, headline: String, height: Dp) {
    Column(
        Modifier
            .align(Alignment.TopStart)
            .fillMaxWidth(HomeMetrics.ProTextShare)
            .height(height)
            .padding(start = HomeMetrics.ProPadStart),
        verticalArrangement = Arrangement.Center
    ) {
        FitText(
            headline,
            style = FrameTheme.typography.display.copy(fontSize = 33.sp, lineHeight = 40.sp),
            color = pro.ink.copy(alpha = 0.85f),
            maxLines = 3,
            minScale = 0.6f
        )
    }
}

@Composable
private fun ProStrip(pro: ProLook, label: String, modifier: Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .height(HomeMetrics.ProStripHeight)
            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.14f), Color.White.copy(alpha = 0.06f))))
            .drawBehind {
                drawLine(Color.White.copy(alpha = 0.28f), Offset(0f, 0.5f), Offset(size.width, 0.5f), 1.dp.toPx())
            }
            .padding(horizontal = HomeMetrics.ProStripPad),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        FitText(
            label,
            style = FrameTheme.typography.label.copy(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
            color = pro.ink,
            modifier = Modifier.weight(1f),
            minScale = 0.7f
        )
        ProStripButton(pro)
    }
}

@Composable
private fun ProStripButton(pro: ProLook) {
    val ping = loop(0f, 1f, 2600, reverse = false)
    val still = LocalSystemA11y.current.reduceMotion
    Box(
        Modifier.size(HomeMetrics.ProStripButton).drawBehind {
            if (!still) {
                val p = ping.value
                drawCircle(
                    pro.pillBg.copy(alpha = 0.75f * (1f - p)),
                    radius = size.minDimension / 2f * (1f + p),
                    style = Stroke(2.dp.toPx())
                )
            }
        },
        Alignment.Center
    ) {
        Box(Modifier.matchParentSize().clip(CircleShape).background(pro.pillBg))
        Image(
            painterResource(FrameIconsExtra.Chevron),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            colorFilter = ColorFilter.tint(pro.pillInk)
        )
    }
}

@Composable
internal fun HomeProRow(onClick: () -> Unit, height: Dp, modifier: Modifier = Modifier) {
    val look = rememberHomeLook()
    val pro = look.pro
    val title = HomeStrings.modePro()
    val desc = HomeStrings.modeProDesc()
    val open = HomeStrings.modeProOpen()
    val shape = HomeMetrics.ProShape
    val flow = loop(0f, 1f, 9000)
    val angle = loop(0f, 360f, 16000, reverse = false)
    val breath = loop(0f, 1f, 3800)
    val lightInk = pro.ink.luminance() > 0.5f
    val ringA = if (lightInk) pro.bloom else pro.stops[2]
    val ringB = if (lightInk) lerp(pro.bloom, Color.White, 0.65f) else pro.stops[1]
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .homePressable(onClick)
            .shadow(14.dp, shape, ambientColor = pro.shadow.copy(alpha = 0.28f), spotColor = pro.shadow.copy(alpha = 0.55f))
            .clip(shape)
            .proSurface(pro, flow)
            .border(1.dp, Brush.verticalGradient(listOf(pro.edgeTop, pro.edgeBottom)), shape)
            .drawWithContent {
                drawContent()
                drawSweepRing(ringA, ringB, angle.value, HomeMetrics.ProRadius.toPx())
            }
            .semantics(mergeDescendants = true) { contentDescription = "$title. $desc. $open" }
    ) {
        ProArtGlow(pro, breath, height * HomeMetrics.ProArtRatio, Modifier.align(Alignment.TopEnd))
        ProArt(
            look,
            height * HomeMetrics.ProArtRatio,
            Modifier.align(Alignment.TopEnd).offset(x = HomeMetrics.ProArtBleed, y = HomeMetrics.ProArtLift).liquidFloat(-7f, 4200)
        )
        ProCopy(pro, desc, height - HomeMetrics.ProStripHeight)
        ProStrip(pro, open, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun ProArtGlow(pro: ProLook, breath: State<Float>, artSize: Dp, modifier: Modifier) {
    Box(
        modifier
            .offset(x = HomeMetrics.ProArtBleed, y = HomeMetrics.ProArtLift)
            .requiredSize(artSize)
            .drawBehind {
                val b = breath.value
                val r = size.maxDimension * (0.42f + 0.16f * b)
                val c = Offset(size.width * 0.5f, size.height * 0.5f)
                val tint = if (pro.jewel) pro.bloom else pro.glow.copy(alpha = 1f)
                drawCircle(
                    Brush.radialGradient(listOf(tint.copy(alpha = 0.30f + 0.28f * b), Color.Transparent), c, r),
                    r,
                    c
                )
            }
    )
}
