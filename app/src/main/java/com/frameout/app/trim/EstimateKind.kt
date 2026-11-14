package com.frameout.app.trim

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.frameout.core.prefs.AppPrefs
import com.frameout.engine.extract.ExtractMode
import com.frameout.engine.extract.decidesContent

internal enum class EstimateKind { Exact, UpperBound, Approx }

internal fun estimateKind(mode: ExtractMode, skipBlurry: Boolean, skipDuplicates: Boolean, peopleOnly: Boolean): EstimateKind = when {
    !isExactEstimate(mode) -> EstimateKind.Approx
    mode.decidesContent() -> EstimateKind.UpperBound
    skipBlurry || skipDuplicates || peopleOnly -> EstimateKind.UpperBound
    else -> EstimateKind.Exact
}

@Composable
internal fun estimateKindFor(prefs: AppPrefs, mode: ExtractMode): EstimateKind {
    val blurry by prefs.skipBlurry.collectAsState()
    val duplicates by prefs.skipDuplicates.collectAsState()
    val people by prefs.peopleOnly.collectAsState()
    return estimateKind(mode, blurry, duplicates, people)
}
