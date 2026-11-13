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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.engine.timeline.formatTimestamp

internal class Shot(val id: Long, val preview: Bitmap, val positionMs: Long, val saved: SavedCapture?)

@Stable
internal class ShotsState {
    val items = mutableStateListOf<Shot>()
    private var serial = 0L

    fun add(preview: Bitmap, positionMs: Long, saved: SavedCapture?) {
        serial += 1
        items.add(0, Shot(serial, preview, positionMs, saved))
        while (items.size > MAX) items.removeAt(items.lastIndex)
    }

    private companion object {
        const val MAX = 60
    }
}

@Composable
internal fun ShotStrip(env: TrimEnv, modifier: Modifier = Modifier) {
    val shots = env.shots.items.toList()
    val aspect = env.player.videoAspect
    val thumbHeight = if (aspect > 0f && aspect < 1f) TALL_THUMB_HEIGHT else THUMB_HEIGHT
    AnimatedVisibility(
        visible = shots.isNotEmpty(),
        modifier = modifier,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        val listState = rememberLazyListState()
        LaunchedEffect(shots.size) { if (shots.isNotEmpty()) listState.animateScrollToItem(0) }
        LazyRow(
            modifier = Modifier.fillMaxWidth().height(thumbHeight + 10.dp),
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(shots, key = { it.id }) { shot -> ShotThumb(shot, thumbHeight) { env.controller.seekTo(shot.positionMs) } }
        }
    }
}

@Composable
private fun ShotThumb(shot: Shot, height: Dp, onClick: () -> Unit) {
    val image = remember(shot.id) { shot.preview.asImageBitmap() }
    val aspect = image.width.toFloat() / image.height.coerceAtLeast(1)
    val width = (height.value * aspect).dp.coerceIn(MIN_THUMB_WIDTH, MAX_THUMB_WIDTH)
    val shape = RoundedCornerShape(10.dp)
    val time = formatTimestamp(shot.positionMs)
    Box(
        Modifier
            .size(width, height)
            .clip(shape)
            .border(1.5.dp, FrameTheme.colors.primary.copy(alpha = 0.55f), shape)
            .frameClickable(onClick = onClick)
            .semantics { contentDescription = time }
    ) {
        Image(image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        FrameText(
            time,
            style = FrameTheme.typography.bodySmall,
            color = Color.White,
            maxLines = 1,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(topEnd = 8.dp))
                .padding(horizontal = 5.dp, vertical = 1.dp)
        )
    }
}

private val THUMB_HEIGHT = 56.dp
private val TALL_THUMB_HEIGHT = 44.dp
private val MIN_THUMB_WIDTH = 52.dp
private val MAX_THUMB_WIDTH = 96.dp
