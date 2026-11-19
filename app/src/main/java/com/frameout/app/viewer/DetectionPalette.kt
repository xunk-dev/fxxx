package com.frameout.app.viewer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.frameout.core.quality.TagGroup
import com.frameout.core.quality.TagNames
import com.frameout.designsystem.theme.FrameTheme

private const val ACCENT_LIFT = 0.3f

internal class DetectionPalette(private val colors: Map<TagGroup, Color>, val ink: Color, val neutral: Color) {
    fun colorOf(detection: Detection): Color = colors.getValue(TagNames.group(detection.tags.first()))
}

@Composable
internal fun rememberDetectionPalette(): DetectionPalette {
    val theme = FrameTheme.colors
    val accent = FrameTheme.accent
    return remember(theme, accent) {
        fun lift(c: Color) = lerp(c, Color.White, ACCENT_LIFT)
        DetectionPalette(
            mapOf(
                TagGroup.People to theme.onDarkAccent,
                TagGroup.Things to lift(theme.accentA),
                TagGroup.Categories to lift(theme.accentB),
                TagGroup.Text to lift(theme.accentC),
                TagGroup.Codes to lift(theme.success)
            ),
            accent.darkOnPrimary,
            theme.onSurfaceVariant
        )
    }
}
