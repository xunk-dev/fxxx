package com.frameout.app.library

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.core.db.ProjectEntity
import com.frameout.core.util.DetailStrings
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.components.DialogButton
import com.frameout.designsystem.components.DialogTone
import com.frameout.designsystem.components.FrameDialog
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameHairline
import java.text.DateFormat
import java.util.Date
import java.util.Locale

private class DetailRow(val icon: Int, val label: String, val value: String, val tint: Color, val ltr: Boolean)

@Composable
internal fun ProjectDetailsDialog(project: ProjectEntity, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val specs = produceState(peekSpecs(project), project.id, project.sourceUri) { value = loadSpecs(context, project) }.value
    val rows = detailRows(project, specs)
    FrameDialog(
        onDismiss = onDismiss,
        tone = DialogTone.Info,
        title = DetailStrings.details(),
        primary = DialogButton(FrameStrings.close(), onDismiss),
        body = project.name,
        icon = FrameIcons.Info
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = 18.dp)
                .clip(FrameShapes.xl)
                .background(FrameTheme.colors.onSurface.copy(alpha = 0.05f))
                .border(1.dp, frameHairline(), FrameShapes.xl)
        ) {
            rows.forEachIndexed { index, row ->
                if (index > 0) Box(Modifier.fillMaxWidth().padding(horizontal = 14.dp).height(1.dp).background(frameHairline()))
                DetailRowView(row)
            }
        }
    }
}

@Composable
private fun detailRows(project: ProjectEntity, specs: VideoSpecs?): List<DetailRow> {
    val colors = FrameTheme.colors
    val dim = colors.primary
    return listOfNotNull(
        DetailRow(FrameIconsExtra.Calendar, DetailStrings.date(), formatStamp(project.createdAt), dim, false),
        project.durationMs.takeIf { it > 0 }?.let { DetailRow(FrameIconsExtra.Timer, DetailStrings.duration(), formatDuration(it), dim, true) },
        DetailRow(FrameIconsExtra.Keyframe, FrameStrings.frames(), FrameStrings.frameCount(project.extractedCount), dim, false),
        specs?.let(::specsHeadline)?.let { DetailRow(FrameIconsExtra.SavedVideo, DetailStrings.quality(), it, dim, true) },
        specs?.codec?.let { DetailRow(FrameIconsExtra.Package, DetailStrings.codec(), it, dim, true) },
        specs?.fps?.let { DetailRow(FrameIconsExtra.Speed, DetailStrings.fps(), fpsLabel(it), dim, true) },
        DetailRow(FrameIcons.Info, DetailStrings.status(), statusText(project.status), statusColor(project.status, colors), false)
    )
}

@Composable
private fun DetailRowView(row: DetailRow) {
    val colors = FrameTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(row.tint.copy(alpha = 0.14f)), Alignment.Center) {
            Image(painterResource(row.icon), null, Modifier.size(16.dp), colorFilter = ColorFilter.tint(row.tint))
        }
        FrameText(row.label, style = FrameTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 1, modifier = Modifier.weight(1f))
        Box(Modifier.weight(1.4f), Alignment.CenterEnd) {
            val style = FrameTheme.typography.label.copy(fontSize = 13.sp)
            FrameText(
                row.value,
                style = if (row.ltr) style.copy(textDirection = TextDirection.Ltr) else style,
                color = if (row.tint == colors.primary) colors.onSurface else row.tint,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

internal fun formatStamp(ms: Long): String {
    val date = Date(ms)
    val day = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(date)
    val time = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).format(date)
    return day + FrameStrings.sep() + time
}
