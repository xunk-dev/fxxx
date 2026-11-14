package com.frameout.app.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.style.TextOverflow
import com.frameout.app.R
import com.frameout.core.util.HomeStrings
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.haptics.FrameHaptic
import com.frameout.designsystem.haptics.rememberFrameHaptics
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameTheme

internal val Orange = Color(0xFFF97316)
internal val Sky = Color(0xFF0EA5E9)
internal val Violet = Color(0xFF6366F1)
internal val Mint = Color(0xFF10B981)
internal val Ink = Color(0xFF050808)
private val CardShape = RoundedCornerShape(28.dp)

@Composable
internal fun loop(from: Float, to: Float, ms: Int, reverse: Boolean = true): State<Float> {
    val reduce = LocalSystemA11y.current.reduceMotion
    if (reduce) return remember { mutableStateOf((from + to) / 2f) }
    val t = rememberInfiniteTransition(label = "homeLoop")
    return t.animateFloat(
        from, to,
        infiniteRepeatable(tween(ms, easing = if (reverse) FastOutSlowInEasing else LinearEasing), if (reverse) RepeatMode.Reverse else RepeatMode.Restart),
        label = "loop"
    )
}

@Composable
internal fun HomeImage(
    name: String,
    modifier: Modifier = Modifier,
    zoom: Float = 1f,
    ox: Float = 0.5f,
    oy: Float = 0.5f,
    filter: ColorFilter? = null
) {
    val context = LocalContext.current
    val id = remember(name) { context.resources.getIdentifier(name, "drawable", context.packageName) }
    Box(modifier.clipToBounds().background(Color(0xFF1E2625))) {
        if (id != 0) {
            Image(
                painterResource(id), contentDescription = null,
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    scaleX = zoom; scaleY = zoom; transformOrigin = TransformOrigin(ox, oy)
                },
                contentScale = ContentScale.Crop, colorFilter = filter
            )
        }
    }
}

@Composable
internal fun Ltr(content: @Composable () -> Unit) =
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) { content() }

@Composable
private fun GlowCard(accent: Color, modifier: Modifier, onClick: () -> Unit, content: @Composable BoxScope.() -> Unit) {
    val reduce = LocalSystemA11y.current.reduceMotion
    val density = LocalDensity.current.density
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val haptics = rememberFrameHaptics()
    var tilt by remember { mutableStateOf(Offset.Zero) }
    val spec = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
    val scale by animateFloatAsState(if (pressed && !reduce) 0.97f else 1f, spec, label = "bentoPress")
    val tx by animateFloatAsState(tilt.x, spec, label = "bentoTx")
    val ty by animateFloatAsState(tilt.y, spec, label = "bentoTy")
    Box(
        modifier
            .graphicsLayer {
                scaleX = scale; scaleY = scale
                rotationY = tx * 10f; rotationX = -ty * 10f
                cameraDistance = 14f * density
            }
            .shadow(14.dp, CardShape, ambientColor = accent, spotColor = accent)
            .clip(CardShape)
            .background(Ink)
            .border(1.5.dp, accent.copy(alpha = 0.6f), CardShape)
            .pointerInput(reduce) {
                awaitPointerEventScope {
                    while (true) {
                        val c = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull()
                        tilt = if (c != null && c.pressed && !reduce && size.width > 0) {
                            Offset(c.position.x / size.width - 0.5f, c.position.y / size.height - 0.5f)
                        } else Offset.Zero
                    }
                }
            }
            .clickable(interactionSource = source, indication = null, role = Role.Button) {
                haptics.perform(FrameHaptic.Tap)
                onClick()
            },
        content = content
    )
}

@Composable
private fun BoxScope.CardLabel(accent: Color, icon: Int, title: String, desc: String) {
    Box(
        Modifier.matchParentSize().background(
            Brush.verticalGradient(0.45f to Color.Transparent, 1f to lerp(accent, Ink, 0.8f).copy(alpha = 0.95f))
        )
    )
    Row(
        Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(lerp(accent, Ink, 0.30f)), Alignment.Center) {
            Image(painterResource(icon), null, Modifier.size(20.dp), colorFilter = ColorFilter.tint(Color.White))
        }
        Column(Modifier.weight(1f)) {
            FitText(title, style = FrameTheme.typography.title, color = Color.White, minScale = 0.75f)
            FrameText(desc, style = FrameTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.78f), maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
internal fun CollageCard(onClick: () -> Unit, modifier: Modifier = Modifier) = GlowCard(Orange, modifier, onClick) {
    Box(Modifier.fillMaxSize().padding(start = 10.dp, top = 10.dp, end = 10.dp, bottom = 66.dp)) { CollageArt() }
    CardLabel(Orange, FrameIcons.Collage, HomeStrings.collageTitle(), HomeStrings.collageDesc())
}

@Composable
internal fun CompareCard(onClick: () -> Unit, modifier: Modifier = Modifier) = GlowCard(Sky, modifier, onClick) {
    CompareArt()
    CardLabel(Sky, R.drawable.ic_home_compare, HomeStrings.compareTitle(), HomeStrings.compareDesc())
}

@Composable
internal fun EditCard(onClick: () -> Unit, modifier: Modifier = Modifier) = GlowCard(Violet, modifier, onClick) {
    EditArt()
    CardLabel(Violet, FrameIconsExtra.Edit, HomeStrings.editTitle(), HomeStrings.editDesc())
}

@Composable
internal fun ExportToolCard(onClick: () -> Unit, modifier: Modifier = Modifier) = GlowCard(Mint, modifier, onClick) {
    val phase by loop(0f, 3f, 9000, reverse = false)
    val descs = listOf(HomeStrings.exportMethodVideo(), HomeStrings.exportMethodSheet(), HomeStrings.exportMethodImages())
    Box(Modifier.matchParentSize().background(Brush.linearGradient(listOf(Color(0xFF0B4A3F), Ink))))
    Row(Modifier.fillMaxSize().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1.08f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(lerp(Mint, Ink, 0.30f)), Alignment.Center) {
                Image(painterResource(FrameIconsExtra.Export), null, Modifier.size(20.dp), colorFilter = ColorFilter.tint(Color.White))
            }
            FrameText(HomeStrings.exportTitle(), style = FrameTheme.typography.title, color = Color.White, maxLines = 1)
            Crossfade(phase.toInt().coerceIn(0, 2), label = "exportDesc") {
                FrameText(descs[it], style = FrameTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.78f), maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
        Ltr { ExportFaces(phase, Modifier.weight(0.92f).fillMaxHeight()) }
    }
}
