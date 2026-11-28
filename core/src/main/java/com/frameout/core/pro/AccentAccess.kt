package com.frameout.core.pro

enum class AccentLock { Open, Ad, Pro }

object AccentAccess {
    fun lock(
        key: String,
        enforced: Boolean,
        isPro: Boolean,
        welcomeKey: String?,
        trialActive: Boolean,
        adsEnabled: Boolean,
        rewarded: Boolean
    ): AccentLock = when {
        !enforced || isPro || ProCatalog.accentIsFree(key, welcomeKey) -> AccentLock.Open
        trialActive && ProCatalog.welcomePickable(key) -> AccentLock.Open
        ProCatalog.accentIsProOnly(key) || !adsEnabled -> AccentLock.Pro
        rewarded -> AccentLock.Open
        else -> AccentLock.Ad
    }

    fun canClaimWelcome(key: String, enforced: Boolean, isPro: Boolean, welcomeOpen: Boolean): Boolean =
        enforced && !isPro && welcomeOpen && ProCatalog.welcomePickable(key)
}
