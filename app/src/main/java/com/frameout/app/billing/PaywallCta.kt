package com.frameout.app.billing

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.app.BuildConfig
import com.frameout.core.pro.ProStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.FrameSpinner
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import kotlinx.coroutines.delay

private fun roll(): ContentTransform =
    (slideInVertically(tween(220)) { it / 2 } + fadeIn(tween(220))) togetherWith
        (slideOutVertically(tween(160)) { -it / 2 } + fadeOut(tween(120)))

internal fun ctaLabel(offer: PlanOffer?): String = when {
    offer == null -> ProStrings.loading()
    offer.plan == ProPlan.Lifetime -> ProStrings.ctaLifetime(offer.price)
    offer.freeTrial && offer.trialDays > 0 -> ProStrings.ctaTrial(offer.trialDays)
    offer.plan == ProPlan.Yearly -> ProStrings.ctaYear(offer.price)
    else -> ProStrings.ctaMonth(offer.price)
}

internal fun pricePerPeriod(offer: PlanOffer): String = when (offer.plan) {
    ProPlan.Monthly -> offer.price + " " + ProStrings.perMonth()
    ProPlan.Yearly -> offer.price + " " + ProStrings.perYear()
    ProPlan.Lifetime -> offer.price
}

internal fun fineText(offer: PlanOffer?): String = when {
    offer == null -> ""
    offer.plan == ProPlan.Lifetime -> ProStrings.fineLifetime(offer.price)
    offer.freeTrial -> ProStrings.fineTrial(pricePerPeriod(offer))
    offer.plan == ProPlan.Yearly -> ProStrings.fineYear(offer.price)
    else -> ProStrings.fineMonth(offer.price)
}

@Composable
fun PaywallCta(
    offer: PlanOffer?,
    state: OffersState,
    busy: Boolean,
    canRetry: Boolean,
    onClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val shimmer = rememberShimmer(3200, 1200)
    val ink = proOnBrush()
    val failed = offer == null && state == OffersState.Failed
    val enabled = !busy && (offer != null || (failed && canRetry))
    val label = when {
        busy -> ProStrings.processing()
        failed -> ProStrings.retry()
        else -> ctaLabel(offer)
    }
    val working = busy || (offer == null && !failed)
    Box(
        modifier
            .fillMaxWidth()
            .then(if (compact) Modifier.height(48.dp) else Modifier.heightIn(min = 56.dp))
            .alpha(if (enabled || busy) 1f else 0.55f)
            .clip(FrameShapes.pill)
            .background(proBrush())
            .shimmerSweep(shimmer, if (enabled) 0.55f else 0f)
            .frameClickable(enabled = enabled, role = Role.Button, onClick = if (offer == null) onRetry else onClick)
            .padding(horizontal = 16.dp, vertical = if (compact) 4.dp else 14.dp),
        Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (working) FrameSpinner(Modifier.size(18.dp), 18.dp, ink, 2.dp)
            AnimatedContent(label, transitionSpec = { roll() }, label = "ctaLabel") { text ->
                FrameText(text, FrameTheme.typography.title.copy(fontSize = if (compact) 15.sp else 16.sp, fontWeight = FontWeight.Bold), ink, textAlign = TextAlign.Center, maxLines = 2)
            }
        }
    }
}

private fun noteText(note: BillingNote): String? = when (note) {
    BillingNote.Error -> ProStrings.billingError()
    BillingNote.Network -> ProStrings.billingNetwork()
    BillingNote.AlreadyOwned -> ProStrings.billingOwned()
    BillingNote.Unavailable -> ProStrings.unavailable()
    BillingNote.Pending -> ProStrings.pending()
    BillingNote.Restored -> ProStrings.restored()
    BillingNote.NothingToRestore -> ProStrings.nothingToRestore()
    BillingNote.None -> null
}

private fun isProblem(note: BillingNote) = note == BillingNote.Error || note == BillingNote.Network || note == BillingNote.Unavailable

private fun openLink(context: android.content.Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

@Composable
fun PaywallFooterCompact(offer: PlanOffer?, manager: BillingManager) {
    val note by manager.note.collectAsState()
    val busy by manager.busy.collectAsState()
    LaunchedEffect(note) {
        if (note != BillingNote.None && note != BillingNote.Pending) {
            delay(6000)
            manager.clearNote()
        }
    }
    val noteLine = noteText(note)
    val problem = isProblem(note)
    val line = StatusLine(noteLine ?: offerFine(offer), noteLine != null, problem)
    val slot = with(LocalDensity.current) { (STATUS_LINE_HEIGHT * STATUS_LINES).toDp() }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().padding(top = 3.dp).height(slot).padding(horizontal = 10.dp), Alignment.Center) {
            AnimatedContent(line, transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) }, label = "billingStatus") { shown ->
                StatusText(shown)
            }
        }
        CompactLinks(manager, busy)
    }
}

private val STATUS_LINE_HEIGHT = 12.sp
private const val STATUS_LINES = 2
private const val MIN_STATUS_SCALE = 0.78f

private data class StatusLine(val text: String, val isNote: Boolean, val problem: Boolean)

private fun offerFine(offer: PlanOffer?): String = fineText(offer)

@Composable
private fun StatusText(line: StatusLine) {
    val colors = FrameTheme.colors
    var scale by remember(line.text) { mutableFloatStateOf(1f) }
    val base = if (line.isNote) FrameTheme.typography.label.copy(fontSize = 10.5.sp) else FrameTheme.typography.bodySmall.copy(fontSize = 10.sp)
    val style = base.copy(fontSize = base.fontSize * scale, lineHeight = STATUS_LINE_HEIGHT)
    val color = when {
        !line.isNote -> colors.onSurfaceVariant
        line.problem -> colors.error
        else -> colors.primary
    }
    FrameText(
        line.text,
        style,
        color,
        if (line.isNote) Modifier.semantics { liveRegion = LiveRegionMode.Polite } else Modifier,
        TextAlign.Center,
        maxLines = STATUS_LINES,
        onTextLayout = { if (it.hasVisualOverflow && scale > MIN_STATUS_SCALE) scale = (scale * 0.94f).coerceAtLeast(MIN_STATUS_SCALE) }
    )
}

@Composable
private fun CompactLinks(manager: BillingManager, busy: Boolean) {
    val context = LocalContext.current
    val muted = FrameTheme.colors.onSurfaceVariant.copy(alpha = 0.8f)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        CompactLink(ProStrings.restore(), FrameTheme.colors.primary, enabled = !busy) { manager.restore() }
        if (BuildConfig.TERMS_URL.isNotBlank()) CompactLink(ProStrings.terms(), muted) { openLink(context, BuildConfig.TERMS_URL) }
        if (BuildConfig.PRIVACY_URL.isNotBlank()) CompactLink(ProStrings.privacy(), muted) { openLink(context, BuildConfig.PRIVACY_URL) }
    }
}

@Composable
private fun CompactLink(label: String, color: androidx.compose.ui.graphics.Color, enabled: Boolean = true, onClick: () -> Unit) {
    FrameText(
        label,
        FrameTheme.typography.label.copy(fontSize = 11.sp),
        color,
        Modifier.alpha(if (enabled) 1f else 0.5f).frameClickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(vertical = 5.dp, horizontal = 2.dp),
        maxLines = 2
    )
}
