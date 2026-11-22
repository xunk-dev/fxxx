package com.frameout.app.trim

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import com.frameout.core.quality.Sharpness
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.engine.player.frameNumberAt
import com.frameout.engine.player.frameTimesUs
import com.frameout.engine.player.grabFrame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt

internal class Neighbor(val frame: Int, val ms: Long, val image: ImageBitmap, val score: Double)

internal object NeighborRules {
    const val RADIUS = 5

    fun keepsWindow(center: Int, anchor: Int, loaded: Boolean): Boolean = loaded && anchor >= 0 && abs(center - anchor) <= RADIUS

    fun range(center: Int, total: Int): IntRange = maxOf(0, center - RADIUS)..minOf(total - 1, center + RADIUS)

    fun relativePercent(score: Double, best: Double): Int =
        if (best <= 0.0) 0 else (score / best * 100.0).roundToInt().coerceIn(0, 100)

    fun isBest(score: Double, best: Double): Boolean = best > 0.0 && score >= best
}

@Composable
internal fun NeighborStrip(env: TrimEnv, modifier: Modifier = Modifier) {
    val controller = env.controller
    val position by controller.position.collectAsState()
    val total = rememberFrameCount(controller)
    val center = controller.frameNumberAt(position) - 1
    val ready = total > 0 && center >= 0 && !env.player.isPlaying && !env.player.animated && env.output == TrimOutput.Frames
    var neighbors by remember { mutableStateOf(emptyList<Neighbor>()) }
    var anchor by remember { mutableIntStateOf(-1) }
    LaunchedEffect(ready, center) {
        if (!ready) return@LaunchedEffect
        if (NeighborRules.keepsWindow(center, anchor, neighbors.isNotEmpty())) return@LaunchedEffect
        delay(SETTLE_MS)
        val times = controller.frameTimesUs() ?: return@LaunchedEffect
        anchor = center
        neighbors = emptyList()
        val loaded = ArrayList<Neighbor>()
        for (frame in NeighborRules.range(center, total)) {
            val shot = controller.grabFrame(frame) ?: continue
            loaded += withContext(Dispatchers.Default) { neighborOf(frame, times[frame] / 1000L, shot) }
            neighbors = loaded.toList()
        }
    }
    val best = neighbors.maxOfOrNull { it.score } ?: 0.0
    AnimatedVisibility(
        visible = ready && neighbors.isNotEmpty(),
        modifier = modifier,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        NeighborRow(neighbors, center, best) { ms -> controller.seekTo(ms) }
    }
}

@Composable
private fun NeighborRow(neighbors: List<Neighbor>, center: Int, best: Double, onSeek: (Long) -> Unit) {
    val listState = rememberLazyListState()
    val at = neighbors.indexOfFirst { it.frame == center }
    LaunchedEffect(at, neighbors.size) { if (at >= 0) listState.animateScrollToItem((at - 2).coerceAtLeast(0)) }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        LazyRow(
            modifier = Modifier.fillMaxWidth().height(THUMB_HEIGHT + 12.dp),
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(neighbors, key = { it.frame }) { n ->
                NeighborThumb(n, n.frame == center, best, THUMB_HEIGHT) { onSeek(n.ms) }
            }
        }
    }
}

private fun neighborOf(frame: Int, ms: Long, shot: Bitmap): Neighbor {
    val longest = maxOf(shot.width, shot.height).coerceAtLeast(1)
    val scale = SCORE_EDGE.toFloat() / longest
    val small = if (scale >= 1f) shot else Bitmap.createScaledBitmap(
        shot,
        (shot.width * scale).roundToInt().coerceAtLeast(3),
        (shot.height * scale).roundToInt().coerceAtLeast(3),
        true
    )
    val score = runCatching { Sharpness.score(Sharpness.lumaOf(small), small.width, small.height) }.getOrDefault(0.0)
    return Neighbor(frame, ms, small.asImageBitmap(), score)
}

@Composable
private fun NeighborThumb(n: Neighbor, current: Boolean, best: Double, height: Dp, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    val aspect = n.image.width.toFloat() / n.image.height.coerceAtLeast(1)
    val width = (height.value * aspect).dp.coerceIn(MIN_WIDTH, MAX_WIDTH)
    val shape = RoundedCornerShape(8.dp)
    val top = NeighborRules.isBest(n.score, best)
    val edge = when {
        current -> colors.primary
        top -> colors.success
        else -> colors.onSurfaceVariant.copy(alpha = 0.35f)
    }
    val label = "#${n.frame + 1}"
    Box(
        Modifier
            .size(width, height)
            .clip(shape)
            .border(if (current || top) 2.dp else 1.dp, edge, shape)
            .frameClickable(onClick = onClick)
            .semantics { contentDescription = label }
    ) {
        Image(n.image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        FrameText(
            label,
            style = FrameTheme.typography.bodySmall,
            color = Color.White,
            maxLines = 1,
            modifier = Modifier.align(Alignment.BottomStart).background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(topEnd = 6.dp)).padding(horizontal = 4.dp)
        )
        FrameText(
            (if (top) "\u2605 " else "") + NeighborRules.relativePercent(n.score, best),
            style = FrameTheme.typography.bodySmall,
            color = if (top) colors.success else Color.White,
            maxLines = 1,
            modifier = Modifier.align(Alignment.TopEnd).background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(bottomStart = 6.dp)).padding(horizontal = 4.dp)
        )
    }
}

private val THUMB_HEIGHT = 60.dp
private val MIN_WIDTH = 48.dp
private val MAX_WIDTH = 104.dp
private const val SETTLE_MS = 260L
private const val SCORE_EDGE = 240
