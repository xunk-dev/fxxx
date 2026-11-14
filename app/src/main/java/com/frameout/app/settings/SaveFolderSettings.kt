package com.frameout.app.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameHairline
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.frameout.core.prefs.AppPrefs
import com.frameout.core.util.FrameStrings

private const val DEFAULT_PATH = com.frameout.core.storage.AppFolders.PICTURES

internal fun hasFolderAccess(context: Context, uri: String): Boolean {
    if (uri.isBlank()) return false
    val tree = Uri.parse(uri)
    return context.contentResolver.persistedUriPermissions.any { it.uri == tree && it.isWritePermission }
}

internal fun folderLabel(context: Context, uri: String): String? = runCatching {
    val tree = Uri.parse(uri)
    val document = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
    context.contentResolver.query(document, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    }
}.getOrNull()

internal fun folderValue(context: Context, uri: String): String =
    if (hasFolderAccess(context, uri)) folderLabel(context, uri) ?: FrameStrings.customFolder() else FrameStrings.defaultFolder()

@Composable
internal fun rememberFolderPicker(prefs: AppPrefs, onPicked: (String) -> Unit = { prefs.setSaveFolderUri(it) }): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { tree ->
        if (tree != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            val granted = runCatching { context.contentResolver.takePersistableUriPermission(tree, flags) }.isSuccess
            if (granted) onPicked(tree.toString())
        }
    }
    return remember(launcher) { { launcher.launch(null) } }
}

@Composable
internal fun SaveFolderSheet(
    visible: Boolean,
    prefs: AppPrefs,
    imagesUri: String,
    zipUri: String,
    zipFollowsText: String,
    onPickImages: () -> Unit,
    onPickZip: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.78f).dp
    BottomSheetFrame(visible, onClose, FrameStrings.saveFolder()) {
        Column(Modifier.heightIn(max = maxHeight).verticalScroll(rememberScrollState())) {
            FolderSection(context, FrameStrings.saveFolder(), FrameStrings.saveFolderHint(), DEFAULT_PATH, imagesUri, { prefs.clearSaveFolderUri() }, onPickImages, onClose)
            Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).height(1.dp).background(frameHairline()))
            FolderSection(context, FrameStrings.exportFolder(), FrameStrings.exportFolderHint(), zipFollowsText, zipUri, { prefs.clearExportFolderUri() }, onPickZip, onClose)
        }
    }
}

@Composable
private fun FolderSection(
    context: Context,
    title: String,
    hint: String,
    defaultText: String,
    uri: String,
    onClear: () -> Unit,
    onPick: () -> Unit,
    onClose: () -> Unit
) {
    val colors = FrameTheme.colors
    val custom = hasFolderAccess(context, uri)
    LaunchedEffect(uri) { if (uri.isNotBlank() && !custom) onClear() }
    Column(Modifier.padding(horizontal = 12.dp), Arrangement.spacedBy(2.dp)) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 6.dp), Arrangement.spacedBy(2.dp)) {
            FrameText(title, style = FrameTheme.typography.label, color = colors.onSurface)
            FrameText(hint, style = FrameTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
        ChoiceItem(ChoiceOption("default", FrameStrings.defaultFolder(), defaultText), !custom) { onClear() }
        if (custom) ChoiceItem(ChoiceOption("custom", folderLabel(context, uri) ?: FrameStrings.customFolder(), FrameStrings.customFolder()), true) { }
        Box(
            Modifier
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .clip(FrameShapes.pill)
                .border(1.dp, colors.primary.copy(alpha = 0.5f), FrameShapes.pill)
                .frameClickable {
                    onClose()
                    onPick()
                }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            FrameText(if (custom) FrameStrings.changeFolder() else FrameStrings.chooseFolder(), style = FrameTheme.typography.label, color = colors.primary)
        }
    }
}
