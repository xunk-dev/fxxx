package com.frameout.app.billing

import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer

fun Modifier.glint(progress: State<Float>, tint: Color = ProGold.Light): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val p = progress.value
        if (p >= 0f) {
            val x = -size.width * 0.6f + size.width * 1.6f * p
            val band = listOf(Color.Transparent, tint.copy(alpha = 0.9f), Color.White, tint.copy(alpha = 0.9f), Color.Transparent)
            drawRect(
                Brush.linearGradient(band, Offset(x, 0f), Offset(x + size.width * 0.5f, size.height * 0.6f)),
                blendMode = BlendMode.SrcAtop
            )
        }
    }
