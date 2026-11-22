package com.frameout.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.frameout.core.util.EditStrings
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.ContrastGuard
import com.frameout.designsystem.theme.FramePressIndication
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme

private val ButtonShape = RoundedCornerShape(18.dp)

@Composable
fun UnsavedLeaveDialog(onSaveLeave: () -> Unit, onLeave: () -> Unit, onStay: () -> Unit, body: String = EditStrings.unsavedBody()) {
    val palette = dialogPalette()
    val reduce = LocalSystemA11y.current.reduceMotion
    val enter = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduce) enter.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow))
    }
    Dialog(onDismissRequest = onStay, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        CompositionLocalProvider(LocalIndication provides FramePressIndication) {
            UnsavedCard(palette, body, onSaveLeave, onLeave, onStay) { enter.value }
        }
    }
}

private class DialogPalette(
    val card: Color,
    val warning: Color,
    val bodyColor: Color,
    val onPrimary: Color,
    val dangerFill: Color,
    val dangerText: Color
)

@Composable
private fun dialogPalette(): DialogPalette {
    val colors = FrameTheme.colors
    val card = if (colors.isDark) colors.container else colors.surface
    val dangerFill = colors.error.copy(alpha = 0.15f)
    return DialogPalette(
        card = card,
        warning = ContrastGuard.ensureAgainst(colors.warning, card, ContrastGuard.BOUNDARY),
        bodyColor = ContrastGuard.ensureAgainst(colors.onSurfaceVariant, card, ContrastGuard.TEXT),
        onPrimary = ContrastGuard.readableOn(colors.primary, listOf(colors.onPrimary, Color.White, Color.Black)),
        dangerFill = dangerFill,
        dangerText = ContrastGuard.ensureAgainst(colors.error, dangerFill.compositeOver(card), ContrastGuard.TEXT)
    )
}

@Composable
private fun UnsavedCard(
    palette: DialogPalette, body: String, onSaveLeave: () -> Unit, onLeave: () -> Unit, onStay: () -> Unit, progress: () -> Float
) {
    val colors = FrameTheme.colors
    Column(
        Modifier
            .padding(horizontal = 24.dp)
            .widthIn(max = 380.dp)
            .fillMaxWidth()
            .graphicsLayer {
                val p = progress()
                scaleX = 0.94f + 0.06f * p
                scaleY = 0.94f + 0.06f * p
                alpha = p.coerceIn(0f, 1f)
            }
            .clip(FrameShapes.xxl)
            .background(palette.card)
            .border(1.dp, colors.outline.copy(alpha = if (colors.isDark) 0.45f else 0.35f), FrameShapes.xxl)
            .heightIn(max = 640.dp)
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        UnsavedHeader(palette, body)
        UnsavedActions(palette, onSaveLeave, onLeave, onStay)
    }
}

@Composable
private fun UnsavedHeader(palette: DialogPalette, body: String) {
    Box(Modifier.size(64.dp).clip(CircleShape).background(palette.warning.copy(alpha = 0.14f)), Alignment.Center) {
        Image(painterResource(FrameIconsExtra.Warning), null, Modifier.size(30.dp), colorFilter = ColorFilter.tint(palette.warning))
    }
    FrameText(
        EditStrings.unsavedTitle(),
        style = FrameTheme.typography.title,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 18.dp)
    )
    FrameText(
        body,
        style = FrameTheme.typography.body,
        color = palette.bodyColor,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp)
    )
}

@Composable
private fun UnsavedActions(palette: DialogPalette, onSaveLeave: () -> Unit, onLeave: () -> Unit, onStay: () -> Unit) {
    val colors = FrameTheme.colors
    DialogAction(
        EditStrings.unsavedSaveLeave(), onSaveLeave, palette.onPrimary,
        Modifier.padding(top = 22.dp).fillMaxWidth().heightIn(min = 54.dp).background(colors.primary, ButtonShape),
        bold = true
    )
    Row(
        Modifier.padding(top = 10.dp).fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        DialogAction(
            EditStrings.keepEditing(), onStay, colors.onSurface,
            Modifier.weight(1f).fillMaxHeight().heightIn(min = 50.dp).border(1.dp, colors.outline.copy(alpha = 0.7f), ButtonShape)
        )
        DialogAction(
            EditStrings.unsavedLeave(), onLeave, palette.dangerText,
            Modifier.weight(1f).fillMaxHeight().heightIn(min = 50.dp).background(palette.dangerFill, ButtonShape),
            bold = true
        )
    }
}

@Composable
private fun DialogAction(label: String, onClick: () -> Unit, textColor: Color, modifier: Modifier, bold: Boolean = false) {
    Box(
        modifier.clip(ButtonShape).frameClickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 12.dp),
        Alignment.Center
    ) {
        FrameText(
            label,
            style = FrameTheme.typography.label.let { if (bold) it.copy(fontWeight = FontWeight.Bold) else it },
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}
