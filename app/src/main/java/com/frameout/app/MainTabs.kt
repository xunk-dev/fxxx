package com.frameout.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.HomeStrings
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.dock.DockItem
import com.frameout.designsystem.dock.FrameDock
import com.frameout.designsystem.dock.LocalDockActive
import com.frameout.designsystem.icons.FrameIcons

internal class TabNav(val onSelect: (AppRoute) -> Unit, val onOpenProject: (Long) -> Unit = {})

internal val LocalTabNav = staticCompositionLocalOf { TabNav({ }, { }) }

internal val LocalTabActive = staticCompositionLocalOf { true }

private val TabRoutes: List<AppRoute> = listOf(AppRoute.Home, AppRoute.Library, AppRoute.Settings)

private const val TAB_SLIDE_MS = 300
private const val TAB_OUT_MS = 90
private const val TAB_IN_MS = 210
private const val TAB_REDUCED_FADE_MS = 90
private val TabSlide = 30.dp

@Composable
internal fun MainTabs(
    selected: AppRoute,
    onSelect: (AppRoute) -> Unit,
    onOpenProject: (Long) -> Unit,
    atRoot: Boolean,
    home: @Composable () -> Unit,
    library: @Composable () -> Unit,
    settings: @Composable () -> Unit
) {
    val index = TabRoutes.indexOf(selected).coerceAtLeast(0)
    val reduceMotion = LocalSystemA11y.current.reduceMotion
    var warm by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        warm = true
    }
    val tracker = remember { intArrayOf(index, index) }
    if (tracker[1] != index) {
        tracker[0] = tracker[1]
        tracker[1] = index
    }
    val previous = tracker[0]
    val latest by rememberUpdatedState(onSelect)
    val latestOpen by rememberUpdatedState(onOpenProject)
    val nav = remember { TabNav({ latest(it) }, { latestOpen(it) }) }
    BackHandler(enabled = atRoot && index != 0) { nav.onSelect(AppRoute.Home) }
    CompositionLocalProvider(LocalTabNav provides nav) {
        Box(Modifier.fillMaxSize()) {
            TabPage(0, index, previous, reduceMotion, warm, home)
            TabPage(1, index, previous, reduceMotion, warm, library)
            TabPage(2, index, previous, reduceMotion, warm, settings)
        }
    }
}

private fun inactiveTabModifier(active: Boolean): Modifier = if (active) Modifier else Modifier
    .pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
            }
        }
    }
    .clearAndSetSemantics { }

@Composable
private fun TabPage(page: Int, current: Int, previous: Int, reduceMotion: Boolean, warm: Boolean, content: @Composable () -> Unit) {
    val active = page == current
    if (!active && !warm) return
    val alpha by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = when {
            reduceMotion -> tween(TAB_REDUCED_FADE_MS, easing = LinearOutSlowInEasing)
            active -> tween(TAB_IN_MS, delayMillis = TAB_OUT_MS, easing = LinearOutSlowInEasing)
            else -> tween(TAB_OUT_MS, easing = FastOutLinearInEasing)
        },
        label = "tabAlpha"
    )
    val slide by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(TAB_SLIDE_MS, easing = FastOutSlowInEasing),
        label = "tabSlide"
    )
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val towards = if (active) previous < page else page > current
    val side = (if (towards) 1f else -1f) * (if (rtl) -1f else 1f)
    val inactive = inactiveTabModifier(active)
    Box(
        Modifier
            .fillMaxSize()
            .zIndex(if (active) 1f else 0f)
            .graphicsLayer {
                this.alpha = alpha
                translationX = if (reduceMotion) 0f else side * TabSlide.toPx() * (1f - slide)
            }
            .then(inactive)
    ) {
        CompositionLocalProvider(LocalTabActive provides active, LocalDockActive provides active) { content() }
    }
}

@Composable
internal fun MainTabBar(selfIndex: Int, sideFab: (@Composable () -> Unit)? = null) {
    val nav = LocalTabNav.current
    val items = listOf(
        DockItem(
            "home", HomeStrings.home(), FrameIcons.Home, selectedIcon = FrameIcons.HomeFilled,
            selected = selfIndex == 0, onClick = { if (selfIndex != 0) nav.onSelect(AppRoute.Home) }
        ),
        DockItem(
            "library", FrameStrings.library(), FrameIcons.Library, selectedIcon = FrameIcons.LibraryFilled,
            selected = selfIndex == 1, onClick = { if (selfIndex != 1) nav.onSelect(AppRoute.Library) }
        ),
        DockItem(
            "settings", FrameStrings.settings(), FrameIcons.Settings, selectedIcon = FrameIcons.SettingsFilled,
            selected = selfIndex == 2, onClick = { if (selfIndex != 2) nav.onSelect(AppRoute.Settings) }
        )
    )
    FrameDock(items = items, modifier = Modifier.fillMaxSize(), sideFab = sideFab)
}
