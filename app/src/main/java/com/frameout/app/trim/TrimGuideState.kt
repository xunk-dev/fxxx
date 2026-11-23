package com.frameout.app.trim

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import com.frameout.core.prefs.AppPrefs
import com.frameout.core.util.TrimStrings

internal enum class TrimGuideStep { Timeline, Precise, Transport, Extract }

internal fun TrimGuideStep.title(): String = when (this) {
    TrimGuideStep.Timeline -> TrimStrings.guideTimelineTitle()
    TrimGuideStep.Precise -> TrimStrings.guidePreciseTitle()
    TrimGuideStep.Transport -> TrimStrings.guideTransportTitle()
    TrimGuideStep.Extract -> TrimStrings.guideExtractTitle()
}

internal fun TrimGuideStep.body(): String = when (this) {
    TrimGuideStep.Timeline -> TrimStrings.guideTimelineBody()
    TrimGuideStep.Precise -> TrimStrings.guidePreciseBody()
    TrimGuideStep.Transport -> TrimStrings.guideTransportBody()
    TrimGuideStep.Extract -> TrimStrings.guideExtractBody()
}

private const val MIN_SIDE = 8f

internal class TrimGuideState(private val prefs: AppPrefs, enabled: Boolean) {
    private val bounds = mutableStateMapOf<TrimGuideStep, Rect>()
    var visible by mutableStateOf(enabled && !prefs.trimGuideSeen())
        private set
    var index by mutableIntStateOf(0)
        private set

    fun report(step: TrimGuideStep, rect: Rect) {
        if (bounds[step] != rect) bounds[step] = rect
    }

    fun boundsOf(step: TrimGuideStep): Rect? = bounds[step]

    fun steps(): List<TrimGuideStep> = TrimGuideStep.values().filter { step ->
        bounds[step]?.let { it.width > MIN_SIDE && it.height > MIN_SIDE } == true
    }

    fun next() {
        if (index >= steps().lastIndex) finish() else index++
    }

    fun finish() {
        prefs.markTrimGuideSeen()
        visible = false
    }
}

internal val LocalTrimGuide = compositionLocalOf<TrimGuideState?> { null }

@Composable
internal fun Modifier.guideTarget(step: TrimGuideStep): Modifier {
    val guide = LocalTrimGuide.current ?: return this
    if (!guide.visible) return this
    return onGloballyPositioned { guide.report(step, it.boundsInWindow()) }
}
