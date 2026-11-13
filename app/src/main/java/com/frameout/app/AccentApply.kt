package com.frameout.app

internal fun FrameOutApp.applyAccent(key: String) {
    container.prefs.setAccent(key)
    container.entitlements.accentSeal.set(key)
}

internal fun FrameOutApp.guardAccent() {
    val entitlements = container.entitlements
    if (!entitlements.enforced || !container.prefs.onboardingDone()) return
    val restore = entitlements.accentSeal.restoreTarget(container.prefs.accent.value)
    if (restore != null) container.prefs.setAccent(restore) else entitlements.accentSeal.adopt(container.prefs.accent.value)
}

internal fun FrameOutApp.settleAccentTrial() {
    container.entitlements.settleTrial(container.prefs.accent.value)
}
