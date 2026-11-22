package com.frameout.app.trim

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.engine.player.PlayerController
import com.frameout.engine.player.frameCount
import com.frameout.engine.player.frameNumberAt
import com.frameout.engine.timeline.formatTimestamp
import kotlinx.coroutines.delay

@Composable
internal fun rememberFrameCount(controller: PlayerController): Int {
    val total by produceState(0, controller) {
        while (value == 0) {
            value = controller.frameCount()
            if (value == 0) delay(300L)
        }
    }
    return total
}

@Composable
internal fun FrameReadout(controller: PlayerController, modifier: Modifier = Modifier) {
    val position by controller.position.collectAsState()
    val total = rememberFrameCount(controller)
    val number = controller.frameNumberAt(position)
    if (total <= 0 || number <= 0) return
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        FrameText(
            "#$number / $total  \u00B7  ${formatTimestamp(position)}",
            style = FrameTheme.typography.label,
            color = FrameTheme.colors.onSurfaceVariant,
            maxLines = 1,
            modifier = modifier
        )
    }
}
