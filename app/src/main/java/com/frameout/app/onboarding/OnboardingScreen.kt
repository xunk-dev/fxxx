package com.frameout.app.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.offset as offsetBy
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.frameout.app.FrameOutApp
import com.frameout.app.PreferredFrameTheme
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.motion.FlowEase
import com.frameout.designsystem.motion.LocalPageActive
import com.frameout.designsystem.motion.frameSpring
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameAmbientGlow
import kotlin.math.absoluteValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal enum class OnboardingPage { Welcome, Hero, Formats, Tools, Pick, Ready }

internal val CopyReserve = 176.dp

@Composable
fun OnboardingScreen(onAllowNotifications: () -> Unit = {}, onStart: () -> Unit = {}, ready: Boolean = true, onDone: () -> Unit) {
    val app = LocalContext.current.applicationContext as FrameOutApp
    val pages = remember {
        val base = listOf(OnboardingPage.Hero, OnboardingPage.Formats, OnboardingPage.Tools, OnboardingPage.Pick, OnboardingPage.Ready)
        if (app.container.entitlements.welcome.decided) base else listOf(OnboardingPage.Welcome) + base
    }
    PreferredFrameTheme { OnbThemed { OnboardingContent(pages, onAllowNotifications, onStart, ready, onDone) } }
}

@Composable
private fun OnboardingContent(pages: List<OnboardingPage>, onAllowNotifications: () -> Unit, onStart: () -> Unit, ready: Boolean, onDone: () -> Unit) {
    val colors = FrameTheme.colors
    val pager = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val reduceMotion = LocalSystemA11y.current.reduceMotion
    val current = pager.currentPage
    val last = current >= pages.lastIndex

    fun goTo(index: Int) {
        scope.launch { if (reduceMotion) pager.scrollToPage(index) else pager.animateScrollToPage(index) }
    }

    BackHandler(enabled = current > 0) { goTo(current - 1) }

    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(ready) { if (ready) entered = true }
    val enter by animateFloatAsState(if (entered) 1f else 0f, frameSpring(), label = "enter")

    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }
    val leave by animateFloatAsState(
        if (pending != null) 1f else 0f,
        if (reduceMotion) snap() else tween(300, easing = FlowEase.InOut),
        label = "leave"
    )
    LaunchedEffect(pending) {
        val action = pending ?: return@LaunchedEffect
        if (!reduceMotion) delay(280)
        action()
    }
    val context = LocalContext.current
    fun finish(action: () -> Unit) {
        if (pending != null) return
        commitWelcomeColor(context)
        pending = action
    }

    Box(Modifier.fillMaxSize().background(colors.background).frameAmbientGlow()) {
        OnboardingColumn(pages, pager, enter, leave, ready, onAllowNotifications, { finish(onStart) }, { finish(onDone) }, ::goTo)
    }
}

@Composable
private fun OnboardingColumn(
    pages: List<OnboardingPage>,
    pager: PagerState,
    enter: Float,
    leave: Float,
    ready: Boolean,
    onAllowNotifications: () -> Unit,
    onStart: () -> Unit,
    onDone: () -> Unit,
    goTo: (Int) -> Unit
) {
    val current = pager.currentPage
    val last = current >= pages.lastIndex
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxHeight < 640.dp
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .graphicsLayer {
                    alpha = enter * (1f - leave)
                    translationY = (1f - enter) * 24.dp.toPx()
                    val s = 1f + 0.06f * leave
                    scaleX = s
                    scaleY = s
                }
        ) {
            val brandAlpha by animateFloatAsState(if (pages[current] == OnboardingPage.Welcome) 0f else 1f, frameSpring(), label = "brand")
            OnboardingTopBar(page = current, total = pages.size, showSkip = !last, onSkip = onDone, brandAlpha = brandAlpha, last = last)
            if (!compact) {
                OnboardingWordmark(Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp, bottom = 4.dp).graphicsLayer { alpha = brandAlpha })
            }
            OnboardingPager(pager, pages, ready, onAllowNotifications, Modifier.weight(1f).fillMaxWidth())
            val label = when {
                last -> FrameStrings.onboardingReadyStart()
                else -> FrameStrings.onboardingNext()
            }
            Column(Modifier.navigationBarsPadding().padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 8.dp)) {
                OnboardingCta(label = label, last = last, onClick = { if (last) onStart() else goTo(current + 1) }, modifier = Modifier.fillMaxWidth())
                androidx.compose.animation.AnimatedVisibility(
                    visible = last,
                    enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(),
                    exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically()
                ) {
                    OnboardingLaterButton(onClick = onDone, modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
                }
                Box(Modifier.fillMaxWidth().heightIn(min = 44.dp), contentAlignment = Alignment.Center) {
                    androidx.compose.animation.AnimatedVisibility(visible = last, enter = androidx.compose.animation.fadeIn(), exit = androidx.compose.animation.fadeOut()) {
                        OnboardingLegalNote()
                    }
                }
            }
        }
    }
}

@Composable
private fun PageBody(page: OnboardingPage, offset: () -> Float, onAllowNotifications: () -> Unit) {
    val dir = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f
    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (page == OnboardingPage.Welcome) {
            WelcomePage(maxWidth, maxHeight, offset, dir, onAllowNotifications)
            return@BoxWithConstraints
        }
        val (factor, extra) = pageHeight(page)
        val wide = page != OnboardingPage.Hero
        val cap = if (wide) 340.dp else 320.dp
        if (maxWidth >= 560.dp && maxWidth > maxHeight) {
            val side = ((maxHeight - extra - 16.dp) / factor).coerceAtMost(cap).coerceAtLeast(120.dp)
            PageBodyWide(page, side, offset, dir, onAllowNotifications)
        } else {
            val floor = if (wide) 220.dp else 150.dp
            val side = (maxWidth - 48.dp).coerceAtMost(cap).coerceAtMost((maxHeight - pageReserve(page) - extra) / factor).coerceAtLeast(floor)
            PageBodyNarrow(page, side, maxHeight, offset, dir, onAllowNotifications)
        }
    }
}

@Composable
private fun OnboardingPager(pager: PagerState, pages: List<OnboardingPage>, ready: Boolean, onAllowNotifications: () -> Unit, modifier: Modifier) {
    HorizontalPager(state = pager, modifier = modifier, key = { pages[it].ordinal }) { index ->
        val offset = { (pager.currentPage - index) + pager.currentPageOffsetFraction }
        CompositionLocalProvider(LocalPageActive provides (ready && pager.currentPage == index)) {
            Box(Modifier.fillMaxSize()) { PageBody(pages[index], offset, onAllowNotifications) }
        }
    }
}

internal val StageBleed = 44.dp

internal fun Modifier.stageParallax(offset: () -> Float, dir: Float): Modifier = this
    .layout { measurable, constraints ->
        val pad = StageBleed.roundToPx()
        val placeable = measurable.measure(constraints.offsetBy(pad * 2, pad * 2))
        layout((placeable.width - pad * 2).coerceAtLeast(0), (placeable.height - pad * 2).coerceAtLeast(0)) {
            placeable.place(-pad, -pad)
        }
    }
    .graphicsLayer {
        val o = offset().coerceIn(-1f, 1f)
        val a = o.absoluteValue
        translationX = o * size.width * 0.22f * dir
        val s = 1f - 0.10f * a
        scaleX = s
        scaleY = s
        alpha = (1f - 1.2f * a).coerceIn(0f, 1f)
        compositingStrategy = if (a > 0f) CompositingStrategy.Offscreen else CompositingStrategy.Auto
    }
    .padding(StageBleed)

internal fun Modifier.copyParallax(offset: () -> Float, dir: Float): Modifier = graphicsLayer {
    val o = offset().coerceIn(-1f, 1f)
    val a = o.absoluteValue
    translationX = -o * size.width * 0.30f * dir
    alpha = (1f - 1.4f * a).coerceIn(0f, 1f)
}
