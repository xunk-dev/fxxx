package com.frameout.app.trim

import com.frameout.core.pro.ProEditGuard
import com.frameout.core.pro.ProFeature
import com.frameout.core.pro.ProTier
import com.frameout.engine.timeline.TrimRange

internal data class TrimSnapshot(val range: TrimRange, val output: TrimOutput)

internal object TrimDirty {
    fun sameRange(a: TrimRange, b: TrimRange): Boolean = a.startMs == b.startMs && a.endMs == b.endMs

    fun changed(opened: TrimSnapshot?, current: TrimSnapshot): Boolean =
        opened != null && (opened.output != current.output || !sameRange(opened.range, current.range))
}

internal data class OutputLock(val output: TrimOutput, val tier: ProTier, val feature: ProFeature, val fallback: TrimOutput) {
    fun toReport(): ProEditGuard.LockReport {
        val pro = tier == ProTier.Pro
        val ad = tier == ProTier.Ad
        return ProEditGuard.LockReport(
            pro = if (pro) 1 else 0,
            ad = if (ad) 1 else 0,
            proFeature = if (pro) feature else null,
            adFeatures = if (ad) linkedSetOf(feature) else emptySet()
        )
    }
}

internal object TrimLock {
    fun check(output: TrimOutput, allows: (ProFeature, ProTier) -> Boolean): OutputLock? {
        if (output.tier == ProTier.Free) return null
        if (allows(ProFeature.AdvancedExport, output.tier)) return null
        return OutputLock(output, output.tier, ProFeature.AdvancedExport, freeFallback(output))
    }

    fun freeFallback(output: TrimOutput): TrimOutput = if (output.tier == ProTier.Free) output else TrimOutput.Video
}

internal object ClipNotes {
    fun join(base: String, notes: List<String?>): String =
        (listOf(base) + notes.filterNotNull().filter { it.isNotBlank() }).joinToString(" · ")
}
