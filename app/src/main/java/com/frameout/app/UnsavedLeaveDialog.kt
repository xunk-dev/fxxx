package com.frameout.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.frameout.core.util.EditStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.DialogButton
import com.frameout.designsystem.components.DialogTone
import com.frameout.designsystem.components.FrameDialog
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme

@Composable
fun UnsavedLeaveDialog(onSaveLeave: () -> Unit, onLeave: () -> Unit, onStay: () -> Unit) {
    val colors = FrameTheme.colors
    FrameDialog(
        onDismiss = onStay,
        tone = DialogTone.Warning,
        title = EditStrings.unsavedTitle(),
        body = EditStrings.unsavedBody(),
        icon = FrameIconsExtra.Warning,
        primary = DialogButton(EditStrings.unsavedSaveLeave(), onSaveLeave),
        secondary = DialogButton(EditStrings.unsavedLeave(), onLeave),
        content = {
            Column(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                FrameText(
                    EditStrings.keepEditing(),
                    style = FrameTheme.typography.label,
                    color = colors.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.clip(FrameShapes.pill).frameClickable(onClick = onStay).padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    )
}
