package com.frameout.app.billing

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import com.frameout.designsystem.a11y.LocalSystemA11y
import kotlinx.coroutines.delay
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.app.R
import com.frameout.core.pro.ProFeature
import com.frameout.core.pro.ProStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme


internal class PerkChip(val art: Int, val label: String, val feature: ProFeature?)

internal fun perks(): List<PerkChip> = allPerks().let { all -> listOf(all[0], all[1], all[2], all[6]) }

internal fun allPerks(): List<PerkChip> = listOf(
    PerkChip(R.drawable.pro_art_noads, ProStrings.noAdsName(), null),
    PerkChip(R.drawable.pro_art_smart, ProStrings.featureName(ProFeature.SmartExtract), ProFeature.SmartExtract),
    PerkChip(R.drawable.pro_art_filters, ProStrings.featureName(ProFeature.Filters), ProFeature.Filters),
    PerkChip(R.drawable.pro_art_shapes, ProStrings.featureName(ProFeature.Shapes), ProFeature.Shapes),
    PerkChip(R.drawable.pro_art_edit, ProStrings.featureName(ProFeature.EditTools), ProFeature.EditTools),
    PerkChip(R.drawable.pro_art_formats, ProStrings.featureName(ProFeature.ExportFormats), ProFeature.ExportFormats),
    PerkChip(R.drawable.pro_art_export, ProStrings.featureName(ProFeature.AdvancedExport), ProFeature.AdvancedExport),
    PerkChip(R.drawable.pro_art_collage, ProStrings.featureName(ProFeature.Collage), ProFeature.Collage),
    PerkChip(R.drawable.pro_art_batch, ProStrings.featureName(ProFeature.Batch), ProFeature.Batch),
    PerkChip(R.drawable.pro_art_compare, ProStrings.featureName(ProFeature.Compare), ProFeature.Compare),
    PerkChip(R.drawable.pro_art_accents, ProStrings.featureName(ProFeature.Accents), ProFeature.Accents),
    PerkChip(R.drawable.pro_art_icons, ProStrings.featureName(ProFeature.AppIcons), ProFeature.AppIcons),
    PerkChip(R.drawable.pro_art_insights, ProStrings.featureName(ProFeature.Insights), ProFeature.Insights)
)

@Composable
fun ProHeroCard(
    pro: Boolean,
    onUpgrade: () -> Unit,
    onManage: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onRestore: (() -> Unit)? = null,
    note: String? = null,
    busy: Boolean = false,
    onPerk: ((ProFeature?) -> Unit)? = null
) {
    if (pro) {
        ProActiveCard(onManage, modifier)
        return
    }
    val shape = FrameShapes.xl
    val brush = proBrush()
    val ink = proOnBrush()
    val shimmer = rememberShimmer(3600, 800)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(brush)
            .border(1.dp, Color.White.copy(alpha = 0.35f), shape)
            .drawBehind {
                val c = Offset(size.width * 0.92f, 0f)
                drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.40f), Color.Transparent), c, size.width * 0.55f), size.width * 0.55f, c)
                listOf(0.30f to 0.10f, 0.40f to 0.22f, 0.06f to 0.45f).forEachIndexed { i, (x, y) ->
                    drawCircle(Color.White.copy(alpha = 0.50f - i * 0.10f), (1.6f + i * 0.4f).dp.toPx(), Offset(size.width * x, size.height * y))
                }
            }
            .frameClickable(role = Role.Button, onClick = onUpgrade)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            FrameText(
                ProStrings.settingsTitle(),
                FrameTheme.typography.title.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                ink,
                Modifier.weight(1f),
                maxLines = 1
            )
            Box(Modifier.size(32.dp).background(ink.copy(alpha = 0.14f), CircleShape).border(1.dp, ink.copy(alpha = 0.25f), CircleShape), Alignment.Center) {
                Image(painterResource(FrameIcons.StarFilled), null, Modifier.size(16.dp), colorFilter = ColorFilter.tint(ink))
            }
        }
        PerkSlides(ink, onPerk)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            UpgradeButton(ink, brush, shimmer, Modifier.weight(1f))
            if (onRestore != null) RestoreLink(ink, note, busy, onRestore, Modifier.widthIn(max = 104.dp))
        }
    }
}

private const val PERK_AUTO_MS = 3800L

@Composable
private fun PerkSlides(ink: Color, onPerk: ((ProFeature?) -> Unit)?) {
    val perks = remember { allPerks() }
    val pager = rememberPagerState { perks.size }
    val reduce = LocalSystemA11y.current.reduceMotion
    val dragged by pager.interactionSource.collectIsDraggedAsState()
    val slideHeight = 64.dp * LocalDensity.current.fontScale.coerceIn(1f, 1.6f)
    LaunchedEffect(pager.settledPage, dragged, reduce) {
        if (reduce || dragged) return@LaunchedEffect
        delay(PERK_AUTO_MS)
        if (!pager.isScrollInProgress) pager.animateScrollToPage((pager.settledPage + 1) % perks.size)
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalPager(pager, Modifier.fillMaxWidth(), key = { perks[it].label }) { page ->
            PerkSlide(perks[page], ink, slideHeight, onPerk)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            repeat(perks.size) { i ->
                val active = i == pager.currentPage
                val w by animateDpAsState(if (active) 16.dp else 5.dp, tween(220), label = "perkDot")
                Box(Modifier.width(w).height(5.dp).background(ink.copy(alpha = if (active) 0.95f else 0.35f), FrameShapes.pill))
            }
        }
    }
}

@Composable
private fun PerkSlide(perk: PerkChip, ink: Color, height: androidx.compose.ui.unit.Dp, onPerk: ((ProFeature?) -> Unit)?) {
    val open = if (onPerk != null) ({ onPerk(perk.feature) }) else null
    Row(
        Modifier
            .fillMaxWidth()
            .height(height)
            .clip(FrameShapes.md)
            .background(ink.copy(alpha = 0.12f))
            .border(1.dp, ink.copy(alpha = 0.18f), FrameShapes.md)
            .then(if (open != null) Modifier.frameClickable(onClickLabel = ProStrings.seeFeature(), role = Role.Button, onClick = open) else Modifier)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(44.dp).background(ink.copy(alpha = 0.16f), CircleShape).border(1.dp, ink.copy(alpha = 0.25f), CircleShape), Alignment.Center) {
            Image(painterResource(perk.art), null, Modifier.size(26.dp), colorFilter = ColorFilter.tint(ink))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            FrameText(perk.label, FrameTheme.typography.title.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold), ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            FrameText(
                perk.feature?.let { ProStrings.featureLine(it) } ?: ProStrings.noAdsLine(),
                FrameTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
                ink.copy(alpha = 0.85f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun UpgradeButton(ink: Color, brush: Brush, shimmer: androidx.compose.runtime.State<Float>, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(FrameShapes.pill)
            .background(ink)
            .shimmerSweep(shimmer, 0.35f)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val label = when {
            ink == ProGold.Ink -> ProGold.Light
            ink.luminance() > 0.5f -> Color(0xFF14123A)
            else -> Color.White
        }
        FrameText(ProStrings.settingsFree(), FrameTheme.typography.title.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold), label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Image(painterResource(FrameIconsExtra.Chevron), null, Modifier.padding(start = 4.dp).size(16.dp), colorFilter = ColorFilter.tint(label))
    }
}

@Composable
private fun RestoreLink(ink: Color, note: String?, busy: Boolean, onRestore: () -> Unit, modifier: Modifier = Modifier) {
    val clickable = note == null && !busy
    FrameText(
        note ?: ProStrings.restore(),
        FrameTheme.typography.label.copy(fontSize = 11.sp, lineHeight = 14.sp),
        ink,
        modifier
            .heightIn(min = 40.dp)
            .then(if (clickable) Modifier.frameClickable(role = Role.Button, onClick = onRestore) else Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            .wrapContentHeight(Alignment.CenterVertically)
            .padding(horizontal = 4.dp),
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}
