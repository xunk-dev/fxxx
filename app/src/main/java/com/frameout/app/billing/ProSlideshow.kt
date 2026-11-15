package com.frameout.app.billing

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.icons.FrameIcons
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.frameout.core.pro.ProFeature
import com.frameout.core.pro.ProShowcase
import com.frameout.core.pro.ProStrings
import com.frameout.core.pro.ShowcaseKind
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.theme.FrameAccent
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import kotlinx.coroutines.delay

private const val AUTO_MS = 5500L
private val CaptionHeight = 62.dp

const val NO_ADS_KEY = "noads"

class ProSlide(val key: String, val feature: ProFeature?, val kind: ShowcaseKind?)

private val SlideOrder = listOf(
    ProFeature.SmartExtract, ProFeature.Filters, ProFeature.Shapes, ProFeature.EditTools,
    ProFeature.ExportFormats, ProFeature.AdvancedExport, ProFeature.Collage, ProFeature.Batch,
    ProFeature.Compare, ProFeature.Accents, ProFeature.AppIcons, ProFeature.Insights
)

fun proSlides(): List<ProSlide> =
    SlideOrder.mapNotNull { feature -> ProShowcase.kindOf(feature)?.let { ProSlide(feature.key, feature, it) } } + ProSlide(NO_ADS_KEY, null, null)

fun slideIndexOf(slides: List<ProSlide>, key: String): Int = slides.indexOfFirst { it.key == key }

@Composable
fun ProSlideshow(pager: PagerState, slides: List<ProSlide>, onAccent: (FrameAccent) -> Unit, modifier: Modifier = Modifier, autoPlay: Boolean = true) {
    val reduce = LocalSystemA11y.current.reduceMotion
    val dragged by pager.interactionSource.collectIsDraggedAsState()
    var paused by remember { mutableStateOf(!autoPlay) }
    val captionHeight = CaptionHeight * LocalDensity.current.fontScale.coerceIn(1f, 2f)
    LaunchedEffect(pager.settledPage, dragged, reduce, paused) {
        if (reduce || dragged || paused || slides.size < 2) return@LaunchedEffect
        delay(AUTO_MS)
        if (!pager.isScrollInProgress) pager.animateScrollToPage((pager.settledPage + 1) % slides.size)
    }
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalPager(pager, Modifier.fillMaxWidth(), key = { slides[it].key }) { page ->
            SlidePage(slides[page], captionHeight, onAccent)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            SlideDots(pager.currentPage, slides.size)
            if (!reduce && slides.size > 1) PauseToggle(paused) { paused = !paused }
        }
    }
}

@Composable
private fun SlidePage(slide: ProSlide, captionHeight: Dp, onAccent: (FrameAccent) -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (slide.kind != null) PaywallShowcase(slide.kind, FrameTheme.accent, onAccent) else PaywallNoAdsShowcase()
        Column(Modifier.height(captionHeight).padding(horizontal = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            FrameText(slide.feature?.let { ProStrings.featureName(it) } ?: ProStrings.noAdsName(), FrameTheme.typography.titleSmall, FrameTheme.colors.onSurface, textAlign = TextAlign.Center, maxLines = 1)
            FrameText(
                slide.feature?.let { ProStrings.featureLine(it) } ?: ProStrings.noAdsLine(),
                FrameTheme.typography.bodySmall,
                FrameTheme.colors.onSurfaceVariant,
                Modifier.padding(top = 2.dp),
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PauseToggle(paused: Boolean, onToggle: () -> Unit) {
    Box(
        Modifier.frameClickable(onClickLabel = if (paused) ProStrings.playSlides() else ProStrings.pauseSlides(), role = Role.Button, onClick = onToggle),
        Alignment.Center
    ) {
        Image(
            painterResource(if (paused) FrameIcons.Play else FrameIcons.Pause), null,
            Modifier.padding(start = 8.dp).size(14.dp),
            colorFilter = ColorFilter.tint(FrameTheme.colors.onSurfaceVariant)
        )
    }
}

@Composable
private fun SlideDots(current: Int, count: Int) {
    val colors = FrameTheme.colors
    val position = ProStrings.slideOf(current + 1, count)
    Row(Modifier.padding(top = 2.dp).semantics { contentDescription = position }, horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { index ->
            val active = index == current
            val w by animateDpAsState(if (active) 18.dp else 6.dp, tween(220), label = "dotWidth")
            Box(Modifier.width(w).height(6.dp).background(if (active) colors.primary else colors.onSurface.copy(alpha = 0.22f), FrameShapes.pill))
        }
    }
}
