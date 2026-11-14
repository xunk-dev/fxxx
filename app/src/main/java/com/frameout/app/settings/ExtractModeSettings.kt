package com.frameout.app.settings

import androidx.compose.runtime.Composable
import com.frameout.core.prefs.AppPrefs
import com.frameout.core.quality.content.ContentSpec
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.ModeStrings
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.engine.extract.ExtractMode
import com.frameout.engine.extract.ExtractModeCodec

internal val ModeKinds = listOf("every_frame", "every_n", "every_time", "keyframes", "fixed", "scene", "slides", "content")

internal fun kindOf(mode: ExtractMode) = when (mode) {
    ExtractMode.EveryFrame -> "every_frame"
    is ExtractMode.EveryNFrames -> "every_n"
    is ExtractMode.EveryNMillis -> "every_time"
    ExtractMode.KeyframesOnly -> "keyframes"
    is ExtractMode.FixedCount -> "fixed"
    ExtractMode.SceneChange -> "scene"
    ExtractMode.Slides -> "slides"
    is ExtractMode.Content -> "content"
}

internal fun modeFor(kind: String, previous: ExtractMode) = when (kind) {
    "every_n" -> ExtractMode.EveryNFrames(if (previous is ExtractMode.EveryNFrames) previous.n else 5)
    "every_time" -> ExtractMode.EveryNMillis(if (previous is ExtractMode.EveryNMillis) previous.millis else 1000L)
    "fixed" -> ExtractMode.FixedCount(if (previous is ExtractMode.FixedCount) previous.count else 30)
    "keyframes" -> ExtractMode.KeyframesOnly
    "scene" -> ExtractMode.SceneChange
    "slides" -> ExtractMode.Slides
    "content" -> ExtractMode.Content(if (previous is ExtractMode.Content) previous.spec else ContentSpec())
    else -> ExtractMode.EveryFrame
}

@Composable
fun ExtractModeSettings(prefs: AppPrefs, stored: String, onOpenSheet: () -> Unit) {
    val mode = ExtractModeCodec.decode(stored)
    NavRow(FrameIconsExtra.Keyframe, FrameStrings.defaultMode(), value = com.frameout.app.trim.planText(mode), onClick = onOpenSheet)
    SettingsHint(ModeStrings.modeHelp(kindOf(mode)))
}

internal fun isSmartKind(kind: String): Boolean = kind == "scene" || kind == "slides" || kind == "content"

internal fun gateMode(kind: String, action: () -> Unit) = gateSkip(isSmartKind(kind), action)

internal fun gateSkip(on: Boolean, action: () -> Unit) {
    val source = com.frameout.core.pro.ProAccess.source
    if (!on || source == null) action() else source.gateSkip(action)
}
