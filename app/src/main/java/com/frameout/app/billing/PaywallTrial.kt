package com.frameout.app.billing

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.frameout.core.pro.ProStrings
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameHairline
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private fun dayLabel(offset: Int): String {
    val date: Date = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, offset) }.time
    val locale = Locale.getDefault()
    return SimpleDateFormat(android.text.format.DateFormat.getBestDateTimePattern(locale, "dMMM"), locale).format(date)
}

@Composable
fun TrialTimeline(offer: PlanOffer?, modifier: Modifier = Modifier) {
    val trial = offer?.freeTrial == true
    val fade by animateFloatAsState(if (trial) 1f else 0f, tween(220), label = "trialFade")
    Box(modifier) {
        TimelineCard(offer?.trialDays ?: 0, Modifier.graphicsLayer { alpha = fade }.hidden(!trial))
        SummaryCard(offer?.plan ?: ProPlan.Yearly, Modifier.graphicsLayer { alpha = 1f - fade }.hidden(trial))
    }
}

private fun Modifier.hidden(hide: Boolean): Modifier = if (hide) clearAndSetSemantics { } else this

@Composable
private fun CardFrame(title: String, modifier: Modifier, content: @Composable () -> Unit) {
    val colors = FrameTheme.colors
    Column(
        modifier
            .fillMaxWidth()
            .background(colors.onSurface.copy(alpha = 0.04f), FrameShapes.xl)
            .border(1.dp, frameHairline(), FrameShapes.xl)
            .padding(start = 10.dp, end = 10.dp, top = 9.dp, bottom = 8.dp)
    ) {
        FrameText(title, FrameTheme.typography.label.copy(fontSize = 11.sp), colors.onSurfaceVariant, Modifier.padding(start = 4.dp, bottom = 8.dp).semantics { heading() })
        content()
    }
}

@Composable
private fun TimelineCard(trialDays: Int, modifier: Modifier) {
    val days = trialDays.coerceAtLeast(1)
    val reminder = days >= 2
    CardFrame(ProStrings.tlTitle(), modifier) {
        Box(Modifier.fillMaxWidth()) {
            TimelineTrack(if (reminder) 3 else 2)
            Row(Modifier.fillMaxWidth()) {
                TimelineStep(true, ProStrings.tlToday(), ProStrings.tlTodayBody(), Modifier.weight(1f))
                if (reminder) TimelineStep(false, dayLabel(days - 1), ProStrings.tlReminder(), Modifier.weight(1f))
                TimelineStep(false, dayLabel(days), ProStrings.tlCharge(), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SummaryCard(plan: ProPlan, modifier: Modifier) {
    val title = when (plan) {
        ProPlan.Monthly -> ProStrings.planMonthlyHint()
        ProPlan.Yearly -> ProStrings.planYearlyHint()
        ProPlan.Lifetime -> ProStrings.oneTime()
    }
    CardFrame(title, modifier) {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(20.dp).background(FrameTheme.colors.primary, FrameShapes.pill), Alignment.Center) {
                AnimatedCheck(1f, FrameTheme.colors.onPrimary, Modifier.size(10.dp))
            }
            Column(Modifier.padding(start = 10.dp)) {
                FrameText(ProStrings.tlToday(), FrameTheme.typography.label.copy(fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold))
                FrameText(ProStrings.tlTodayBody(), FrameTheme.typography.bodySmall.copy(fontSize = 10.5.sp, lineHeight = 14.sp), FrameTheme.colors.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TimelineTrack(steps: Int) {
    val reduce = LocalSystemA11y.current.reduceMotion
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val progress = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) { if (!reduce) progress.animateTo(1f, tween(1400, 900, FastOutSlowInEasing)) }
    val colors = FrameTheme.colors
    val colorsInOrder = listOf(colors.accentC, colors.accentA, colors.accentB).let { if (rtl) it.reversed() else it }
    Box(Modifier.fillMaxWidth().padding(top = 9.dp), Alignment.TopCenter) {
        Box(Modifier.fillMaxWidth((steps - 1f) / steps).height(2.dp).background(frameHairline(), FrameShapes.pill)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .graphicsLayer {
                        scaleX = progress.value
                        transformOrigin = TransformOrigin(if (rtl) 1f else 0f, 0.5f)
                    }
                    .background(Brush.horizontalGradient(colorsInOrder), FrameShapes.pill)
            )
        }
    }
}

@Composable
private fun TimelineStep(done: Boolean, title: String, body: String, modifier: Modifier) {
    val colors = FrameTheme.colors
    Column(modifier.padding(horizontal = 3.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(20.dp)
                .background(if (done) colors.primary else colors.surface, FrameShapes.pill)
                .border(2.dp, colors.primary, FrameShapes.pill),
            Alignment.Center
        ) { if (done) AnimatedCheck(1f, colors.onPrimary, Modifier.size(10.dp)) }
        FrameText(title, FrameTheme.typography.label.copy(fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 7.dp))
        FrameText(body, FrameTheme.typography.bodySmall.copy(fontSize = 10.5.sp, lineHeight = 14.sp), colors.onSurfaceVariant, Modifier.padding(top = 1.dp), textAlign = TextAlign.Center)
    }
}
