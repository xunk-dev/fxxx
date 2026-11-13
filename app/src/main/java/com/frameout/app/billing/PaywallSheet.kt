package com.frameout.app.billing

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.frameout.core.pro.ProFeature
import com.frameout.core.pro.ProShowcase
import com.frameout.core.pro.ProStrings
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.theme.FrameAccent
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameHairline
import kotlinx.coroutines.launch

private val SheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
private const val REVEAL_MS = 1500f

@Stable
class SheetMotion(val enter: Animatable<Float, AnimationVector1D>, val reveal: Animatable<Float, AnimationVector1D>, val close: () -> Unit)

@Composable
fun rememberSheetMotion(onDismiss: () -> Unit): SheetMotion {
    val reduce = LocalSystemA11y.current.reduceMotion
    val enter = remember { Animatable(if (reduce) 1f else 0f) }
    val reveal = remember { Animatable(if (reduce) 1f else 0f) }
    val scope = rememberCoroutineScope()
    val dismiss by rememberUpdatedState(onDismiss)
    LaunchedEffect(Unit) {
        if (reduce) return@LaunchedEffect
        launch { enter.animateTo(1f, spring(0.82f, Spring.StiffnessLow)) }
        reveal.animateTo(1f, tween(REVEAL_MS.toInt(), easing = LinearEasing))
    }
    return remember {
        var closing = false
        SheetMotion(enter, reveal) {
            if (!closing) {
                closing = true
                scope.launch { if (!reduce) enter.animateTo(0f, tween(220)); dismiss() }
            }
        }
    }
}

fun Modifier.revealed(reveal: Animatable<Float, AnimationVector1D>, index: Int): Modifier = graphicsLayer {
    val local = ((reveal.value * REVEAL_MS - (260f + 70f * index)) / 600f).coerceIn(0f, 1f)
    alpha = local
    translationY = (1f - local) * 16.dp.toPx()
}

@Composable
fun PaywallSheet(
    feature: ProFeature,
    trialsLeft: Int?,
    manager: BillingManager,
    onLaunch: (PlanOffer) -> Unit,
    onDismiss: () -> Unit,
    initialAccent: FrameAccent? = null
) {
    val offers by manager.offers.collectAsState()
    val state by manager.state.collectAsState()
    val busy by manager.busy.collectAsState()
    var chosen by remember { mutableStateOf(ProPlan.Yearly) }
    val selected = offers.firstOrNull { it.plan == chosen } ?: offers.firstOrNull()
    var picked by remember(initialAccent) { mutableStateOf(initialAccent) }
    val motion = rememberSheetMotion(onDismiss)
    val shortScreen = LocalConfiguration.current.screenHeightDp < 480
    val footer: @Composable () -> Unit = {
        Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 0.dp, bottom = 2.dp)) {
            CompactPlanBar(offers, selected?.plan, { chosen = it })
            PaywallCta(
                selected, state, busy, RevenueCatSync.isActive,
                { selected?.let(onLaunch) }, manager::retry, Modifier.padding(top = 6.dp), compact = true
            )
            PaywallFooterCompact(selected, manager)
        }
    }
    Dialog(onDismissRequest = motion.close, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        RetintedTheme(picked) {
            SheetFrame(motion) {
                PaywallBody(feature, trialsLeft, motion, Modifier.weight(1f), if (shortScreen) footer else null) { picked = it }
                if (!shortScreen) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(frameHairline()))
                    Column(Modifier.padding(top = 6.dp).navigationBarsPadding()) { footer() }
                }
            }
        }
    }
}

@Composable
private fun SheetFrame(motion: SheetMotion, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val colors = FrameTheme.colors
    val glow = if (colors.isDark) 0.34f else 0.18f
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { translationY = (1f - motion.enter.value) * size.height }
            .background(colors.surface)
            .drawBehind {
                val stops = listOf(colors.accentA.copy(alpha = glow), Color.Transparent)
                drawRect(Brush.radialGradient(stops, Offset(size.width / 2f, 0f), size.width * 0.8f))
            },
        Alignment.TopCenter
    ) {
        Box(Modifier.widthIn(max = 560.dp).fillMaxSize()) {
            Column(Modifier.fillMaxSize(), content = content)
            SheetChrome(motion.close)
        }
    }
}

@Composable
private fun BoxScope.SheetChrome(onClose: () -> Unit) {
    val colors = FrameTheme.colors
    Box(
        Modifier
            .align(Alignment.TopEnd)
            .statusBarsPadding()
            .padding(6.dp)
            .size(48.dp)
            .frameClickable(onClickLabel = ProStrings.notNow(), onClick = onClose),
        Alignment.Center
    ) {
        Box(Modifier.size(32.dp).background(colors.onSurface.copy(alpha = 0.1f), RoundedCornerShape(50)), Alignment.Center) {
            Image(painterResource(FrameIcons.Close), null, Modifier.size(14.dp), colorFilter = ColorFilter.tint(colors.onSurface))
        }
    }
}

@Composable
private fun PaywallBody(
    feature: ProFeature,
    trialsLeft: Int?,
    motion: SheetMotion,
    modifier: Modifier,
    footer: (@Composable () -> Unit)?,
    onAccent: (FrameAccent) -> Unit
) {
    val reveal = motion.reveal
    val scroll = rememberScrollState()
    val slides = remember { proSlides() }
    val start = remember(feature) { slideIndexOf(slides, feature.key).coerceAtLeast(0) }
    val pager = rememberPagerState(initialPage = start) { slides.size }
    val scope = rememberCoroutineScope()
    val currentKey = slides.getOrNull(pager.currentPage)?.key
    Column(modifier.verticalScroll(scroll).padding(bottom = 8.dp)) {
        Spacer(Modifier.statusBarsPadding().height(6.dp))
        ProSlideshow(pager, slides, onAccent, autoPlay = feature == ProFeature.General)
        PaywallHeader(feature, trialsLeft, Modifier.revealed(reveal, 0), trialsVisible = currentKey == feature.key)
        ProFeatureList(motion, currentKey, Modifier.padding(top = 12.dp)) { picked ->
            val index = slideIndexOf(slides, picked)
            if (index >= 0) scope.launch {
                scroll.animateScrollTo(0)
                pager.animateScrollToPage(index)
            }
        }
        if (footer != null) {
            Spacer(Modifier.height(10.dp))
            footer()
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}
