package com.frameout.app.billing

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.ColorFilter
import com.frameout.designsystem.icons.FrameIconsExtra
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.app.R
import com.frameout.core.pro.ProFeature
import com.frameout.core.pro.ProStrings
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameHairline

private const val MAX_STAGGER = 6
private const val COLLAPSED_COUNT = 6
private const val COLUMNS = 2

private class ProItem(val art: Int, val name: String, val line: String, val feature: ProFeature?, val from: Color, val to: Color) {
    val key: String get() = feature?.key ?: NO_ADS_KEY
}

private fun featureItem(art: Int, feature: ProFeature, from: Long, to: Long): ProItem =
    ProItem(art, ProStrings.featureName(feature), ProStrings.featureLine(feature), feature, Color(from), Color(to))

private fun proItems(): List<ProItem> = listOf(
    featureItem(R.drawable.pro_art_smart, ProFeature.SmartExtract, 0xFFFF5F3D, 0xFFFFB02E),
    featureItem(R.drawable.pro_art_filters, ProFeature.Filters, 0xFF12B886, 0xFF84CC16),
    featureItem(R.drawable.pro_art_shapes, ProFeature.Shapes, 0xFF0EA5A4, 0xFF22C55E),
    featureItem(R.drawable.pro_art_edit, ProFeature.EditTools, 0xFF3B82F6, 0xFF22D3EE),
    featureItem(R.drawable.pro_art_formats, ProFeature.ExportFormats, 0xFFF43F5E, 0xFFFB923C),
    featureItem(R.drawable.pro_art_export, ProFeature.AdvancedExport, 0xFF8B5CF6, 0xFFEC4899),
    featureItem(R.drawable.pro_art_collage, ProFeature.Collage, 0xFF14B8A6, 0xFF3B82F6),
    featureItem(R.drawable.pro_art_batch, ProFeature.Batch, 0xFFFF6B9A, 0xFFFF9A5C),
    featureItem(R.drawable.pro_art_compare, ProFeature.Compare, 0xFFFF8A3D, 0xFFFFC24D),
    featureItem(R.drawable.pro_art_accents, ProFeature.Accents, 0xFF7C5CFF, 0xFFB06BFF),
    featureItem(R.drawable.pro_art_icons, ProFeature.AppIcons, 0xFF06B6D4, 0xFF6366F1),
    featureItem(R.drawable.pro_art_insights, ProFeature.Insights, 0xFFEAB308, 0xFFF97316),
    ProItem(R.drawable.pro_art_noads, ProStrings.noAdsName(), ProStrings.noAdsLine(), null, Color(0xFF6366F1), Color(0xFFA855F7))
)

@Composable
fun ProFeatureList(motion: SheetMotion, currentKey: String?, modifier: Modifier = Modifier, onSelect: ((String) -> Unit)? = null) {
    val items = proItems()
    Column(modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        items.chunked(COLUMNS).forEachIndexed { row, pair ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                pair.forEach { item ->
                    val open = if (onSelect != null) ({ onSelect(item.key) }) else null
                    val tile = Modifier.weight(1f).fillMaxHeight().revealed(motion.reveal, 2 + minOf(row, MAX_STAGGER))
                    ProFeatureTile(item, item.key == currentKey, open, tile)
                }
            }
        }
    }
}

@Composable
private fun ProFeatureTile(item: ProItem, highlight: Boolean, onOpen: (() -> Unit)?, modifier: Modifier) {
    val colors = FrameTheme.colors
    val fill = if (highlight) colors.primary.copy(alpha = 0.12f) else colors.onSurface.copy(alpha = 0.05f)
    val edge = if (highlight) colors.primary.copy(alpha = 0.75f) else frameHairline()
    PressSurface(
        onClick = onOpen,
        shape = FrameShapes.md,
        fill = fill,
        edge = edge,
        modifier = modifier.heightIn(min = 44.dp).semantics(mergeDescendants = true) { contentDescription = item.name + ". " + item.line },
        edgeWidth = if (highlight) 1.5.dp else 1.dp,
        onClickLabel = if (onOpen != null) ProStrings.seeFeature() else null,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ArtTile(item, Modifier.padding(start = 6.dp, top = 5.dp, bottom = 5.dp))
        FrameText(item.name, FrameTheme.typography.titleSmall.copy(fontSize = 12.5.sp, lineHeight = 15.sp), colors.onSurface, Modifier.weight(1f).padding(end = 8.dp), maxLines = 2)
    }
}

@Composable
private fun ArtTile(item: ProItem, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(9.dp)
    Box(
        modifier
            .size(32.dp)
            .clip(shape)
            .background(Brush.linearGradient(listOf(item.from, item.to)))
            .border(1.dp, Color.White.copy(alpha = 0.28f), shape),
        Alignment.Center
    ) {
        Image(painterResource(item.art), null, Modifier.size(22.dp))
    }
}
