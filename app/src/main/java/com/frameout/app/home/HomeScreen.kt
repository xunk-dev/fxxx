package com.frameout.app.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.frameout.app.FrameOutApp
import com.frameout.app.ads.bannerSlotHeightDp
import com.frameout.app.LocalTabActive
import com.frameout.app.LocalTabNav
import com.frameout.app.MainTabBar
import com.frameout.app.PreferredFrameTheme
import com.frameout.app.billing.ProStatusBadge
import com.frameout.app.library.LibraryBrand
import com.frameout.app.library.rememberIsPro
import com.frameout.app.library.rememberVideoSuggestion
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameRailPadding

class HomeActions(
    val onQuickStart: (Uri, com.frameout.engine.timeline.TrimRange, () -> Unit) -> Unit,
    val onPro: () -> Unit,
    val onEditPhoto: (Uri) -> Unit,
    val onCollage: (List<Uri>) -> Unit,
    val onCompare: (List<Uri>) -> Unit,
    val onOpenVideo: (Uri) -> Unit
)

private const val COLLAGE_MAX = 9
private val DockReserve = 88.dp

private class HomePickers(
    val video: () -> Unit,
    val photo: () -> Unit,
    val collage: () -> Unit,
    val compare: () -> Unit
)

@Composable
private fun rememberHomePickers(actions: HomeActions, onVideo: (String) -> Unit): HomePickers {
    val context = LocalContext.current
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            onVideo(uri.toString())
        }
    }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let(actions.onEditPhoto) }
    val collagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(COLLAGE_MAX)) { uris ->
        if (uris.isNotEmpty()) actions.onCollage(uris)
    }
    val comparePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(2)) { uris ->
        if (uris.isNotEmpty()) actions.onCompare(uris)
    }
    val imageOnly = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
    return HomePickers(
        video = { videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) },
        photo = { photoPicker.launch(imageOnly) },
        collage = { collagePicker.launch(imageOnly) },
        compare = { comparePicker.launch(imageOnly) }
    )
}

@Composable
fun HomeScreen(actions: HomeActions) {
    var quickUri by rememberSaveable { mutableStateOf<String?>(null) }
    var exportOpen by rememberSaveable { mutableStateOf(false) }
    var exportTab by rememberSaveable { mutableIntStateOf(0) }
    val pickers = rememberHomePickers(actions) { quickUri = it }
    PreferredFrameTheme {
        Box(Modifier.fillMaxSize().background(FrameTheme.colors.background).homeAmbientGlow()) {
            Column(Modifier.fillMaxSize().frameRailPadding()) {
                HomeHeader()
                HomeCards(actions, pickers, Modifier.weight(1f).align(Alignment.CenterHorizontally)) {
                    exportTab = it
                    exportOpen = true
                }
            }
            val strip by rememberExtractStrip()
            val nav = LocalTabNav.current
            MainTabBar(
                0,
                sideFab = { HomeQuickFab(strip, onStart = pickers.video, onOpen = nav.onOpenProject) }
            )
            quickUri?.let { raw ->
                key(raw) {
                    QuickSheet(
                        uri = Uri.parse(raw),
                        onClose = { quickUri = null },
                        snapshot = true,
                        compact = true,
                        onStart = { uri, range, _, _ ->
                            actions.onQuickStart(uri, range) { quickUri = null }
                        }
                    )
                }
            }
            ExportHost(exportOpen, exportTab) { exportOpen = false }
        }
    }
}

@Composable
private fun HomeToolsEntry(
    pickers: HomePickers,
    onExport: (Int) -> Unit,
    tallHeight: Dp,
    shortHeight: Dp,
    bottomInset: Dp,
    latest: (@Composable (Modifier) -> Unit)?
) {
    HomeToolsSheet(
        onImages = { onExport(1) },
        onVideo = { onExport(0) },
        onCompare = pickers.compare,
        onCollage = pickers.collage,
        onEdit = pickers.photo,
        tallHeight = tallHeight,
        shortHeight = shortHeight,
        bottomInset = bottomInset,
        latest = latest
    )
}


private val TileColumns = 2

private class HomeSizing(val proHeight: Dp, val tallHeight: Dp, val shortHeight: Dp)

@Composable
private fun rememberHomeSizing(width: Dp, grow: Float, available: Float, adTotal: Float): HomeSizing {
    return remember(width, grow, available, adTotal) {
        val content = minOf(width.value, 640f) - 2f * HomeMetrics.ScreenPad.value
        val tileWidth = (content - HomeMetrics.TileGap.value * (TileColumns - 1)) / TileColumns
        val pro = (content * 0.46f).coerceIn(156f, 184f) + HomeMetrics.ProHeightGrow.value * grow
        val tall = (tileWidth * 0.98f).coerceIn(148f, 192f) + 14f * grow
        val short = (tall * 0.56f).coerceIn(66f, 96f) + 10f * grow
        val fit = fitHomeHeights(pro, tall, short, available, adTotal)
        HomeSizing(fit.pro.dp, fit.tall.dp, fit.short.dp)
    }
}

@Composable
private fun HomeCards(actions: HomeActions, pickers: HomePickers, modifier: Modifier, onExport: (Int) -> Unit) {
    val active = LocalTabActive.current
    val suggestion = rememberVideoSuggestion()
    val navBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val grow = (LocalDensity.current.fontScale - 1f).coerceIn(0f, 1f)
    val bottom = navBar + DockReserve
    val hub = (LocalContext.current.applicationContext as FrameOutApp).ads
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val adTotal = if (hub.active()) bannerSlotHeightDp(hub, (minOf(maxWidth.value, 640f) - 2f * HomeMetrics.ScreenPad.value).toInt()).toFloat() else 0f
        val sizing = rememberHomeSizing(maxWidth, grow, maxHeight.value - bottom.value, adTotal)
        val proHeight = sizing.proHeight
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(HomeMetrics.SectionGap)
        ) {
            val inset = Modifier.align(Alignment.CenterHorizontally).widthIn(max = 640.dp).fillMaxWidth().padding(horizontal = HomeMetrics.ScreenPad)
            val ui = suggestion.ui
            val latest: (@Composable (Modifier) -> Unit)? = ui?.let { u ->
                @Composable { m: Modifier ->
                    LatestVideoTile(
                        u.cover,
                        onOpen = {
                            suggestion.handled()
                            actions.onOpenVideo(u.video.uri)
                        },
                        onDismiss = suggestion.handled,
                        modifier = m
                    )
                }
            }
            HomeEntrance(0, active, inset) { HomeProRow(actions.onPro, proHeight) }
            HomeEntrance(1, active, Modifier.fillMaxWidth()) {
                HomeToolsEntry(pickers, onExport, sizing.tallHeight, sizing.shortHeight, bottom, latest)
            }
        }
    }
}

@Composable
private fun HomeHeader() {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(start = HomeMetrics.ScreenPad, end = HomeMetrics.ScreenPad, top = 8.dp, bottom = 8.dp).height(44.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LibraryBrand(pro = rememberIsPro())
        Spacer(Modifier.weight(1f))
        ProStatusBadge()
    }
}
