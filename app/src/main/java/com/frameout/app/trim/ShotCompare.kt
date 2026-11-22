package com.frameout.app.trim

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.frameout.app.compare.CompareZoom
import com.frameout.app.compare.PaneItem
import com.frameout.app.compare.SliderCompare
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.engine.timeline.formatTimestamp

private fun paneOf(tag: String, shot: Shot): PaneItem =
    PaneItem(tag, shot.preview, formatTimestamp(shot.positionMs), "", tag)

@Composable
internal fun ShotCompareDialog(a: Shot, b: Shot, onClose: () -> Unit) {
    val zoom = remember { CompareZoom() }
    val panes = remember(a.id, b.id) { paneOf("A", a) to paneOf("B", b) }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxSize().background(FrameTheme.colors.background).padding(12.dp)) {
            Box(Modifier.fillMaxWidth(), Alignment.CenterEnd) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(FrameTheme.colors.container).frameClickable(onClick = onClose),
                    Alignment.Center
                ) { FrameText("\u2715", style = FrameTheme.typography.title, color = FrameTheme.colors.onSurface) }
            }
            SliderCompare(panes.first, panes.second, zoom, Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp))
        }
    }
}
