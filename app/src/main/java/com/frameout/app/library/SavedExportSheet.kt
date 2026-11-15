package com.frameout.app.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.frameout.app.FrameOutApp
import com.frameout.app.frames.ContactSheetSheet
import com.frameout.app.frames.FramesSheet
import com.frameout.app.settings.NavRow
import com.frameout.app.settings.SettingsCard
import com.frameout.app.settings.SettingsDivider
import com.frameout.app.settings.SettingsSection
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.SavedStrings
import com.frameout.designsystem.icons.FrameIconsExtra

internal class SavedExportChoices(val onZip: () -> Unit, val onPdf: () -> Unit, val onContact: () -> Unit, val onFolder: () -> Unit)

@Composable
internal fun SavedExportSheet(visible: Boolean, count: Int, choices: SavedExportChoices, onClose: () -> Unit) {
    FramesSheet(visible, SavedStrings.exportTitle() + FrameStrings.sep() + FrameStrings.selectedCount(count), onClose) {
        SettingsSection(FrameStrings.groupExport(), caption = com.frameout.core.util.QualityStrings.exportProCaption(), badge = true) {
            SettingsCard {
                NavRow(FrameIconsExtra.Storage, FrameStrings.exportZip(), hint = SavedStrings.exportZipHint(), onClick = { onClose(); choices.onZip() })
                SettingsDivider()
                NavRow(FrameIconsExtra.SavedFile, FrameStrings.exportPdf(), hint = SavedStrings.exportPdfHint(), onClick = { onClose(); choices.onPdf() })
                SettingsDivider()
                NavRow(FrameIconsExtra.ContactSheet, SavedStrings.exportContact(), hint = SavedStrings.exportContactHint(), onClick = { onClose(); choices.onContact() })
                SettingsDivider()
                NavRow(FrameIconsExtra.Folder, SavedStrings.exportFolder(), hint = SavedStrings.exportFolderHint(), onClick = { onClose(); choices.onFolder() })
            }
        }
    }
}

@Composable
internal fun SavedExportHost(visible: Boolean, count: Int, vm: LibraryViewModel, pickFolder: () -> Unit, onClose: () -> Unit) {
    var contactOpen by remember { mutableStateOf(false) }
    val prefs = (LocalContext.current.applicationContext as FrameOutApp).container.prefs
    val gated: (() -> Unit) -> Unit = { run -> com.frameout.app.billing.ExportGate.gated(run) }
    val choices = SavedExportChoices(
        onZip = { gated { vm.exportSelectedSaved(SavedExportKind.Zip, null) } },
        onPdf = { gated { vm.exportSelectedSaved(SavedExportKind.Pdf, null) } },
        onContact = { gated { contactOpen = true } },
        onFolder = { gated(pickFolder) }
    )
    SavedExportSheet(visible, count, choices, onClose)
    ContactSheetSheet(contactOpen && count > 0, count, prefs, { contactOpen = false }, forSaved = true) { options ->
        vm.exportSelectedSaved(SavedExportKind.Contact, null, options)
    }
}
