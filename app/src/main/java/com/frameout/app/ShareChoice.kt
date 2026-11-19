package com.frameout.app

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.frameout.app.frames.FramesSheet
import com.frameout.app.home.ExportHost
import com.frameout.app.home.ExportStyle
import com.frameout.core.db.FrameEntity
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.HomeStrings
import com.frameout.core.util.SavedStrings
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.haptics.FrameHaptic
import com.frameout.designsystem.haptics.rememberFrameHaptics
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameCardEdge
import androidx.compose.ui.res.stringResource

internal class ShareExport(
    val uris: List<Uri>,
    val video: Boolean,
    val style: ExportStyle?,
    val after: AppRoute?
)

internal class ShareFlow {
    var pending by mutableStateOf<IncomingMedia.Stabilized?>(null)
    var export by mutableStateOf<ShareExport?>(null)
}

internal enum class ShareChoice(val icon: Int) {
    Edit(FrameIconsExtra.Edit),
    Collage(FrameIcons.Collage),
    Contact(FrameIconsExtra.ContactSheet),
    ExportImages(FrameIconsExtra.Export),
    Batch(FrameIconsExtra.Frames),
    ExportVideo(FrameIconsExtra.Clip);

    fun title(): String = when (this) {
        Edit -> HomeStrings.editTitle()
        Collage -> HomeStrings.collageTitle()
        Contact -> SavedStrings.exportContact()
        ExportImages -> HomeStrings.exportTitle()
        Batch -> FrameStrings.batchQueue()
        ExportVideo -> HomeStrings.videoTitle()
    }

    fun description(): String = when (this) {
        Edit -> HomeStrings.editDesc()
        Collage -> HomeStrings.collageDesc()
        Contact -> SavedStrings.exportContactHint()
        ExportImages -> HomeStrings.exportMethodImages()
        Batch -> HomeStrings.extractTitle()
        ExportVideo -> HomeStrings.exportMethodVideo()
    }

    companion object {
        val Images = listOf(Edit, Collage, Contact, ExportImages)
        val Videos = listOf(Batch, ExportVideo)
    }
}

internal fun openShared(done: IncomingMedia.Stabilized, navigate: (AppRoute) -> Unit) {
    if (done.ready.isNotEmpty()) navigate(pickerRoute(done.ready))
    stillRoute(done.stills)?.let(navigate)
}

internal fun resolveShare(choice: ShareChoice, done: IncomingMedia.Stabilized, flow: ShareFlow, navigate: (AppRoute) -> Unit) {
    val later = done.ready.takeIf { it.isNotEmpty() }?.let(::pickerRoute)
    when (choice) {
        ShareChoice.Edit -> {
            later?.let(navigate)
            stillRoute(done.stills, edit = true)?.let(navigate)
        }
        ShareChoice.Collage -> {
            later?.let(navigate)
            done.stills.firstOrNull()?.let { navigate(AppRoute.Collage(it.projectId, done.stills.map(FrameEntity::id))) }
        }
        ShareChoice.Contact -> flow.export = ShareExport(done.stillUris, false, ExportStyle.Contact, later)
        ShareChoice.ExportImages -> flow.export = ShareExport(done.stillUris, false, null, later)
        ShareChoice.Batch -> navigate(AppRoute.Batch(done.ready.map(Uri::toString)))
        ShareChoice.ExportVideo -> flow.export = ShareExport(done.ready, true, null, null)
    }
}

@Composable
internal fun BoxScope.ShareOverlays(flow: ShareFlow, navigate: (AppRoute) -> Unit) {
    val done = flow.pending
    PreferredFrameTheme {
        ShareChoiceSheet(done, { flow.pending = null }) { choice ->
            flow.pending = null
            done?.let { resolveShare(choice, it, flow, navigate) }
        }
        val request = flow.export
        ExportHost(request != null, if (request?.video == true) 0 else 1, request) {
            flow.export?.after?.let(navigate)
            flow.export = null
        }
    }
}

@Composable
private fun ShareChoiceSheet(done: IncomingMedia.Stabilized?, onClose: () -> Unit, onPick: (ShareChoice) -> Unit) {
    var last by remember { mutableStateOf(done) }
    if (done != null) last = done
    val shown = last
    val images = shown != null && shown.stills.size >= 2
    val title = stringResource(if (images) R.string.share_photos_title else R.string.share_videos_title)
    FramesSheet(done != null, title, onClose) {
        Column(Modifier.fillMaxWidth(), Arrangement.spacedBy(10.dp)) {
            (if (images) ShareChoice.Images else ShareChoice.Videos).forEach { ShareChoiceRow(it) { onPick(it) } }
        }
    }
}

@Composable
private fun ShareChoiceRow(choice: ShareChoice, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    val haptics = rememberFrameHaptics()
    Row(
        Modifier
            .fillMaxWidth()
            .clip(FrameShapes.xl)
            .background(colors.containerHigh)
            .border(1.dp, frameCardEdge(colors.primary, 0.30f), FrameShapes.xl)
            .clickable(role = Role.Button) {
                haptics.perform(FrameHaptic.Tap)
                onClick()
            }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Image(painterResource(choice.icon), contentDescription = null, modifier = Modifier.size(24.dp), colorFilter = ColorFilter.tint(colors.primary))
        Column(Modifier.weight(1f), Arrangement.spacedBy(2.dp)) {
            FrameText(choice.title(), style = FrameTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            FrameText(choice.description(), style = FrameTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
