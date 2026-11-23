package com.frameout.app.update

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.frameout.core.util.UpdateStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.a11y.screenHeading
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameTheme

private val ButtonShape = RoundedCornerShape(18.dp)

@Composable
internal fun UpdateBody(info: UpdateInfo, onLater: () -> Unit, onUpdate: () -> Unit, modifier: Modifier = Modifier) {
    val colors = FrameTheme.colors
    Column(modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 22.dp)) {
        FrameText(
            UpdateStrings.title(),
            style = FrameTheme.typography.title.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.screenHeading()
        )
        FrameText(
            UpdateStrings.meta(info.sizeLabel),
            style = FrameTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(top = 3.dp)
        )
        NotesRow()
        Row(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LaterButton(onLater, Modifier.weight(1f))
            UpdateButton(onUpdate, Modifier.weight(1.6f))
        }
    }
}

@Composable
private fun NotesRow() {
    val colors = FrameTheme.colors
    Column(Modifier.fillMaxWidth().padding(top = 14.dp)) {
        FrameText(UpdateStrings.notesTitle(), style = FrameTheme.typography.label.copy(fontWeight = FontWeight.Bold))
        FrameText(UpdateStrings.notesBody(), style = FrameTheme.typography.bodySmall, color = colors.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun LaterButton(onClick: () -> Unit, modifier: Modifier) {
    val colors = FrameTheme.colors
    Box(
        modifier
            .heightIn(min = 54.dp)
            .clip(ButtonShape)
            .background(colors.onSurface.copy(alpha = 0.07f))
            .frameClickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        Alignment.Center
    ) {
        FrameText(UpdateStrings.later(), style = FrameTheme.typography.label, textAlign = TextAlign.Center)
    }
}

@Composable
private fun UpdateButton(onClick: () -> Unit, modifier: Modifier) {
    val colors = FrameTheme.colors
    Row(
        modifier
            .heightIn(min = 54.dp)
            .clip(ButtonShape)
            .background(colors.primary)
            .frameClickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        Image(painterResource(FrameIconsExtra.Download), null, Modifier.size(20.dp), colorFilter = ColorFilter.tint(colors.onPrimary))
        FrameText(UpdateStrings.now(), style = FrameTheme.typography.label.copy(fontWeight = FontWeight.Bold), color = colors.onPrimary)
    }
}
