package com.frameout.designsystem.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import com.frameout.designsystem.a11y.LocalSystemA11y
import kotlinx.coroutines.delay

@Immutable
data class FrameFit(
    val minScale: Float = FitMath.DEFAULT_MIN_SCALE,
    val scroll: Boolean = true,
    val minSp: Float = FitMath.DEFAULT_MIN_SP
) {
    val active: Boolean get() = minScale < 1f || scroll

    companion object {
        val Auto = FrameFit()

        val None = FrameFit(minScale = 1f, scroll = false)

        val ShrinkOnly = FrameFit(scroll = false)

        val ScrollOnly = FrameFit(minScale = 1f, scroll = true)
    }
}

private class FitState {
    var scale by mutableFloatStateOf(1f)
    var scroll by mutableStateOf(false)
    var solvedWidth = -1
}

internal fun TextStyle.fitScaled(s: Float): TextStyle = copy(
    fontSize = if (fontSize.isSpecified) fontSize * s else fontSize,
    lineHeight = if (lineHeight.isSpecified) lineHeight * s else lineHeight,
    letterSpacing = if (letterSpacing.isSpecified) letterSpacing * s else letterSpacing
)

private fun TextLayoutResult.breaksInsideWord(text: CharSequence): Boolean {
    for (i in 0 until lineCount - 1) {
        if (FitMath.splitsWord(text, getLineEnd(i))) return true
    }
    return false
}


@Composable
fun FrameAutoText(
    text: AnnotatedString,
    style: TextStyle,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    fit: FrameFit = FrameFit.Auto
) {
    if (!fit.active) {
        BasicText(
            text = text, modifier = modifier, style = style, onTextLayout = onTextLayout,
            overflow = overflow, softWrap = softWrap, maxLines = maxLines, minLines = minLines
        )
        return
    }
    val reduceMotion = LocalSystemA11y.current.reduceMotion
    val fontScale = LocalDensity.current.fontScale
    val floor = FitMath.floorScale(fit.minScale, fit.minSp, if (style.fontSize.isSp) style.fontSize.value else Float.NaN)
    val plain = text.text
    val singleLine = maxLines == 1 || (!softWrap && '\n' !in plain)
    val st = remember(
        plain, style.fontSize, style.lineHeight, style.fontFamily, style.fontWeight, style.letterSpacing,
        maxLines, softWrap, fit, fontScale, reduceMotion
    ) { FitState() }

    val onLayout: (TextLayoutResult) -> Unit = { r ->
        onTextLayout(r)
        if (!st.scroll) {
            val width = r.layoutInput.constraints.maxWidth
            if (st.scale < 1f && FitMath.roomGrew(st.solvedWidth, width)) {
                st.solvedWidth = width
                st.scale = 1f
            } else if (r.hasVisualOverflow || (softWrap && r.breaksInsideWord(plain))) {
                val natural = r.multiParagraph.maxIntrinsicWidth
                if (!FitMath.atFloor(st.scale, floor)) {
                    st.solvedWidth = width
                    st.scale = FitMath.nextScale(st.scale, floor, width, natural, singleLine)
                } else if (singleLine && fit.scroll && !reduceMotion && width != Int.MAX_VALUE && natural > width + 0.5f) {
                    st.solvedWidth = width
                    st.scroll = true
                }
            }
        }
    }

    val shown = if (st.scale < 1f) style.fitScaled(st.scale) else style
    if (st.scroll) {
        FitScroller(text, shown, modifier) { width ->
            if (FitMath.roomGrew(st.solvedWidth, width)) {
                st.solvedWidth = width
                st.scale = 1f
                st.scroll = false
            }
        }
    } else {
        val ellipsize = overflow == TextOverflow.Clip && FitMath.atFloor(st.scale, floor)
        BasicText(
            text = text, modifier = modifier, style = shown, onTextLayout = onLayout,
            overflow = if (ellipsize) TextOverflow.Ellipsis else overflow,
            softWrap = softWrap, maxLines = maxLines, minLines = minLines
        )
    }
}

@Composable
private fun FitScroller(text: AnnotatedString, style: TextStyle, modifier: Modifier, onWidth: (Int) -> Unit) {
    val scroll = rememberMarqueeScroll(text)
    Box(modifier.onSizeChanged { onWidth(it.width) }.edgeFade()) {
        BasicText(
            text = text,
            modifier = Modifier.horizontalScroll(scroll, enabled = false),
            style = style,
            softWrap = false,
            maxLines = 1
        )
    }
}
