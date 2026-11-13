package com.frameout.app.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.roundToInt
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.theme.FrameTheme

@Composable
private fun tileTitleStyle() = FrameTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)

internal class ToolRowSpec(val title: String, val desc: String, val art: HomeArt, val tone: Color, val onClick: () -> Unit)

private class Aurora(
    val flow: State<Float>,
    val angle: State<Float>,
    val stage: List<Color>,
    val surface: List<Color>,
    val ringA: Color,
    val ringB: Color,
    val phase: Float
)

@Composable
private fun rememberAurora(spec: ToolRowSpec, look: HomeLook): Aurora {
    val flow = loop(0f, 1f, 7600 + spec.art.bobMs / 8)
    val angle = loop(0f, 360f, 12000 + spec.art.bobMs / 2, reverse = false)
    return remember(spec.tone, look, flow, angle) {
        val dark = look.dark
        val o = spec.tone.toOklch()
        val chroma = if (look.neutral) o.c else o.c.coerceIn(0.04f, 0.17f)
        val lightness = if (dark) floatArrayOf(0.30f, 0.38f, 0.43f, 0.35f) else floatArrayOf(0.90f, 0.84f, 0.88f, 0.82f)
        val hueShift = floatArrayOf(-18f, 0f, 24f, 48f)
        val k = if (dark) 1f else 0.6f
        val stage = List(4) { oklch(lightness[it], chroma * k, o.h + hueShift[it]) }
        val surface = stage.map { lerp(it, if (dark) Color.Black else Color.White, 0.4f).copy(alpha = if (dark) 0.62f else 0.60f) }
        Aurora(
            flow, angle, stage, surface,
            ringA = oklch(if (dark) 0.82f else 0.62f, chroma * 1.1f, o.h + 20f),
            ringB = oklch(if (dark) 0.90f else 0.72f, chroma * 0.9f, o.h + 60f),
            phase = spec.art.ordinal * 72f
        )
    }
}

internal fun DrawScope.flowBrush(colors: List<Color>, f: Float): Brush {
    val shift = f * size.width * 2.2f
    return Brush.linearGradient(
        colors,
        start = Offset(-shift, size.height * 0.2f),
        end = Offset(-shift + size.width * 3.2f, size.height * 0.8f),
        tileMode = TileMode.Mirror
    )
}

internal fun DrawScope.drawSweepRing(ringA: Color, ringB: Color, angleDeg: Float, radius: Float) {
    val sw = 1.6.dp.toPx()
    val clear = ringA.copy(alpha = 0f)
    val shader = android.graphics.SweepGradient(
        size.width / 2f, size.height / 2f,
        intArrayOf(clear.toArgb(), clear.toArgb(), ringA.toArgb(), ringB.toArgb(), clear.toArgb()),
        floatArrayOf(0f, 0.55f, 0.78f, 0.90f, 1f)
    )
    val m = android.graphics.Matrix()
    m.setRotate(angleDeg, size.width / 2f, size.height / 2f)
    shader.setLocalMatrix(m)
    drawRoundRect(
        ShaderBrush(shader),
        topLeft = Offset(sw / 2f, sw / 2f),
        size = Size(size.width - sw, size.height - sw),
        cornerRadius = CornerRadius(radius - sw / 2f),
        style = Stroke(sw)
    )
}

@Composable
private fun Modifier.tileSurface(spec: ToolRowSpec, look: HomeLook, a: Aurora): Modifier {
    val g = rememberGlassStyle()
    val shape = HomeMetrics.TileShape
    val tone = spec.tone
    val lift = if (look.dark) Modifier
    else Modifier.shadow(10.dp, shape, ambientColor = tone.copy(alpha = 0.12f), spotColor = tone.copy(alpha = 0.30f))
    return this
        .homePressable(spec.onClick)
        .then(lift)
        .clip(shape)
        .drawBehind {
            drawRect(Color.White.copy(alpha = if (look.dark) 0.05f else 0.50f))
            drawRect(flowBrush(a.surface, a.flow.value))
            val inset = HomeMetrics.TileRadius.toPx()
            drawLine(g.highlight, Offset(inset, 0.5f), Offset(size.width - inset, 0.5f), 1.dp.toPx())
        }
        .border(1.dp, g.border, shape)
        .drawWithContent {
            drawContent()
            drawSweepRing(a.ringA, a.ringB, a.angle.value + a.phase, HomeMetrics.TileRadius.toPx())
        }
        .semantics(mergeDescendants = true) { contentDescription = "${spec.title}. ${spec.desc}" }
}

private fun Modifier.auroraStage(spec: ToolRowSpec, look: HomeLook, a: Aurora): Modifier =
    this
        .clip(HomeMetrics.StageShape)
        .drawBehind {
            val f = a.flow.value
            drawRect(flowBrush(a.stage, f))
            val c = Offset(size.width * (-0.1f + 0.9f * f), size.height * (-0.2f + 0.7f * f))
            val r = size.width * 0.55f
            val light = lerp(spec.tone, Color.White, 0.55f).copy(alpha = if (look.dark) 0.40f else 0.35f)
            drawCircle(Brush.radialGradient(listOf(light, Color.Transparent), c, r), r, c)
        }

@Composable
private fun TileTexts(spec: ToolRowSpec, look: HomeLook, showDesc: Boolean, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(HomeMetrics.TileTitleGap)) {
        FitText(spec.title, style = tileTitleStyle(), color = look.title, minScale = 0.8f)
        if (showDesc) {
            FrameText(
                spec.desc,
                style = FrameTheme.typography.label.copy(fontSize = 12.sp, lineHeight = 16.sp),
                color = look.desc,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun ToolTile(spec: ToolRowSpec, look: HomeLook, modifier: Modifier) {
    val a = rememberAurora(spec, look)
    Column(modifier.tileSurface(spec, look, a).padding(HomeMetrics.TileInset)) {
        Box(Modifier.weight(1f).fillMaxWidth().auroraStage(spec, look, a)) {
            ToolArt(
                spec.art, spec.tone, look.dark,
                Modifier.align(Alignment.Center).fillMaxHeight(0.9f).aspectRatio(spec.art.aspect)
            )
        }
        TileTexts(spec, look, true, Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 4.dp))
    }
}

@Composable
internal fun CompareCompactTile(spec: ToolRowSpec, look: HomeLook, modifier: Modifier) =
    HorizontalTile(spec, look, modifier, wide = false)

@Composable
internal fun CompareWideTile(spec: ToolRowSpec, look: HomeLook, modifier: Modifier) =
    HorizontalTile(spec, look, modifier, wide = true)

@Composable
private fun HorizontalTile(spec: ToolRowSpec, look: HomeLook, modifier: Modifier, wide: Boolean) {
    val a = rememberAurora(spec, look)
    Row(
        modifier.tileSurface(spec, look, a).padding(HomeMetrics.TileInset),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.weight(if (wide) 0.44f else 0.46f).fillMaxHeight().auroraStage(spec, look, a)) {
            ToolArt(
                spec.art, spec.tone, look.dark,
                Modifier.align(Alignment.Center).fillMaxHeight(0.88f).aspectRatio(spec.art.aspect)
            )
        }
        TileTexts(spec, look, wide, Modifier.weight(if (wide) 0.56f else 0.54f).padding(horizontal = 12.dp))
    }
}
