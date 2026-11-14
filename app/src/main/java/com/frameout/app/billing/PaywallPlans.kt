package com.frameout.app.billing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.core.pro.ProStrings
import com.frameout.designsystem.a11y.frameSelectable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameHairline

private val PlanOrder = listOf(ProPlan.Monthly, ProPlan.Yearly, ProPlan.Lifetime)
private const val FEATURED_WEIGHT = 1.14f

private val CompactShape = RoundedCornerShape(14.dp)

@Composable
internal fun ShrinkText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    minScale: Float = 0.55f
) {
    var scale by remember(text, style.fontSize) { mutableFloatStateOf(1f) }
    FrameText(
        text,
        style.copy(fontSize = style.fontSize * scale),
        color,
        modifier,
        textAlign,
        maxLines = 1,
        softWrap = false,
        onTextLayout = { if (it.hasVisualOverflow && scale > minScale) scale = (scale * 0.92f).coerceAtLeast(minScale) }
    )
}

private fun planName(plan: ProPlan) = when (plan) {
    ProPlan.Monthly -> ProStrings.monthly()
    ProPlan.Yearly -> ProStrings.yearly()
    ProPlan.Lifetime -> ProStrings.lifetime()
}

private fun planPer(plan: ProPlan) = when (plan) {
    ProPlan.Monthly -> ProStrings.perMonth()
    ProPlan.Yearly -> ProStrings.perYear()
    ProPlan.Lifetime -> ProStrings.perOnce()
}

@Composable
private fun Ribbon(saving: Int, modifier: Modifier) {
    val text = if (saving > 0) ProStrings.bestSave(saving) else ProStrings.bestValue()
    ShrinkText(
        text,
        FrameTheme.typography.label.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
        proOnBrush(),
        modifier.background(proBrush(), FrameShapes.xs).padding(horizontal = 9.dp, vertical = 3.dp)
    )
}

@Composable
private fun planBarHeight(): androidx.compose.ui.unit.Dp = PLAN_BAR_HEIGHT * LocalDensity.current.fontScale.coerceIn(1f, 1.35f)

private val PLAN_BAR_HEIGHT = 72.dp

@Composable
fun CompactPlanBar(offers: List<PlanOffer>, selected: ProPlan?, onPick: (ProPlan) -> Unit, modifier: Modifier = Modifier) {
    val saving = BillingCatalog.savingsPercent(
        offers.firstOrNull { it.plan == ProPlan.Monthly }?.micros ?: 0L,
        offers.firstOrNull { it.plan == ProPlan.Yearly }?.micros ?: 0L
    )
    Row(
        modifier.fillMaxWidth().padding(top = 8.dp).height(planBarHeight()).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        if (offers.isEmpty()) repeat(3) { Box(Modifier.weight(1f).fillMaxHeight().clip(CompactShape).background(FrameTheme.colors.onSurface.copy(alpha = 0.06f))) }
        offers.sortedBy { PlanOrder.indexOf(it.plan) }.forEach { offer ->
            val weight = if (offer.plan == ProPlan.Yearly) FEATURED_WEIGHT else 1f
            CompactPlan(offer, offer.plan == selected, saving, Modifier.weight(weight).fillMaxHeight()) { onPick(offer.plan) }
        }
    }
}

@Composable
private fun CompactPlan(offer: PlanOffer, selected: Boolean, saving: Int, modifier: Modifier, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    Box(modifier) {
        Column(
            Modifier
                .fillMaxSize()
                .background(if (selected) colors.primary.copy(alpha = 0.14f) else colors.onSurface.copy(alpha = 0.05f), CompactShape)
                .border(if (selected) 1.6.dp else 1.dp, if (selected) colors.primary else frameHairline(), CompactShape)
                .clip(CompactShape)
                .frameSelectable(selected, Role.RadioButton, onClick)
                .padding(horizontal = 4.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ShrinkText(planName(offer.plan), FrameTheme.typography.body.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold), colors.onSurface, textAlign = TextAlign.Center)
            ShrinkText(offer.price, FrameTheme.typography.mono.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium), colors.onSurface, textAlign = TextAlign.Center)
            ShrinkText(planPer(offer.plan), FrameTheme.typography.bodySmall.copy(fontSize = 10.sp), colors.onSurfaceVariant, textAlign = TextAlign.Center)
            if (offer.freeTrial && offer.trialDays > 0) {
                ShrinkText(
                    ProStrings.planTrialHint(offer.trialDays),
                    FrameTheme.typography.label.copy(fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold),
                    colors.primary,
                    Modifier.padding(top = 2.dp),
                    TextAlign.Center
                )
            }
        }
        if (selected) {
            Box(
                Modifier.align(Alignment.TopStart).padding(start = 6.dp, top = 14.dp).size(15.dp).background(colors.primary, CircleShape),
                Alignment.Center
            ) { AnimatedCheck(1f, colors.onPrimary, Modifier.size(9.dp)) }
        }
        if (offer.plan == ProPlan.Yearly) Ribbon(saving, Modifier.align(Alignment.TopCenter).offset(y = (-9).dp))
    }
}
