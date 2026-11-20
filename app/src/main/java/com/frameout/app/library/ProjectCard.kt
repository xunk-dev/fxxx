package com.frameout.app.library

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import com.frameout.engine.work.LiveExtractProgress
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.app.thumb.rememberThumb
import com.frameout.codecpack.MediaFailure
import com.frameout.core.db.ProjectEntity
import com.frameout.core.db.progressTotal
import com.frameout.core.util.FailureMarker
import com.frameout.core.util.FailureStrings
import com.frameout.core.util.FrameA11y
import com.frameout.core.util.ThumbCache
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.a11y.frameCombinedClickable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.glass.frameGlow
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameCardBrush
import com.frameout.designsystem.theme.frameCardEdge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ProjectCard(
    project: ProjectEntity,
    selected: Boolean,
    onOpen: () -> Unit,
    onLongClick: () -> Unit,
    onResume: () -> Unit,
    onPause: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = FrameTheme.colors
    val context = LocalContext.current
    val thumb = rememberThumb(project.thumbnailPath, { ThumbCache.peek(project.thumbnailPath) }, { ThumbCache.load(project.thumbnailPath) }).value
    val failure = produceState<MediaFailure?>(null, project.status, project.updatedAt) {
        value = if (project.status == "Failed") withContext(Dispatchers.IO) { FailureMarker.read(context, project.id) } else null
    }.value
    var details by remember { androidx.compose.runtime.mutableStateOf(false) }
    val live = isLive(project.status)
    val pulse = livePulse(live)
    val shape = FrameShapes.xl
    val glass = com.frameout.app.home.rememberGlassStyle()
    val tone = cardTone(project.status, colors)
    val border = when {
        selected -> Modifier.border(2.dp, colors.primary, shape)
        live -> Modifier.border(1.5.dp, frameCardEdge(colors.primary, 0.70f), shape)
        else -> Modifier.border(1.dp, if (glass.dark) Color.White.copy(alpha = 0.13f) else glass.border, shape)
    }
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .frameCombinedClickable(onOpen, onLongClick, FrameA11y.select())
            .semantics { this.selected = selected }
            .then(if (live) Modifier.frameGlow(colors.primary, { pulse?.value ?: 0.16f }, 12.dp, { 0.12f }) else Modifier)
            .projectGlass(shape, tone, boost = if (live) (pulse?.value ?: 0.16f) * 0.5f else 0f)
            .then(border)
    ) {
        val progress = rememberCardProgress(project)
        val percent = (progress as? CardProgress.Value)?.percent
        Row(
            Modifier.clip(shape).padding(11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProjectThumb(thumb, project, 76.dp)
            ProjectInfo(project, failure, percent, Modifier.weight(1f))
            CardTrailing(project, selected, onResume, onPause, { details = true }, onDelete)
        }
        if (progress != CardProgress.None) {
            ProjectProgressBar(
                progress,
                Modifier.align(Alignment.BottomStart),
                height = 3.dp,
                shape = androidx.compose.ui.graphics.RectangleShape,
                brush = if (project.status == "Paused") Brush.horizontalGradient(listOf(colors.warning, colors.warning)) else null
            )
        }
        if (details) ProjectDetailsDialog(project) { details = false }
    }
}

@Composable
private fun CardTrailing(project: ProjectEntity, selected: Boolean, onResume: () -> Unit, onPause: () -> Unit, onDetails: () -> Unit, onDelete: () -> Unit) {
    Column(Modifier.width(48.dp).then(if (selected) Modifier.height(48.dp) else Modifier), Arrangement.Center, Alignment.CenterHorizontally) {
        if (!selected) {
            if (hasStateAction(project)) StateAction(project, onResume, onPause)
            ProjectMoreAction(onDetails, onDelete)
        }
    }
}

@Composable
private fun livePulse(live: Boolean): State<Float>? {
    val reduce = LocalSystemA11y.current.reduceMotion || !com.frameout.app.LocalTabActive.current
    return if (live && !reduce) {
        rememberInfiniteTransition(label = "cardGlow").animateFloat(
            initialValue = 0.10f,
            targetValue = 0.30f,
            animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "cardGlowValue"
        )
    } else null
}

@Composable
private fun ProjectInfo(project: ProjectEntity, failure: MediaFailure?, percent: Int?, modifier: Modifier) {
    val colors = FrameTheme.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        FrameText(project.name, style = FrameTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (failure != null) {
            FrameText(FailureStrings.of(failure), style = FrameTheme.typography.bodySmall, color = colors.error, maxLines = 1, overflow = TextOverflow.Ellipsis)
        } else {
            DateLine(project.createdAt)
        }
        Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FrameCountChip("${project.extractedCount}", null, com.frameout.core.util.FrameStrings.frameCount(project.extractedCount))
            if (project.status != "Completed") StatusPill(project.status, percent)
        }
    }
}

@Composable
private fun rememberCardProgress(project: ProjectEntity): CardProgress {
    val tabActive = com.frameout.app.LocalTabActive.current
    val liveState = remember { androidx.compose.runtime.mutableStateOf(LiveExtractProgress.all.value) }
    androidx.compose.runtime.LaunchedEffect(tabActive) {
        if (tabActive) LiveExtractProgress.all.collect { liveState.value = it }
    }
    val allLive by liveState
    val live by remember(project.id) { derivedStateOf { allLive[project.id] } }
    val peak = remember(project.id) { floatArrayOf(0f) }
    return cardProgress(project, live).let { shown ->
        if (shown is CardProgress.Value) {
            peak[0] = maxOf(shown.fraction, peak[0])
            CardProgress.Value(peak[0], percentOf(peak[0]))
        } else {
            peak[0] = 0f
            shown
        }
    }
}

@Composable
private fun DateLine(ms: Long) {
    val colors = FrameTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        androidx.compose.foundation.Image(
            androidx.compose.ui.res.painterResource(com.frameout.designsystem.icons.FrameIconsExtra.Calendar),
            null,
            Modifier.size(13.dp),
            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(colors.onSurfaceVariant)
        )
        FrameText(formatStamp(ms), style = FrameTheme.typography.monoSmall.copy(fontSize = 11.sp), color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
