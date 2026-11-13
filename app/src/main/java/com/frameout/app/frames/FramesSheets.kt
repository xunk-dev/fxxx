package com.frameout.app.frames

import com.frameout.designsystem.glass.frameSheetSurface
import com.frameout.designsystem.glass.frameGlow
import androidx.compose.ui.graphics.Brush
import com.frameout.designsystem.theme.frameCardEdge
import com.frameout.designsystem.theme.frameCardBrush
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.frameout.app.settings.NavRow
import com.frameout.app.settings.SettingsCard
import com.frameout.app.settings.SettingsDivider
import com.frameout.app.settings.SettingsSection
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.DatasetStrings
import com.frameout.core.util.QualityStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.a11y.screenHeading
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.motion.frameEnter
import com.frameout.designsystem.motion.frameExit
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameHairline

private val SheetTopShape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

@Composable
internal fun FramesSheet(visible: Boolean, title: String, onClose: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val colors = FrameTheme.colors
    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.82f).dp
    BackHandler(enabled = visible, onBack = onClose)
    AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut()) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (FrameTheme.colors.isDark) 0.5f else 0.34f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose)
        )
    }
    AnimatedVisibility(visible, enter = frameEnter(), exit = frameExit(), modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize(), Alignment.BottomCenter) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .frameSheetSurface(SheetTopShape)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                    .navigationBarsPadding()
                    .padding(top = 8.dp)
            ) {
                Box(Modifier.align(Alignment.CenterHorizontally).size(width = 36.dp, height = 4.dp).background(colors.primary.copy(alpha = 0.45f), FrameShapes.pill))
                Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 20.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    FrameText(title, style = FrameTheme.typography.title, modifier = Modifier.weight(1f).screenHeading())
                    HeaderButton(FrameIcons.Close, QualityStrings.contentClose(), onClose)
                }
                Column(Modifier.fillMaxWidth().heightIn(max = maxHeight).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 20.dp), content = content)
            }
        }
    }
}

@Composable
fun FramesToolsSheet(
    state: FramesState,
    onClose: () -> Unit,
    onSlides: () -> Unit,
    onVideo: () -> Unit,
    onGif: () -> Unit,
    onPdf: () -> Unit,
    onZip: () -> Unit,
    onCsv: () -> Unit,
    onEvidence: () -> Unit,
    onContactSheet: (() -> Unit)? = null,
    onDataset: (() -> Unit)? = null
) {
    fun then(action: () -> Unit): () -> Unit = { onClose(); action() }
    FramesSheet(state.toolsOpen, QualityStrings.toolsExport(), onClose) {
        SettingsSection(QualityStrings.toolsExport(), caption = QualityStrings.exportProCaption(), badge = true) {
            SettingsCard {
                NavRow(FrameIconsExtra.Live, FrameStrings.exportVideo(), onClick = then(onVideo))
                SettingsDivider()
                NavRow(FrameIconsExtra.Gif, FrameStrings.exportGif(), onClick = then(onGif))
                SettingsDivider()
                NavRow(FrameIconsExtra.ImageFormat, FrameStrings.exportPdf(), onClick = then(onPdf))
                SettingsDivider()
                onContactSheet?.let {
                    NavRow(FrameIconsExtra.ContactSheet, FrameStrings.exportContact(), hint = FrameStrings.contactHint(), onClick = then(it))
                    SettingsDivider()
                }
                NavRow(FrameIconsExtra.Storage, FrameStrings.exportZip(), onClick = then(onZip))
                SettingsDivider()
                NavRow(FrameIconsExtra.Storage, FrameStrings.exportCsv(), onClick = then(onCsv))
                SettingsDivider()
                NavRow(FrameIconsExtra.Warning, FrameStrings.exportEvidence(), onClick = then(onEvidence))
                SettingsDivider()
                NavRow(FrameIconsExtra.ImageFormat, QualityStrings.slidesPdf(), hint = QualityStrings.toolsSlidesHint(), onClick = then(onSlides))
                onDataset?.let {
                    SettingsDivider()
                    NavRow(FrameIconsExtra.Detect, DatasetStrings.title(), hint = DatasetStrings.rowHint(), onClick = then(it))
                }
            }
            FrameText(QualityStrings.toolsExportScope(), style = FrameTheme.typography.bodySmall, color = FrameTheme.colors.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp))
        }
    }
}

@Composable
fun FramesFilterSheet(
    state: FramesState,
    onClose: () -> Unit,
    onAll: () -> Unit,
    onSmart: (SmartFilter) -> Unit,
    onSharpest: () -> Unit
) {
    fun then(action: () -> Unit): () -> Unit = { onClose(); action() }
    val sharpest = state.filter == FrameFilter.Sharpest
    val none = state.smartFilter == SmartFilter.None && !sharpest
    FramesSheet(state.filterOpen, QualityStrings.filterTitle(), onClose) {
        SettingsSection(QualityStrings.filterTitle()) {
            SettingsCard {
                NavRow(FrameIcons.GridSize, QualityStrings.filterAll(), selected = none, onClick = then(onAll))
                SettingsDivider()
                com.frameout.app.billing.ProGroup(locked = com.frameout.app.billing.rememberProLocked()) {
                    NavRow(FrameIcons.StarFilled, QualityStrings.important(), hint = QualityStrings.toolsImportantHint(), selected = state.smartFilter == SmartFilter.Important, pro = true, onClick = { onSmart(SmartFilter.Important) })
                    SettingsDivider()
                    NavRow(FrameIconsExtra.Blur, QualityStrings.sharpest(), hint = QualityStrings.toolsSharpestHint(), selected = sharpest, pro = true, onClick = then(onSharpest))
                    SettingsDivider()
                    NavRow(FrameIconsExtra.Duplicate, QualityStrings.duplicates(), hint = QualityStrings.toolsDuplicatesHint(), selected = state.smartFilter == SmartFilter.Duplicates, pro = true, onClick = { onSmart(SmartFilter.Duplicates) })
                    SettingsDivider()
                    NavRow(FrameIconsExtra.Person, QualityStrings.noPeople(), hint = QualityStrings.toolsNoPeopleHint(), selected = state.smartFilter == SmartFilter.NoPeople, pro = true, onClick = { onSmart(SmartFilter.NoPeople) })
                }
            }
        }
        FrameText(QualityStrings.filterCaption(), style = FrameTheme.typography.bodySmall, color = FrameTheme.colors.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp))
    }
}

@Composable
fun FramesSmartChip(state: FramesState, onClear: () -> Unit) {
    val colors = FrameTheme.colors
    val (icon, label) = when (state.smartFilter) {
        SmartFilter.Important -> FrameIcons.StarFilled to QualityStrings.important()
        SmartFilter.Duplicates -> FrameIconsExtra.Duplicate to QualityStrings.duplicates()
        SmartFilter.NoPeople -> FrameIconsExtra.Person to QualityStrings.noPeople()
        SmartFilter.None -> return
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier.clip(FrameShapes.pill).background(colors.primary.copy(alpha = 0.14f)).border(1.dp, colors.primary.copy(alpha = 0.4f), FrameShapes.pill)
                .frameClickable(onClick = onClear).padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)
        ) {
            androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(icon), null, Modifier.size(15.dp), colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(colors.primary))
            FrameText(label, style = FrameTheme.typography.label, color = colors.primary, maxLines = 1)
            androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(FrameIcons.Close), QualityStrings.contentClose(), Modifier.size(18.dp).padding(2.dp), colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(colors.primary))
        }
        FrameText(FrameStrings.frameCount(state.frameCount.coerceAtLeast(1)), style = FrameTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 1)
    }
}
