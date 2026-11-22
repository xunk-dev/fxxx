package com.frameout.designsystem.components

import com.frameout.designsystem.util.safeCoerceIn

internal object FitMath {
    const val STEP = 0.93f
    const val EPSILON = 0.002f
    const val GROW_TOLERANCE = 1.08f
    const val DEFAULT_MIN_SCALE = 0.6f
    const val DEFAULT_MIN_SP = 9f
    private const val WIDTH_SLACK = 0.985f
    private const val MAX_WORD = 32

    fun floorScale(minScale: Float, minSp: Float, fontSizeSp: Float): Float {
        val relative = minScale.coerceIn(0.1f, 1f)
        if (fontSizeSp.isNaN() || fontSizeSp <= 0f) return relative
        return maxOf(relative, (minSp / fontSizeSp).coerceAtMost(1f))
    }

    fun atFloor(scale: Float, floor: Float): Boolean = scale <= floor + EPSILON

    fun nextScale(current: Float, floor: Float, maxWidth: Int, naturalWidth: Float, singleLine: Boolean): Float {
        if (current <= floor + EPSILON) return floor
        val stepped = current * STEP
        val bounded = maxWidth > 0 && maxWidth != Int.MAX_VALUE
        val target = if (singleLine && bounded && naturalWidth > 0f) current * (maxWidth / naturalWidth) * WIDTH_SLACK else stepped
        return minOf(stepped, target).safeCoerceIn(floor, current)
    }

    fun roomGrew(solvedWidth: Int, width: Int): Boolean =
        solvedWidth > 0 && width != Int.MAX_VALUE && width > solvedWidth * GROW_TOLERANCE

    fun noSpaceScript(codePoint: Int): Boolean = when (Character.UnicodeScript.of(codePoint)) {
        Character.UnicodeScript.HAN, Character.UnicodeScript.HIRAGANA, Character.UnicodeScript.KATAKANA,
        Character.UnicodeScript.THAI, Character.UnicodeScript.LAO, Character.UnicodeScript.KHMER,
        Character.UnicodeScript.MYANMAR -> true
        else -> false
    }

    private fun isMark(c: Char): Boolean {
        val t = Character.getType(c)
        return t == Character.NON_SPACING_MARK.toInt() || t == Character.COMBINING_SPACING_MARK.toInt() || t == Character.ENCLOSING_MARK.toInt()
    }

    private fun isWordChar(c: Char): Boolean =
        c.isLetterOrDigit() || isMark(c) || c == '-' || c == '\'' || c == '’' || c == '‐' || c == '‑' ||
            c == '.' || c == ',' || c == ';' || c == ':' || c == '!' || c == '?' || c == '(' || c == ')' || c == '«' ||
            c == '»' || c == '"' || c == '“' || c == '”' || c == '…'

    fun splitsWord(text: CharSequence, end: Int): Boolean {
        if (end <= 0 || end >= text.length) return false
        val a = text[end - 1]
        val b = text[end]
        if (!a.isLetterOrDigit() || !b.isLetterOrDigit()) return false
        if (noSpaceScript(a.code) || noSpaceScript(b.code)) return false
        var from = end
        while (from > 0 && !text[from - 1].isWhitespace() && end - from <= MAX_WORD) from--
        var to = end
        while (to < text.length && !text[to].isWhitespace() && to - end <= MAX_WORD) to++
        if (to - from > MAX_WORD) return false
        for (i in from until to) if (!isWordChar(text[i])) return false
        return true
    }
}
