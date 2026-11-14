package com.frameout.app.onboarding

import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.app.BuildConfig
import com.frameout.app.settings.openLegalLink
import com.frameout.core.pro.ProStrings
import com.frameout.designsystem.theme.FrameTheme

private fun legalAnnotated(
    note: String,
    termsLabel: String,
    privacyLabel: String,
    terms: String,
    privacy: String,
    primary: Color,
    muted: Color,
    context: Context
): AnnotatedString {
    val linkStyles = TextLinkStyles(
        style = SpanStyle(
            color = primary,
            fontWeight = FontWeight.Medium,
            textDecoration = TextDecoration.Underline
        )
    )
    return buildAnnotatedString {
        append(note)
        if (terms.isNotBlank()) {
            append(" ")
            withLink(
                LinkAnnotation.Clickable("terms", linkStyles) { openLegalLink(context, terms) }
            ) { append(termsLabel) }
        }
        if (terms.isNotBlank() && privacy.isNotBlank()) {
            append("  ")
            pushStyle(SpanStyle(color = muted.copy(alpha = 0.6f)))
            append("\u2022")
            pop()
            append("  ")
        } else if (privacy.isNotBlank()) {
            append(" ")
        }
        if (privacy.isNotBlank()) {
            withLink(
                LinkAnnotation.Clickable("privacy", linkStyles) { openLegalLink(context, privacy) }
            ) { append(privacyLabel) }
        }
    }
}

@Composable
internal fun OnboardingLegalNote(modifier: Modifier = Modifier) {
    val terms = BuildConfig.TERMS_URL
    val privacy = BuildConfig.PRIVACY_URL
    if (terms.isBlank() && privacy.isBlank()) return
    val context = LocalContext.current
    val baseStyle = FrameTheme.typography.titleSmall.copy(fontSize = 12.sp, lineHeight = 20.sp)
    val muted = FrameTheme.colors.onSurfaceVariant
    val primary = FrameTheme.colors.primary
    val note = ProStrings.legalNote()
    val termsLabel = ProStrings.terms()
    val privacyLabel = ProStrings.privacy()
    val text = remember(note, termsLabel, privacyLabel, terms, privacy, primary, muted) {
        legalAnnotated(note, termsLabel, privacyLabel, terms, privacy, primary, muted, context)
    }
    BasicText(
        text = text,
        style = baseStyle.copy(color = muted, textAlign = TextAlign.Center),
        modifier = modifier.fillMaxWidth().padding(vertical = 6.dp)
    )
}
