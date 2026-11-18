package com.frameout.app.frames

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.frameout.app.thumb.ThumbPlaceholder
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme

@Composable
fun FrameSkeleton() {
    Box(Modifier.aspectRatio(1f).padding(2.dp).background(FrameTheme.colors.container, FrameShapes.sm)) {
        ThumbPlaceholder(pulsing = true)
    }
}
