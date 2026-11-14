package com.frameout.app.viewer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import com.frameout.core.quality.TagNames
import com.frameout.core.util.AppLang
import com.frameout.designsystem.a11y.frameClickable
import kotlin.math.roundToInt

private val BlurActionBottom = 96.dp

@Composable
internal fun DetectionOverlay(
    detections: List<Detection>,
    imageWidth: Int,
    imageHeight: Int,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    selected: Detection?,
    onSelect: (Detection?) -> Unit,
    onBlur: (Detection) -> Unit,
    modifier: Modifier = Modifier
) {
    if (detections.isEmpty() || imageWidth <= 0 || imageHeight <= 0) return
    val palette = rememberDetectionPalette()
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        BoxWithConstraints(modifier.fillMaxSize()) {
            val width = constraints.maxWidth.toFloat()
            val height = constraints.maxHeight.toFloat()
            val fit = remember(width, height, imageWidth, imageHeight, scale, offsetX, offsetY) {
                DetectionFit(width, height, imageWidth, imageHeight, scale, offsetX, offsetY)
            }
            DetectionFrames(detections, fit, palette, selected)
            DetectionHits(detections, fit) { onSelect(if (selected == it) null else it) }
            DetectionBlurAction(
                color = selected?.let { palette.colorOf(it) } ?: palette.neutral,
                ink = palette.ink,
                enabled = selected != null,
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = BlurActionBottom)
            ) { selected?.let(onBlur) }
        }
    }
}

@Composable
private fun DetectionFrames(detections: List<Detection>, fit: DetectionFit, palette: DetectionPalette, selected: Detection?) {
    val lang = AppLang.current()
    val measurer = rememberTextMeasurer()
    Canvas(Modifier.fillMaxSize()) {
        detections.forEach { d ->
            if (d != selected) drawDetection(fit.rectOf(d), palette.colorOf(d), false)
        }
        selected?.let { drawDetection(fit.rectOf(it), palette.colorOf(it), true) }
        detections.forEach { d ->
            if (d != selected) drawDetectionLabel(fit.rectOf(d), palette.colorOf(d), palette.ink, d.tags.joinToString(" · ") { TagNames.display(it, lang) }, measurer, size.width, false)
        }
        selected?.let { d ->
            drawDetectionLabel(fit.rectOf(d), palette.colorOf(d), palette.ink, d.tags.joinToString(" · ") { TagNames.display(it, lang) }, measurer, size.width, true)
        }
    }
}

@Composable
private fun DetectionHits(detections: List<Detection>, fit: DetectionFit, onSelect: (Detection) -> Unit) {
    val density = LocalDensity.current
    val lang = AppLang.current()
    val ordered = remember(detections) { detections.sortedByDescending { it.area() } }
    ordered.forEach { d ->
        val rect = fit.rectOf(d)
        val label = d.tags.joinToString(" · ") { TagNames.display(it, lang) }
        val minHit = with(density) { 48.dp.toPx() }
        val w = maxOf(rect.width, minHit)
        val h = maxOf(rect.height, minHit)
        val left = rect.left + rect.width / 2f - w / 2f
        val top = rect.top + rect.height / 2f - h / 2f
        Box(
            Modifier
                .absoluteOffset { IntOffset(left.roundToInt(), top.roundToInt()) }
                .size(with(density) { w.toDp() }, with(density) { h.toDp() })
                .frameClickable(onClickLabel = label) { onSelect(d) }
        )
    }
}
