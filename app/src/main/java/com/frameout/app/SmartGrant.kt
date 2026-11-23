package com.frameout.app

import com.frameout.core.prefs.AppPrefs
import com.frameout.core.pro.ProAccess
import com.frameout.engine.extract.ExtractMode
import com.frameout.engine.extract.ExtractModeCodec
import com.frameout.engine.extract.GateFlags
import com.frameout.engine.extract.isSmart

internal class SmartGrant(val mode: ExtractMode, val gateFlags: Int)

private fun isSmartMode(mode: ExtractMode): Boolean = mode.isSmart()

internal fun resolveSmartGrant(prefs: AppPrefs, wanted: ExtractMode): SmartGrant {
    val blurry = prefs.skipBlurry.value
    val duplicates = prefs.skipDuplicates.value
    val people = prefs.peopleOnly.value
    val smartMode = isSmartMode(wanted)
    val needs = smartMode || blurry || duplicates || people
    val pro = ProAccess.isProActive()
    val usedTrial = needs && !pro && ProAccess.consumeSkipTrial()
    val ok = !needs || pro || usedTrial
    if (needs && !pro) resetSmartPrefs(prefs)
    val mode = if (ok || !smartMode) wanted else ExtractMode.EveryFrame
    return SmartGrant(mode, GateFlags.encode(ok && blurry, ok && duplicates, ok && people, prefs.peopleBody.value, granted = pro || usedTrial, trial = usedTrial))
}

private fun resetSmartPrefs(prefs: AppPrefs) {
    prefs.setSkipBlurry(false)
    prefs.setSkipDuplicates(false)
    prefs.setPeopleOnly(false)
    if (isSmartMode(ExtractModeCodec.decode(prefs.extractMode.value))) prefs.setExtractMode(ExtractModeCodec.encode(ExtractMode.EveryFrame))
}
