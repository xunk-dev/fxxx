package com.frameout.designsystem.theme

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

val FrameRailCompactWidth: Dp = 72.dp
val FrameRailExpandedWidth: Dp = 220.dp
val FrameContentMaxWidth: Dp = 840.dp

private val RAIL_GUTTER = 24.dp
private val REFERENCE_WIDTH = 400.dp
private const val MAX_COLUMNS = 16

enum class FrameRail { None, Compact, Expanded }

@Immutable
class FrameAdaptive(val width: Dp, val height: Dp, val isTv: Boolean) {
    val size: FrameWindowSize get() = frameWindowSize(width)

    val landscape: Boolean get() = width > height

    val sideEditor: Boolean get() = landscape && (height < 500.dp || width >= 840.dp)

    val rail: FrameRail
        get() = when (size) {
            FrameWindowSize.Compact -> FrameRail.None
            FrameWindowSize.Medium -> FrameRail.Compact
            FrameWindowSize.Expanded -> FrameRail.Expanded
        }

    val railInset: Dp
        get() = when (rail) {
            FrameRail.None -> 0.dp
            FrameRail.Compact -> FrameRailCompactWidth + RAIL_GUTTER
            FrameRail.Expanded -> FrameRailExpandedWidth + RAIL_GUTTER
        }

    val usableWidth: Dp get() = (width - railInset).coerceAtLeast(0.dp)

    fun columns(base: Int): Int {
        val factor = usableWidth.value / REFERENCE_WIDTH.value
        return (base * factor).roundToInt().coerceIn(base, MAX_COLUMNS.coerceAtLeast(base))
    }

    fun listColumns(): Int = when {
        usableWidth >= 1100.dp -> 3
        usableWidth >= 700.dp -> 2
        else -> 1
    }
}

@Composable
fun rememberFrameAdaptive(): FrameAdaptive {
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val tv = remember(context) {
        (context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager)?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }
    return remember(configuration.screenWidthDp, configuration.screenHeightDp, tv) {
        FrameAdaptive(configuration.screenWidthDp.dp, configuration.screenHeightDp.dp, tv)
    }
}

@Composable
fun Modifier.frameRailPadding(): Modifier = padding(end = rememberFrameAdaptive().railInset)

fun Modifier.frameMaxWidth(max: Dp = FrameContentMaxWidth): Modifier =
    wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = max).fillMaxWidth()
