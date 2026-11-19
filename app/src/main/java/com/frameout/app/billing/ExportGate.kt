package com.frameout.app.billing

import com.frameout.core.pro.ProAccess
import com.frameout.core.pro.ProFeature

object ExportGate {
    fun permit(): Boolean {
        val entitlements = ProAccess.source ?: return false
        if (entitlements.exportPermitted()) return true
        entitlements.showPaywall(ProFeature.AdvancedExport)
        return false
    }

    fun begin(): Boolean {
        val entitlements = ProAccess.source ?: return false
        if (entitlements.beginExportBatch()) return true
        entitlements.showPaywall(ProFeature.AdvancedExport)
        return false
    }

    fun end() {
        ProAccess.source?.endExportBatch()
    }

    fun gated(run: () -> Unit) {
        if (ProCheck.passOrAd(ProFeature.AdvancedExport) { run() }) run()
    }
}
