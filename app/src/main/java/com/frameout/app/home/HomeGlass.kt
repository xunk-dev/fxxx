package com.frameout.app.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.frameout.designsystem.theme.ContrastGuard
import com.frameout.designsystem.theme.FrameTheme

private fun rr(x: Float, y: Float, w: Float, h: Float, r: Float): String =
    "M${x + r},${y}h${w - 2 * r}a$r,$r 0 0 1 $r,${r}v${h - 2 * r}a$r,$r 0 0 1 -$r,${r}h${-(w - 2 * r)}a$r,$r 0 0 1 -$r,${-r}v${-(h - 2 * r)}a$r,$r 0 0 1 $r,${-r}z"

private fun dot(cx: Float, cy: Float, r: Float): String =
    "M${cx - r},${cy}a$r,$r 0 1,0 ${2 * r},0a$r,$r 0 1,0 ${-2 * r},0z"

internal class GlyphPart(val path: String, val fill: Boolean = true)

internal enum class HomeGlyph(val parts: List<GlyphPart>) {
    Bolt(listOf(GlyphPart("M13 2 4 14h7l-1 8 9-12h-7z"))),

    Sliders(
        listOf(
            GlyphPart("M4 5h4M14 5h6M4 12h8M18 12h2M4 19h2M12 19h8", fill = false),
            GlyphPart(dot(11f, 5f, 2.4f)),
            GlyphPart(dot(15f, 12f, 2.4f)),
            GlyphPart(dot(9f, 19f, 2.4f))
        )
    ),

    Image(
        listOf(
            GlyphPart("M7 3h11a3 3 0 0 1 3 3v9", fill = false),
            GlyphPart(rr(3f, 7f, 16f, 13f, 3.5f)),
            GlyphPart(dot(8f, 12f, 1.4f)),
            GlyphPart("m3.5 18 4.5-4.5 3.5 3.5 2.5-2.5 4.5 4.5", fill = false)
        )
    ),
    Video(
        listOf(
            GlyphPart(rr(3f, 5f, 18f, 14f, 4f)),
            GlyphPart("M10 9.2v5.6a.6.6 0 0 0 .9.5l4.6-2.8a.6.6 0 0 0 0-1L10.9 8.7a.6.6 0 0 0-.9.5z")
        )
    ),

    Compare(
        listOf(
            GlyphPart(rr(3f, 4f, 18f, 16f, 4f)),
            GlyphPart("M12 4v16", fill = false),
            GlyphPart(dot(7.3f, 9.2f, 1.2f)),
            GlyphPart("m4 17.5 3.3-3.7 2.7 2.7", fill = false),
            GlyphPart("m14.5 17 2.6-3 3.4 4", fill = false),
            GlyphPart(dot(12f, 12f, 2.3f))
        )
    ),
    Collage(
        listOf(
            GlyphPart(rr(3f, 3f, 8f, 11f, 2.5f)),
            GlyphPart(rr(13f, 3f, 8f, 6f, 2.5f)),
            GlyphPart(rr(13f, 11f, 8f, 10f, 2.5f)),
            GlyphPart(rr(3f, 16f, 8f, 5f, 2.5f)),
            GlyphPart("m4.5 11 2.5-2.5 2.5 2.5", fill = false)
        )
    ),
    Edit(
        listOf(
            GlyphPart("M15.2 4.8a2.5 2.5 0 0 1 3.5 0l.5.5a2.5 2.5 0 0 1 0 3.5L9 19l-5 1 1-5z"),
            GlyphPart("M13.5 6.5l4 4", fill = false)
        )
    )
}

@Immutable
internal class GlassStyle(
    val dark: Boolean,
    val fillTop: Color,
    val fillBottom: Color,
    val border: Color,
    val highlight: Color,
    val lightAlpha: Float,
    val title: Color,
    val subtitle: Color,
    val images: Color,
    val video: Color,
    val collage: Color,
    val edit: Color,
    val compare: Color,
    val quick: Color,
    val background: Color
)

@Composable
internal fun rememberGlassStyle(): GlassStyle {
    val c = FrameTheme.colors
    return remember(c) {
        val a = c.accentA
        val b = c.accentB
        val d = c.accentC
        GlassStyle(
            dark = c.isDark,
            fillTop = Color.White.copy(alpha = if (c.isDark) 0.17f else 0.78f),
            fillBottom = Color.White.copy(alpha = if (c.isDark) 0.05f else 0.42f),
            border = Color.White.copy(alpha = if (c.isDark) 0.24f else 0.95f),
            highlight = Color.White.copy(alpha = if (c.isDark) 0.38f else 0.9f),
            lightAlpha = if (c.isDark) 0.55f else 0.40f,
            title = c.onSurface,
            subtitle = c.onSurfaceVariant,
            images = a,
            video = b,
            collage = d,
            edit = lerp(a, d, 0.5f),
            compare = lerp(b, d, 0.5f),
            quick = lerp(a, b, 0.5f),
            background = c.background
        )
    }
}

@Composable
internal fun LiquidGlyph(
    glyph: HomeGlyph,
    tone: Color,
    modifier: Modifier = Modifier,
    rim: Color? = null,
    fillAlpha: Float = 0.34f
) {
    val c = FrameTheme.colors
    val paths = remember(glyph) { glyph.parts.map { PathParser().parsePathString(it.path).toPath() to it.fill } }
    val rimColor = rim ?: remember(tone, c.isDark, c.background) {
        if (c.isDark) Color.White.copy(alpha = 0.92f)
        else ContrastGuard.ensure(lerp(tone, Color.Black, 0.35f), listOf(c.background), ContrastGuard.BOUNDARY, false)
    }
    Canvas(modifier) {
        scale(size.width / 24f, Offset.Zero) {
            translate(0f, 2.4f) {
                for (i in 1..5) {
                    val w = 1.1f + i * 1.3f
                    paths.forEach { (p, _) ->
                        drawPath(p, tone.copy(alpha = 0.07f), style = Stroke(w, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                }
            }
            paths.forEach { (p, fill) -> if (fill) drawPath(p, tone.copy(alpha = fillAlpha)) }
            paths.forEach { (p, _) -> drawPath(p, rimColor, style = Stroke(1.1f, cap = StrokeCap.Round, join = StrokeJoin.Round)) }
        }
    }
}

@Composable
internal fun Modifier.liquidFloat(baseRotation: Float = 0f, bobMs: Int = 3600): Modifier {
    val bob = loop(-1f, 1f, bobMs)
    return graphicsLayer {
        translationY = bob.value * 1.5.dp.toPx()
        rotationZ = baseRotation
    }
}

@Composable
internal fun GlassTile(
    glyph: HomeGlyph,
    tone: Color,
    shape: Shape,
    modifier: Modifier = Modifier,
    description: String,
    glyphSize: Dp = 84.dp,
    glyphX: Dp = 12.dp,
    glyphY: Dp = (-14).dp,
    glyphRotation: Float = -10f,
    glyphAlign: Alignment = Alignment.TopEnd,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    val g = rememberGlassStyle()
    Box(
        modifier
            .homePressable(onClick)
            .then(
                if (g.dark) Modifier
                else Modifier.shadow(12.dp, shape, ambientColor = tone.copy(alpha = 0.25f), spotColor = tone.copy(alpha = 0.25f))
            )
            .clip(shape)
            .background(Brush.linearGradient(listOf(g.fillTop, g.fillBottom)))
            .drawBehind {
                val rtl = layoutDirection == LayoutDirection.Rtl
                val cx = size.width * (if (rtl) 0.14f else 0.86f)
                val cy = size.height * 0.10f
                val r = size.width * 0.9f
                drawCircle(
                    Brush.radialGradient(listOf(tone.copy(alpha = g.lightAlpha), Color.Transparent), Offset(cx, cy), r),
                    r, Offset(cx, cy)
                )
                drawLine(g.highlight, Offset(size.height * 0.3f, 0.5f), Offset(size.width - size.height * 0.3f, 0.5f), 1.dp.toPx())
            }
            .border(1.dp, g.border, shape)
            .semantics(mergeDescendants = true) { contentDescription = description }
    ) {
        Box(Modifier.matchParentSize()) {
            LiquidGlyph(
                glyph, tone,
                Modifier
                    .align(glyphAlign)
                    .offset(glyphX, glyphY)
                    .size(glyphSize)
                    .liquidFloat(glyphRotation)
            )
        }
        content()
    }
}
