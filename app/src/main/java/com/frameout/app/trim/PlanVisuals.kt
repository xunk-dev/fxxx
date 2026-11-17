package com.frameout.app.trim

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.frameout.core.util.TrimStrings
import com.frameout.designsystem.components.FrameRollingText
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameHairline
import com.frameout.engine.extract.ExtractMode
import java.text.NumberFormat
import kotlin.math.ln

private const val METER_MAX_MB = 2048f
private const val METER_WARM_MB = 600f
private const val METER_HOT_MB = 1500f
private const val MB_PER_FRAME = 0.18f

internal fun estimateMbFromFrames(frames: Int): Float = frames * MB_PER_FRAME

internal fun planCount(count: Int, exact: Boolean = true): String {
    val text = NumberFormat.getIntegerInstance().format(count.toLong())
    return if (exact) text else "\u2248 $text"
}

internal fun planCount(count: Int, kind: EstimateKind): String {
    val text = NumberFormat.getIntegerInstance().format(count.toLong())
    return when (kind) {
        EstimateKind.Exact -> text
        EstimateKind.UpperBound -> "\u2264 $text"
        EstimateKind.Approx -> "\u2248 $text"
    }
}

private const val MB_PER_GB = 1024f
private const val MB_PER_TB = 1024f * 1024f

private enum class SizeScale { MB, GB, TB }

private fun scaleFor(mb: Float): SizeScale = when {
    mb >= MB_PER_TB -> SizeScale.TB
    mb >= MB_PER_GB -> SizeScale.GB
    else -> SizeScale.MB
}

internal fun planMegabytes(mb: Float): String {
    val (value, digits) = when (scaleFor(mb)) {
        SizeScale.TB -> mb / MB_PER_TB to 2
        SizeScale.GB -> mb / MB_PER_GB to 1
        SizeScale.MB -> mb to (if (mb >= 100f) 0 else 1)
    }
    return NumberFormat.getNumberInstance().apply { minimumFractionDigits = digits; maximumFractionDigits = digits }.format(value.toDouble())
}

private fun statUnit(text: String): String =
    text.replace(Regex("[\\p{Nd}\u2248\u2264\u066B\u066C,]"), "").trim { it == '.' || it.isWhitespace() }.ifBlank { text }

internal fun planFramesUnit(frames: Int): String = statUnit(TrimStrings.planFrames(frames))

internal fun planSizeUnit(mb: Float): String = statUnit(
    when (scaleFor(mb)) {
        SizeScale.TB -> TrimStrings.planSizeTb("0")
        SizeScale.GB -> TrimStrings.planSizeGb("0")
        SizeScale.MB -> TrimStrings.planSize("0")
    }
)

internal fun planVideosUnit(count: Int): String = statUnit(TrimStrings.batchVideos(count))

private fun stepFor(n: Int): Int = when {
    n <= 1 -> 1
    n == 2 -> 2
    n <= 4 -> 3
    n <= 10 -> 4
    else -> 6
}

internal fun pickPattern(mode: ExtractMode, cells: Int): BooleanArray {
    val out = BooleanArray(cells)
    fun mark(fraction: Float) { out[(fraction * (cells - 1)).toInt().coerceIn(0, cells - 1)] = true }
    when (mode) {
        ExtractMode.EveryFrame -> out.fill(true)
        is ExtractMode.EveryNFrames -> { var i = 0; val s = stepFor(mode.n); while (i < cells) { out[i] = true; i += s } }
        is ExtractMode.EveryNMillis -> { val s = (cells / 6).coerceAtLeast(2); var i = s / 2; while (i < cells) { out[i] = true; i += s } }
        is ExtractMode.FixedCount -> { val k = mode.count.coerceIn(2, 6); for (j in 0 until k) mark((j + 0.5f) / k) }
        ExtractMode.KeyframesOnly -> listOf(0.04f, 0.2f, 0.46f, 0.61f, 0.86f).forEach { mark(it) }
        ExtractMode.SceneChange -> listOf(0.2f, 0.47f, 0.74f).forEach { mark(it) }
        ExtractMode.Slides -> for (j in 0 until 3) out[(j * cells / 3).coerceIn(0, cells - 1)] = true
        is ExtractMode.Content -> listOf(0.12f, 0.41f, 0.58f, 0.9f).forEach { mark(it) }
    }
    return out
}

@Composable
internal fun PlanFilmStrip(mode: ExtractMode, modifier: Modifier = Modifier, height: Dp = 56.dp) {
    val colors = FrameTheme.colors
    val shape = FrameShapes.sm
    val well = if (colors.isDark) Color.Black.copy(alpha = 0.32f) else Color.Black.copy(alpha = 0.07f)
    Box(modifier.fillMaxWidth().height(height).clip(shape).background(well).border(1.dp, frameHairline(), shape)) {
        PlanStrip(mode, Modifier.fillMaxSize(), cells = 30, perforations = true)
    }
}

@Composable
internal fun PlanMeter(megabytes: Float, modifier: Modifier) {
    val colors = FrameTheme.colors
    val target = (ln(1f + megabytes) / ln(1f + METER_MAX_MB)).coerceIn(0.03f, 1f)
    val fill by animateFloatAsState(target, tween(550, easing = FastOutSlowInEasing), label = "planMeter")
    val a = colors.accentA
    val c = colors.accentC
    val warm = colors.warning
    val hot = colors.error
    val track = colors.onSurface.copy(alpha = 0.08f)
    Canvas(modifier) {
        val radius = CornerRadius(size.height / 2f)
        drawRoundRect(track, cornerRadius = radius)
        val fw = (size.width * fill).coerceAtLeast(1f)
        val pair = when {
            megabytes > METER_HOT_MB -> listOf(warm, hot)
            megabytes > METER_WARM_MB -> listOf(a, warm)
            else -> listOf(a, c)
        }
        drawRoundRect(Brush.horizontalGradient(pair, startX = 0f, endX = fw), size = Size(fw, size.height), cornerRadius = radius)
    }
}

@Composable
internal fun PlanStat(icon: Int, value: String, unit: String, modifier: Modifier = Modifier) {
    val colors = FrameTheme.colors
    val shape = FrameShapes.sm
    Column(
        modifier
            .clip(shape)
            .background(colors.onSurface.copy(alpha = 0.05f))
            .border(1.dp, frameHairline(), shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        Arrangement.spacedBy(4.dp)
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(6.dp), Alignment.CenterVertically) {
            Image(painterResource(icon), null, Modifier.size(14.dp), colorFilter = ColorFilter.tint(colors.primary))
            FrameText(unit, style = FrameTheme.typography.label, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            FrameRollingText(value, FrameTheme.typography.title, colors.onSurface)
        }
    }
}
