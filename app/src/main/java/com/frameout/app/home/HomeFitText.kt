package com.frameout.app.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.isSpecified
import com.frameout.designsystem.components.FrameText

private const val ShrinkStep = 0.92f

private fun TextStyle.scaled(scale: Float): TextStyle = copy(
    fontSize = if (fontSize.isSpecified) fontSize * scale else fontSize,
    lineHeight = if (lineHeight.isSpecified) lineHeight * scale else lineHeight
)

@Composable
internal fun FitText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
    minScale: Float = 0.7f,
    textAlign: TextAlign? = null
) {
    var scale by remember(text, style.fontSize, maxLines) { mutableFloatStateOf(1f) }
    FrameText(
        text,
        style.scaled(scale),
        color,
        modifier,
        textAlign,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        softWrap = maxLines > 1,
        onTextLayout = {
            if (it.hasVisualOverflow && scale > minScale) scale = (scale * ShrinkStep).coerceAtLeast(minScale)
        }
    )
}
