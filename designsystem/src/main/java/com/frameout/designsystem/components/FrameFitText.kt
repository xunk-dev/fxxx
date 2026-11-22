package com.frameout.designsystem.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import com.frameout.designsystem.a11y.LocalSystemA11y
import kotlinx.coroutines.delay

private const val FIT_STEP = 0.04f

private class Fit(val scale: Float, val overflow: Boolean)

private fun TextStyle.scaled(s: Float): TextStyle = copy(
    fontSize = if (fontSize.isSpecified) fontSize * s else fontSize,
    lineHeight = if (lineHeight.isSpecified) lineHeight * s else lineHeight
)

private fun composeText(text: String, unit: String?, unitStyle: TextStyle, unitColor: Color, s: Float): AnnotatedString =
    buildAnnotatedString {
        append(text)
        if (!unit.isNullOrEmpty()) {
            append(' ')
            withStyle(
                SpanStyle(
                    fontSize = if (unitStyle.fontSize.isSpecified) unitStyle.fontSize * s else TextUnit.Unspecified,
                    fontWeight = unitStyle.fontWeight,
                    color = unitColor
                )
            ) { append(unit) }
        }
    }

private fun solveFit(
    measurer: TextMeasurer,
    text: String,
    unit: String?,
    style: TextStyle,
    unitStyle: TextStyle,
    unitColor: Color,
    maxWidth: Int,
    minScale: Float
): Fit {
    var s = 1f
    while (true) {
        val width = measurer.measure(
            composeText(text, unit, unitStyle, unitColor, s),
            style = style.scaled(s),
            softWrap = false,
            maxLines = 1
        ).size.width
        if (width <= maxWidth) return Fit(s, false)
        if (s <= minScale) return Fit(minScale, true)
        s = (s - FIT_STEP).coerceAtLeast(minScale)
    }
}

@Composable
fun FrameFitText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    unit: String? = null,
    unitStyle: TextStyle = style,
    unitColor: Color = color,
    description: String? = null,
    minScale: Float = 0.7f,
    alignment: Alignment = Alignment.Center
) {
    val measurer = rememberTextMeasurer()
    val reduceMotion = LocalSystemA11y.current.reduceMotion
    val spoken = description ?: if (unit.isNullOrEmpty()) text else "$text $unit"
    BoxWithConstraints(modifier.clearAndSetSemantics { contentDescription = spoken }, alignment) {
        val maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else Int.MAX_VALUE
        val fit = remember(text, unit, style, unitStyle, unitColor, maxWidth, minScale) {
            solveFit(measurer, text, unit, style, unitStyle, unitColor, maxWidth, minScale)
        }
        val shown = composeText(text, unit, unitStyle, unitColor, fit.scale)
        val shownStyle = style.scaled(fit.scale).copy(color = color)
        when {
            !fit.overflow -> BasicText(shown, style = shownStyle, maxLines = 1, softWrap = false)
            reduceMotion -> BasicText(
                shown, Modifier.fillMaxWidth(), shownStyle,
                overflow = TextOverflow.Ellipsis, softWrap = false, maxLines = 1
            )
            else -> FitMarquee(shown, shownStyle)
        }
    }
}

@Composable
private fun FitMarquee(text: AnnotatedString, style: TextStyle) {
    val scroll = rememberMarqueeScroll(text)
    Box(Modifier.fillMaxWidth().edgeFade()) {
        BasicText(
            text,
            Modifier.horizontalScroll(scroll, enabled = false),
            style,
            softWrap = false,
            maxLines = 1
        )
    }
}
