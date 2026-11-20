package com.frameout.core.pro

import com.frameout.core.edit.BrushCoverMark
import com.frameout.core.edit.CoverMark
import com.frameout.core.edit.EditMark
import com.frameout.core.edit.FilterPresets
import com.frameout.core.edit.FrameEdit
import com.frameout.core.edit.ShapeMark
import com.frameout.core.edit.TextMark

object ProEditGuard {
    class Lock(val feature: ProFeature, val tier: ProTier)

    private fun filterLock(id: String): Lock? {
        if (id == FilterPresets.NONE) return null
        val tier = ProCatalog.filterTier(id)
        return if (ProAccess.allows(ProFeature.Filters, tier)) null else Lock(ProFeature.Filters, tier)
    }

    fun lockOf(mark: EditMark): Lock? {
        val (feature, tier) = when (mark) {
            is TextMark -> return null
            is CoverMark -> ProFeature.EditTools to ProCatalog.coverTier(mark.style)
            is BrushCoverMark -> ProFeature.EditTools to ProCatalog.coverTier(mark.style)
            is ShapeMark -> ProFeature.Shapes to ProCatalog.shapeTier(mark.kind)
            else -> return null
        }
        return if (ProAccess.allows(feature, tier)) null else Lock(feature, tier)
    }

    private fun allLocks(edit: FrameEdit): List<Lock> =
        listOfNotNull(filterLock(edit.filter)) + edit.marks.mapNotNull { lockOf(it) }

    fun firstLock(edit: FrameEdit): Lock? = allLocks(edit).maxByOrNull { it.tier.ordinal }

    fun firstLocked(edit: FrameEdit): ProFeature? = firstLock(edit)?.feature

    fun sanitize(edit: FrameEdit): FrameEdit {
        if (firstLock(edit) == null) return edit
        val filter = if (filterLock(edit.filter) != null) FilterPresets.NONE else edit.filter
        return edit.copy(filter = filter, marks = edit.marks.filter { lockOf(it) == null })
    }

    class LockReport(val pro: Int, val ad: Int, val proFeature: ProFeature?, val adFeatures: Set<ProFeature>) {
        val mixed: Boolean get() = pro > 0 && ad > 0
    }

    fun report(edit: FrameEdit): LockReport? = report(listOf(edit))

    fun report(edits: Collection<FrameEdit>): LockReport? {
        val locks = edits.flatMap { allLocks(it) }
        if (locks.isEmpty()) return null
        val pro = locks.filter { it.tier == ProTier.Pro }
        val ad = locks.filter { it.tier == ProTier.Ad }
        return LockReport(pro.size, ad.size, pro.firstOrNull()?.feature, ad.map { it.feature }.toCollection(LinkedHashSet()))
    }

    private fun open(lock: Lock) {
        ProAccess.source?.showPaywall(lock.feature, adEligible = lock.tier == ProTier.Ad)
    }

    fun enforce(mark: EditMark): Boolean {
        val lock = lockOf(mark) ?: return true
        open(lock)
        return false
    }

    fun enforceText(): Boolean = true
}
