package com.frameout.app.trim

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.TransformOrigin
import kotlin.math.acos
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.frameout.core.util.TrimStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.theme.FrameTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val GRAB_AREA = 30.dp
private val FOLD_GAP = 12.dp

private const val FLING_VELOCITY = 600f
private const val FOLD_CAMERA = 40f
private const val OVERSHOOT_LIMIT = 0.6f
private const val OVERSHOOT_DAMPING = 0.35f

@Stable
internal class PanelFold(private val scope: CoroutineScope, private val onSettle: () -> Unit) {
    var fraction by mutableFloatStateOf(1f)
    var dragging by mutableStateOf(false)
    var natural = 0
    var overshoot by mutableFloatStateOf(0f)
    private var raw = 1f
    private var job: Job? = null
    private var bounce: Job? = null

    fun drag(deltaY: Float) {
        job?.cancel()
        bounce?.cancel()
        raw = (raw - deltaY / natural.coerceAtLeast(1)).coerceIn(-OVERSHOOT_LIMIT, 1f + OVERSHOOT_LIMIT)
        fraction = raw.coerceIn(0f, 1f)
        overshoot = (raw - fraction) * OVERSHOOT_DAMPING
    }

    private fun release() {
        raw = fraction
        if (overshoot == 0f) return
        bounce?.cancel()
        bounce = scope.launch {
            animate(overshoot, 0f, animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow)) { value, _ -> overshoot = value }
        }
    }

    fun settle(velocityY: Float) {
        release()
        val target = when {
            velocityY > FLING_VELOCITY -> 0f
            velocityY < -FLING_VELOCITY -> 1f
            fraction > 0.5f -> 1f
            else -> 0f
        }
        animateTo(target)
    }

    fun toggle() = animateTo(if (fraction > 0.5f) 0f else 1f)

    private fun animateTo(target: Float) {
        job?.cancel()
        onSettle()
        job = scope.launch {
            animate(fraction, target, animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)) { value, _ ->
                fraction = value.coerceIn(0f, 1f)
                raw = fraction
            }
        }
    }
}

@Composable
internal fun Foldable(fold: PanelFold, modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    val colors = FrameTheme.colors
    val shade = colors.surface
    val hinge = if (colors.isDark) Color.White else colors.primary
    Box(
        modifier
            .clipToBounds()
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints.copy(minHeight = 0))
                fold.natural = placeable.height
                val height = (placeable.height * fold.fraction.coerceIn(0f, 1f)).roundToInt().coerceAtLeast(0)
                layout(placeable.width, height) { placeable.placeRelative(0, 0) }
            }
            .graphicsLayer {
                val f = fold.fraction.coerceIn(0f, 1f)
                transformOrigin = TransformOrigin(0.5f, 0f)
                rotationX = Math.toDegrees(acos(f.toDouble())).toFloat()
                cameraDistance = FOLD_CAMERA * density
                val squeeze = 1f - 0.06f * (1f - f)
                scaleX = squeeze
                alpha = (f / 0.18f).coerceIn(0f, 1f)
            }
            .drawWithContent {
                drawContent()
                val f = fold.fraction.coerceIn(0f, 1f)
                val away = 1f - f
                if (away > 0f) {
                    drawRect(shade.copy(alpha = 0.78f * away))
                    drawRect(
                        Brush.horizontalGradient(listOf(Color.Transparent, hinge.copy(alpha = 0.55f * away), Color.Transparent)),
                        size = androidx.compose.ui.geometry.Size(size.width, 1.5.dp.toPx())
                    )
                }
            }
    ) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(top = FOLD_GAP), Arrangement.spacedBy(FOLD_GAP), content = content)
    }
}

@Composable
internal fun PanelGrabber(fold: PanelFold) {
    val colors = FrameTheme.colors
    val state = rememberDraggableState { fold.drag(it) }
    val width by animateDpAsState(if (fold.dragging) 64.dp else 44.dp, spring(stiffness = Spring.StiffnessMedium), label = "grabWidth")
    val label = TrimStrings.panelHandle()
    Box(
        Modifier
            .fillMaxWidth()
            .height(GRAB_AREA)
            .draggable(
                state, Orientation.Vertical,
                onDragStarted = { fold.dragging = true },
                onDragStopped = { velocity ->
                    fold.dragging = false
                    fold.settle(velocity)
                }
            )
            .frameClickable(onClickLabel = label) { fold.toggle() }
            .semantics { contentDescription = label; role = Role.Button },
        Alignment.Center
    ) {
        Column(Modifier, Arrangement.spacedBy(3.dp), Alignment.CenterHorizontally) {
            val tint = if (fold.dragging) colors.primary else colors.onSurfaceVariant.copy(alpha = 0.55f)
            Box(Modifier.width(width).height(4.dp).background(tint, CircleShape))
            Box(Modifier.width(width * 0.55f).height(3.dp).background(tint.copy(alpha = tint.alpha * 0.6f), CircleShape))
        }
    }
}
