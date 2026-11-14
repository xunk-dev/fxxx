package com.frameout.app.billing

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.core.pro.ProStrings
import com.frameout.designsystem.R as DsR
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.theme.FrameShapes

private val ChipIcons = listOf(DsR.drawable.ic_filter, DsR.drawable.ic_shapes, DsR.drawable.ic_tools, DsR.drawable.ic_image_format, DsR.drawable.ic_sparkle, DsR.drawable.ic_export)

internal fun Modifier.revealAt(t: State<Float>, from: Float, to: Float): Modifier = graphicsLayer {
    val k = seg(t.value, from, to)
    alpha = k
    translationY = (1f - k) * 14.dp.toPx()
    scaleX = 0.94f + 0.06f * k
    scaleY = 0.94f + 0.06f * k
}

@Composable
internal fun Medallion(t: State<Float>, pulse: State<Float>, modifier: Modifier = Modifier) {
    val shimmer = rememberShimmer(3200, 1400)
    Box(
        modifier
            .graphicsLayer {
                val pop = 0.35f + 0.65f * easeOutBack(seg(t.value, 0.04f, 0.34f))
                val s = pop * (1f + 0.02f * pulse.value)
                scaleX = s
                scaleY = s
                alpha = seg(t.value, 0.04f, 0.16f)
            }
            .size(132.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(ProGold.Light, ProGold.Deep)), CircleShape)
            .border(2.dp, Color.White.copy(alpha = 0.6f), CircleShape)
            .shimmerSweep(shimmer, 0.6f),
        Alignment.Center
    ) {
        Box(Modifier.size(96.dp).background(ProGold.Ink.copy(alpha = 0.92f), CircleShape).border(1.dp, ProGold.Light.copy(alpha = 0.5f), CircleShape), Alignment.Center) {
            Image(painterResource(FrameIcons.StarFilled), null, Modifier.size(52.dp), colorFilter = ColorFilter.tint(ProGold.Light))
        }
    }
}

@Composable
internal fun DonePerks(t: State<Float>) {
    val labels = ProStrings.doneChips()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.chunked(2).forEachIndexed { row, pair ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEachIndexed { col, label ->
                    val i = row * 2 + col
                    val reveal = Modifier.weight(1f).fillMaxHeight().revealAt(t, 0.46f + 0.05f * i, 0.62f + 0.05f * i)
                    DonePerk(ChipIcons[i % ChipIcons.size], label, reveal)
                }
                if (pair.size == 1) Box(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DonePerk(icon: Int, label: String, modifier: Modifier) {
    Row(
        modifier
            .heightIn(min = 52.dp)
            .background(Color.White.copy(alpha = 0.07f), FrameShapes.md)
            .border(1.dp, ProGold.Deep.copy(alpha = 0.35f), FrameShapes.md)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(30.dp).background(ProGold.Deep.copy(alpha = 0.2f), CircleShape), Alignment.Center) {
            Image(painterResource(icon), null, Modifier.size(18.dp), colorFilter = ColorFilter.tint(ProGold.Light))
        }
        FrameText(label, com.frameout.designsystem.theme.FrameTheme.typography.label.copy(fontSize = 12.5.sp), Color.White, Modifier.weight(1f), maxLines = 2)
        Image(painterResource(FrameIcons.Check), null, Modifier.size(15.dp), colorFilter = ColorFilter.tint(ProGold.Light))
    }
}

@Composable
internal fun DoneCta(modifier: Modifier, enabled: Boolean, onClick: () -> Unit) {
    val shimmer = rememberShimmer(3000, 900)
    Box(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(FrameShapes.pill)
            .background(Brush.horizontalGradient(listOf(ProGold.Light, ProGold.Deep)))
            .border(1.dp, Color.White.copy(alpha = 0.45f), FrameShapes.pill)
            .shimmerSweep(shimmer, 0.55f)
            .frameClickable(enabled = enabled, onClick = onClick),
        Alignment.Center
    ) {
        FrameText(ProStrings.doneCta(), com.frameout.designsystem.theme.FrameTheme.typography.title.copy(fontWeight = FontWeight.Bold), ProGold.Ink, textAlign = TextAlign.Center)
    }
}
