package com.frameout.app.viewer

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.icons.FrameIconsExtra

internal class LandscapeToggle(val active: Boolean, val toggle: () -> Unit)

@Composable
internal fun rememberLandscapeToggle(): LandscapeToggle {
    var active by rememberSaveable { mutableStateOf(false) }
    return LandscapeToggle(active) { active = !active }
}

internal fun Modifier.landscapeRotated(active: Boolean): Modifier = if (!active) this else this
    .layout { measurable, constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val placeable = measurable.measure(Constraints.fixed(height, width))
        layout(width, height) { placeable.place((width - placeable.width) / 2, (height - placeable.height) / 2) }
    }
    .graphicsLayer { rotationZ = 90f }

@Composable
internal fun LandscapeSegment(toggle: LandscapeToggle) {
    val label = FrameStrings.landscapeView()
    StripSegment(onClick = toggle.toggle, selected = toggle.active, description = label, square = true) {
        Image(
            painter = painterResource(FrameIconsExtra.Landscape),
            contentDescription = label,
            modifier = Modifier.size(18.dp),
            colorFilter = ColorFilter.tint(if (toggle.active) ViewerInk.accent else ViewerInk.text)
        )
    }
}
