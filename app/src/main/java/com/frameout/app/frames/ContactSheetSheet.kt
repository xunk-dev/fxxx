package com.frameout.app.frames

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.frameout.app.settings.SettingsActionButton
import com.frameout.app.settings.SettingsCard
import com.frameout.core.export.ContactSheetLayout
import com.frameout.core.export.ContactSheetPreview
import com.frameout.core.prefs.AppPrefs
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.SavedStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme

private val DefaultAspect = 16f / 9f

internal class ContactPreview(
    val aspect: Float = DefaultAspect,
    val thumbs: List<ImageBitmap?> = emptyList(),
    val onAdd: (() -> Unit)? = null
)

private fun AppPrefs.contactOptions() = ContactSheetLayout.Options(contactColumns(), contactShowTime(), contactShowNumber(), contactPng())

@Composable
internal fun ContactSheetSheet(
    visible: Boolean,
    frameCount: Int,
    prefs: AppPrefs,
    onClose: () -> Unit,
    forSaved: Boolean = false,
    preview: ContactPreview = ContactPreview(),
    onCreate: (ContactSheetLayout.Options) -> Unit
) {
    var options by remember { mutableStateOf(prefs.contactOptions()) }
    LaunchedEffect(visible) { if (visible) options = prefs.contactOptions() }
    val captioned = options.showNumber || (options.showTime && !forSaved)
    val summary = ContactSheetPreview.summarize(frameCount, preview.aspect, options.copy(showTime = options.showTime && !forSaved))
    FramesSheet(visible, if (forSaved) SavedStrings.exportContact() else FrameStrings.contactTitle(), onClose) {
        ContactHint(forSaved, frameCount)
        PreviewCard(PaperSpec(summary, captioned, preview.thumbs, preview.onAdd != null), preview.onAdd)
        Box(Modifier.padding(top = 10.dp)) {
            ColumnsCard(options.safeColumns) {
                prefs.setContactColumns(it)
                options = options.copy(columns = it)
            }
        }
        Box(Modifier.padding(top = 10.dp)) { StatsCard(summary) }
        ChipRow(chipsFor(options, forSaved, prefs) { options = it })
        Box(Modifier.padding(top = 12.dp)) {
            SettingsActionButton(FrameStrings.contactCreate(), enabled = frameCount > 0 && !summary.tooMany) {
                onClose()
                onCreate(options)
            }
        }
    }
}

private fun chipsFor(options: ContactSheetLayout.Options, forSaved: Boolean, prefs: AppPrefs, update: (ContactSheetLayout.Options) -> Unit): List<ChipSpec> = buildList {
    if (!forSaved) {
        add(ChipSpec(FrameIconsExtra.Timer, FrameStrings.contactChipTime(), options.showTime) {
            prefs.setContactShowTime(it)
            update(options.copy(showTime = it))
        })
    }
    add(ChipSpec(FrameIconsExtra.Duplicate, FrameStrings.contactChipNumber(), options.showNumber) {
        prefs.setContactShowNumber(it)
        update(options.copy(showNumber = it))
    })
    add(ChipSpec(FrameIconsExtra.ImageFormat, FrameStrings.contactChipQuality(), options.png) {
        prefs.setContactPng(it)
        update(options.copy(png = it))
    })
}

@Composable
private fun PreviewCard(spec: PaperSpec, onAdd: (() -> Unit)?) {
    SettingsCard(Modifier.padding(top = 4.dp)) {
        Column(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            ContactPaper(spec, FrameStrings.contactAddImages(), onAdd)
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                PaperFacts(spec.summary, Modifier.weight(1f), onAdd != null)
                if (onAdd != null) AddImagesButton(onAdd)
            }
        }
    }
}

@Composable
private fun PaperFacts(summary: ContactSheetPreview.Summary, modifier: Modifier, canAdd: Boolean) {
    val colors = FrameTheme.colors
    if (summary.empty && canAdd) {
        FrameText(FrameStrings.contactAddHint(), style = FrameTheme.typography.bodySmall, color = colors.onSurfaceVariant, modifier = modifier.padding(end = 8.dp))
        return
    }
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        FactChip(FrameStrings.contactSheetsN(summary.pages))
        FactChip(FrameStrings.contactRowsN(summary.totalRows))
    }
}

@Composable
private fun FactChip(text: String) {
    val colors = FrameTheme.colors
    FrameText(
        text,
        style = FrameTheme.typography.bodySmall,
        color = colors.onSurface,
        modifier = Modifier.clip(FrameShapes.pill).background(colors.containerHigh).padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
private fun AddImagesButton(onClick: () -> Unit) {
    val colors = FrameTheme.colors
    Row(
        Modifier.height(36.dp).clip(FrameShapes.pill).background(colors.primaryContainer).frameClickable(onClick = onClick).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Image(painterResource(FrameIconsExtra.Plus), null, Modifier.size(16.dp), colorFilter = ColorFilter.tint(colors.onSurface))
        FrameText(FrameStrings.contactAddImages(), style = FrameTheme.typography.label, color = colors.onSurface)
    }
}

@Composable
private fun ContactHint(forSaved: Boolean, count: Int) {
    FrameText(
        contactHintText(forSaved, count),
        style = FrameTheme.typography.bodySmall,
        color = FrameTheme.colors.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
    )
}

private fun contactHintText(forSaved: Boolean, count: Int): String = when {
    forSaved -> SavedStrings.exportContactSheetHint() + if (count > 0) "  ·  " + FrameStrings.selectedCount(count) else ""
    else -> FrameStrings.contactHint() + if (count > 0) "  ·  " + FrameStrings.frameCount(count) else ""
}
