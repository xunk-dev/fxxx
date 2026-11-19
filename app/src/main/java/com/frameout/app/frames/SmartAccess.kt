package com.frameout.app.frames

import com.frameout.core.db.ProjectEntity
import com.frameout.core.pro.Entitlements
import com.frameout.engine.extract.GateFlags

internal enum class SmartVerdict { Allowed, NeedsReextract, SpendTrial, NeedsGate }

internal object SmartAccess {
    fun decide(
        enforced: Boolean,
        pro: Boolean,
        trialArmed: Boolean,
        flags: Int,
        createdAt: Long,
        stampSince: Long
    ): SmartVerdict = when {
        !enforced -> SmartVerdict.Allowed
        GateFlags.trial(flags) -> SmartVerdict.Allowed
        pro -> if (GateFlags.granted(flags) || createdAt < stampSince) SmartVerdict.Allowed else SmartVerdict.NeedsReextract
        trialArmed -> SmartVerdict.SpendTrial
        else -> SmartVerdict.NeedsGate
    }

    fun decide(e: Entitlements, project: ProjectEntity?): SmartVerdict {
        if (project == null) return if (!e.enforced) SmartVerdict.Allowed else SmartVerdict.NeedsGate
        return decide(e.enforced, e.isPro.value, e.skipTrialArmed(), project.gateFlags, project.createdAt, e.smartStampSince())
    }

    fun allowedSilently(e: Entitlements, project: ProjectEntity?): Boolean = decide(e, project) == SmartVerdict.Allowed
}
