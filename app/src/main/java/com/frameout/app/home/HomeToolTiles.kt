package com.frameout.app.home

import androidx.compose.foundation.Canvas
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.core.util.FrameStrings
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.Image
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.app.ads.AdBannerSlot
import com.frameout.core.util.HomeStrings
import com.frameout.core.util.QualityStrings
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.theme.FrameTheme

@Composable
internal fun SquircleGlyph(glyph: HomeGlyph, tone: Color, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(30)
    val paths = remember(glyph) { glyph.parts.map { PathParser().parsePathString(it.path).toPath() to it.fill } }
    val onTone = if (tone.luminance() > 0.3f) lerp(tone, Color.Black, 0.82f) else Color.White
    val base = Brush.linearGradient(listOf(lerp(tone, Color.White, 0.32f), tone, lerp(tone, Color.Black, 0.22f)))
    Canvas(modifier.clip(shape).background(base)) {
        drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.35f), Color.Transparent), 0f, size.height * 0.55f))
        val unit = size.width * 0.56f / 24f
        translate(size.width * 0.22f, size.height * 0.22f) {
            scale(unit, Offset.Zero) {
                paths.forEach { (p, fill) ->
                    if (fill) drawPath(p, onTone.copy(alpha = 0.22f))
                    drawPath(p, onTone, style = Stroke(1.7f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
        }
        drawRoundRect(Color.White.copy(alpha = 0.35f), cornerRadius = CornerRadius(size.width * 0.3f), style = Stroke(1.dp.toPx()))
    }
}

@Composable
internal fun LatestVideoTile(cover: android.graphics.Bitmap, onOpen: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val label = FrameStrings.latestVideoOpen()
    Box(
        modifier
            .clip(HomeMetrics.TileShape)
            .background(Color.Black)
            .border(1.dp, Color.White.copy(alpha = 0.16f), HomeMetrics.TileShape)
            .homePressable(onOpen)
            .semantics { contentDescription = label }
    ) {
        Image(cover.asImageBitmap(), contentDescription = null, modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop)
        Box(
            Modifier.matchParentSize().background(
                Brush.verticalGradient(0.35f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.78f))
            )
        )
        PlayBadge(Modifier.align(Alignment.Center))
        FrameText(
            label,
            style = FrameTheme.typography.label.copy(fontSize = 13.sp, lineHeight = 17.sp),
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        )
        DismissButton(onDismiss, Modifier.align(Alignment.TopEnd))
    }
}

@Composable
private fun PlayBadge(modifier: Modifier) {
    Box(
        modifier.size(44.dp).clip(CircleShape)
            .background(Color.White.copy(alpha = 0.22f))
            .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape),
        Alignment.Center
    ) {
        Image(painterResource(FrameIcons.Play), contentDescription = null, modifier = Modifier.size(20.dp), colorFilter = ColorFilter.tint(Color.White))
    }
}

@Composable
private fun DismissButton(onDismiss: () -> Unit, modifier: Modifier) {
    Box(
        modifier.size(44.dp).frameClickable(onClickLabel = FrameStrings.latestVideoDismiss(), onClick = onDismiss),
        Alignment.Center
    ) {
        Box(Modifier.size(28.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.6f)), Alignment.Center) {
            Image(
                painterResource(FrameIcons.Close),
                contentDescription = FrameStrings.latestVideoDismiss(),
                modifier = Modifier.size(14.dp),
                colorFilter = ColorFilter.tint(Color.White)
            )
        }
    }
}

@Composable
private fun ToolsGrid(tools: List<ToolRowSpec>, look: HomeLook, tallHeight: Dp, shortHeight: Dp, latest: (@Composable (Modifier) -> Unit)?) {
    val reduce = LocalSystemA11y.current.reduceMotion
    val gap = HomeMetrics.TileGap
    val rows = listOf(tools[0] to tools[1], tools[2] to tools[3])
    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        rows.forEach { (a, b) ->
            Row(Modifier.fillMaxWidth().height(tallHeight), horizontalArrangement = Arrangement.spacedBy(gap)) {
                ToolTile(a, look, Modifier.weight(1f).fillMaxHeight())
                ToolTile(b, look, Modifier.weight(1f).fillMaxHeight())
            }
        }
        Box(Modifier.fillMaxWidth().then(if (reduce) Modifier else Modifier.animateContentSize()).height(shortHeight)) {
            if (latest != null) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    CompareCompactTile(tools[4], look, Modifier.weight(1f).fillMaxHeight())
                    latest(Modifier.weight(1f).fillMaxHeight())
                }
            } else {
                CompareWideTile(tools[4], look, Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
internal fun HomeToolsSheet(
    onImages: () -> Unit,
    onVideo: () -> Unit,
    onCompare: () -> Unit,
    onCollage: () -> Unit,
    onEdit: () -> Unit,
    tallHeight: Dp,
    shortHeight: Dp,
    bottomInset: Dp,
    modifier: Modifier = Modifier,
    latest: (@Composable (Modifier) -> Unit)? = null
) {
    val look = rememberHomeLook()
    val t = look.tones
    Box(modifier.fillMaxWidth()) {
        Column(
            Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 640.dp)
                .fillMaxWidth()
                .padding(start = HomeMetrics.ScreenPad, end = HomeMetrics.ScreenPad, bottom = bottomInset),
            verticalArrangement = Arrangement.spacedBy(HomeMetrics.LabelGap)
        ) {
            FrameText(
                QualityStrings.toolsLabel(),
                style = FrameTheme.typography.title.copy(fontSize = 18.sp, lineHeight = 24.sp),
                color = look.title,
                modifier = Modifier.padding(start = HomeMetrics.LabelInset)
            )
            ToolsGrid(
                listOf(
                    ToolRowSpec(HomeStrings.editTitle(), HomeStrings.editDesc(), HomeArt.Edit, t.edit, onEdit),
                    ToolRowSpec(HomeStrings.imagesTitle(), HomeStrings.imagesDesc(), HomeArt.Images, t.images, onImages),
                    ToolRowSpec(HomeStrings.videoTitle(), HomeStrings.videoDesc(), HomeArt.Video, t.video, onVideo),
                    ToolRowSpec(HomeStrings.collageTitle(), HomeStrings.collageDesc(), HomeArt.Collage, t.collage, onCollage),
                    ToolRowSpec(HomeStrings.compareTitle(), HomeStrings.compareDesc(), HomeArt.Compare, t.compare, onCompare)
                ),
                look,
                tallHeight,
                shortHeight,
                latest
            )
            AdBannerSlot(reserve = true, horizontalPad = 0.dp)
        }
    }
}
