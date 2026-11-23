package com.frameout.app.trim

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.frameout.core.util.FrameA11y
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.TrimStrings
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.theme.FrameTheme

@Composable
internal fun BoxScope.TimesSheet(env: TrimEnv, visible: Boolean) {
    TrimSheetHost(visible, FrameA11y.timeline(), { env.onSheet(TrimSheet.None) }) {
        val base = env.clamped
        TimeRow(FrameStrings.from(), base.startMs, false) { env.moveEdge(Handle.Start, it, false) }
        TimeRow(FrameStrings.to(), base.endMs, false) { env.moveEdge(Handle.End, it, false) }
        TimeRow(TrimStrings.rangeLength(), base.spanMs(), true) { env.setLength(it) }
    }
}

@Composable
private fun TimeRow(label: String, valueMs: Long, emphasis: Boolean, onCommit: (Long) -> Unit) {
    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp), Alignment.CenterVertically) {
        FrameText(
            label,
            style = FrameTheme.typography.label.copy(textDirection = TextDirection.Content),
            color = FrameTheme.colors.onSurfaceVariant,
            modifier = Modifier.width(80.dp),
            maxLines = 1
        )
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            TimeEntry(valueMs, Modifier.weight(1f), flat = true, emphasis = emphasis, onCommit = onCommit)
        }
    }
}
