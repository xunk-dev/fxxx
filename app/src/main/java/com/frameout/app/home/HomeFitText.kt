package com.frameout.app.home

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.frameout.designsystem.components.FrameFit
import com.frameout.designsystem.components.FrameText

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
    FrameText(
        text,
        style,
        color,
        modifier,
        textAlign,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        softWrap = maxLines > 1,
        fit = FrameFit(minScale = minScale)
    )
}

private const val FitStep = 0.5f

private fun noSpaceScript(c: Char): Boolean = when (Character.UnicodeScript.of(c.code)) {
    Character.UnicodeScript.HAN, Character.UnicodeScript.HIRAGANA, Character.UnicodeScript.KATAKANA,
    Character.UnicodeScript.THAI, Character.UnicodeScript.LAO, Character.UnicodeScript.KHMER,
    Character.UnicodeScript.MYANMAR -> true
    else -> false
}

private fun TextLayoutResult.breaksInsideWord(text: String): Boolean {
    for (i in 0 until lineCount - 1) {
        val end = getLineEnd(i)
        if (end in 1 until text.length) {
            val a = text[end - 1]
            val b = text[end]
            if (a.isLetterOrDigit() && b.isLetterOrDigit() && !noSpaceScript(a) && !noSpaceScript(b)) return true
        }
    }
    return false
}

private fun fitStyle(style: TextStyle, size: Float): TextStyle {
    val ratio = if (style.fontSize.isSp && style.lineHeight.isSp && style.fontSize.value > 0f) style.lineHeight.value / style.fontSize.value else 1.2f
    return style.copy(fontSize = size.sp, lineHeight = (size * ratio).sp)
}

private fun fitFontSize(
    measurer: TextMeasurer,
    text: String,
    style: TextStyle,
    minSize: Float,
    maxSize: Float,
    maxLines: Int,
    width: Int,
    height: Int,
    direction: LayoutDirection
): TextStyle {
    fun fits(size: Float): Boolean {
        val r = measurer.measure(
            text,
            fitStyle(style, size),
            overflow = TextOverflow.Ellipsis,
            softWrap = maxLines > 1,
            maxLines = maxLines,
            constraints = Constraints(maxWidth = width),
            layoutDirection = direction
        )
        if (r.didOverflowHeight || r.didOverflowWidth) return false
        if (height != Constraints.Infinity && r.size.height > height) return false
        return !r.breaksInsideWord(text)
    }
    if (width == Constraints.Infinity || width <= 0) return fitStyle(style, maxSize)
    var lo = minSize
    var hi = maxSize
    if (fits(hi)) return fitStyle(style, hi)
    if (!fits(lo)) return fitStyle(style, lo)
    while (hi - lo > FitStep) {
        val mid = (lo + hi) / 2f
        if (fits(mid)) lo = mid else hi = mid
    }
    return fitStyle(style, lo)
}

@Composable
internal fun FitParagraph(
    text: String,
    style: TextStyle,
    color: Color,
    minSize: TextUnit,
    maxSize: TextUnit,
    maxLines: Int,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null
) {
    val measurer = rememberTextMeasurer()
    val direction = LocalLayoutDirection.current
    val density = LocalDensity.current
    BoxWithConstraints(modifier) {
        val w = constraints.maxWidth
        val h = constraints.maxHeight
        val fitted = remember(text, style, w, h, minSize, maxSize, maxLines, density.density, density.fontScale, direction) {
            fitFontSize(measurer, text, style, minSize.value, maxSize.value, maxLines, w, h, direction)
        }
        FrameText(text, fitted, color, Modifier, textAlign, maxLines = maxLines, overflow = TextOverflow.Ellipsis, softWrap = maxLines > 1, fit = FrameFit.None)
    }
}
