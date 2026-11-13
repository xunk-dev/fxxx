package com.frameout.designsystem.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.frameout.designsystem.R
import java.util.Locale

val FrameFontFamily = FontFamily(
    Font(R.font.ibm_plex_sans_arabic_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_sans_arabic_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_sans_arabic_semibold, FontWeight.SemiBold)
)
val FrameMonoFamily: FontFamily = FontFamily.Monospace
data class FrameTypography(
    val display: TextStyle, val title: TextStyle, val titleSmall: TextStyle, val body: TextStyle,
    val bodySmall: TextStyle, val label: TextStyle, val mono: TextStyle, val monoSmall: TextStyle
)
fun frameTypography(language: String = Locale.getDefault().language): FrameTypography {
    val f = FrameScript.family(language)
    val m = FrameMonoFamily
    val k = FrameScript.lineHeightScale(language)
    val style = if (FrameScript.isTall(language)) LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None) else null
    fun text(family: FontFamily, weight: FontWeight, size: Int, line: Int) = TextStyle(
        fontFamily = family, fontWeight = weight, fontSize = size.sp, lineHeight = (line * (if (family === m) 1f else k)).sp,
        lineHeightStyle = if (family === m) null else style
    )
    return FrameTypography(
        text(f, FontWeight.SemiBold, 28, 34),
        text(f, FontWeight.Medium, 20, 26),
        text(f, FontWeight.Medium, 16, 22),
        text(f, FontWeight.Normal, 15, 22),
        text(f, FontWeight.Normal, 13, 18),
        text(f, FontWeight.Medium, 12, 16),
        text(m, FontWeight.Normal, 14, 18),
        text(m, FontWeight.Normal, 11, 14)
    )
}
