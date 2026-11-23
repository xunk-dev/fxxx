package com.frameout.core.pro

object ProAccess {
    @Volatile var source: Entitlements? = null

    fun permits(feature: ProFeature): Boolean = source?.permits(feature) ?: false

    fun allows(feature: ProFeature, tier: ProTier): Boolean {
        if (tier == ProTier.Free) return true
        return source?.allows(feature, tier) ?: false
    }

    fun isProActive(): Boolean = source?.isPro?.value ?: false

    fun consumeSkipTrial(): Boolean = source?.consumeSkipTrial() ?: false

    fun formatAllowed(format: com.frameout.core.storage.ImageFormat): Boolean =
        ProCatalog.formatIsFree(format) || permits(ProFeature.ExportFormats)

    fun applyVerifiedEntitlement(active: Boolean, plan: String) {
        source?.applyBillingResult(active, plan)
    }
}
