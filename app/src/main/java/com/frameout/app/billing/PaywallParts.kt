package com.frameout.app.billing

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.core.pro.ProFeature
import com.frameout.core.pro.ProStrings
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameHairline

@Composable
internal fun Kicker() {
    val colors = FrameTheme.colors
    Row(
        Modifier
            .background(colors.primary.copy(alpha = 0.13f), FrameShapes.pill)
            .border(1.dp, colors.primary.copy(alpha = 0.28f), FrameShapes.pill)
            .padding(horizontal = 11.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(painterResource(FrameIcons.StarFilled), null, Modifier.size(11.dp), colorFilter = ColorFilter.tint(colors.primary))
        FrameText(ProStrings.badge(), FrameTheme.typography.label.copy(fontSize = 11.sp), colors.primary, Modifier.padding(start = 5.dp))
    }
}

internal fun featureIcon(feature: ProFeature): Int = when (feature) {
    ProFeature.SmartExtract -> FrameIconsExtra.Detect
    ProFeature.Filters -> FrameIcons.Filter
    ProFeature.Shapes -> FrameIconsExtra.Shapes
    ProFeature.EditTools -> FrameIconsExtra.Brush
    ProFeature.ExportFormats -> FrameIconsExtra.ImageFormat
    ProFeature.AdvancedExport -> FrameIconsExtra.ExportOut
    ProFeature.Collage -> FrameIcons.Collage
    ProFeature.Batch -> FrameIcons.VideoAdd
    ProFeature.Compare -> FrameIconsExtra.Adjust
    ProFeature.Accents -> FrameIconsExtra.Palette
    ProFeature.AppIcons -> FrameIconsExtra.Cover
    ProFeature.Insights -> FrameIcons.FaceGood
    ProFeature.General -> FrameIcons.StarFilled
}

private fun trialsLine(trialsLeft: Int?): String? = when {
    trialsLeft == null -> null
    trialsLeft > 0 -> ProStrings.trialsLeft(trialsLeft)
    else -> ProStrings.noTrials()
}

@Composable
fun PaywallHeader(feature: ProFeature, trialsLeft: Int?, modifier: Modifier = Modifier, trialsVisible: Boolean = true) {
    val colors = FrameTheme.colors
    val general = feature == ProFeature.General
    val trials = trialsLine(trialsLeft)
    val title = FrameTheme.typography.display.copy(
        brush = Brush.horizontalGradient(listOf(colors.onSurface, colors.onSurface, colors.primary, colors.accentC)),
        fontSize = 24.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
    Column(modifier.fillMaxWidth().padding(horizontal = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (general) {
            BasicText(ProStrings.title(), Modifier.padding(top = 10.dp, bottom = 4.dp).semantics { heading() }, title)
            FrameText(ProStrings.subtitle(), FrameTheme.typography.bodySmall, colors.onSurfaceVariant, textAlign = TextAlign.Center)
        } else {
            Kicker()
            if (trials != null) {
                FrameText(
                    trials,
                    FrameTheme.typography.bodySmall,
                    colors.onSurfaceVariant,
                    Modifier.padding(top = 6.dp).graphicsLayer { alpha = if (trialsVisible) 1f else 0f },
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
