package com.frameout.app.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import com.frameout.app.trim.TrimOutput
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.SavedStrings
import com.frameout.core.util.TrimStrings
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameTheme

private const val PREVIEW_MS = 2800

internal enum class ExportStyle(val key: String, val icon: Int) {
    Video("video", FrameIconsExtra.Clip),
    Live("live", FrameIconsExtra.Live),
    Gif("gif", FrameIconsExtra.Gif),
    Webp("webp", FrameIconsExtra.Webp),
    Zip("zip", FrameIconsExtra.Storage),
    Pdf("pdf", FrameIconsExtra.SavedFile),
    Contact("contact", FrameIconsExtra.ContactSheet),
    Folder("folder", FrameIconsExtra.Folder);

    val isVideo: Boolean get() = ordinal <= Webp.ordinal

    val asset: String get() = "export_prev_$key"

    val trimOutput: TrimOutput?
        get() = when (this) {
            Video -> TrimOutput.Video
            Live -> TrimOutput.Live
            Gif -> TrimOutput.Gif
            Webp -> TrimOutput.Webp
            else -> null
        }

    fun title(): String = when (this) {
        Video -> TrimStrings.outVideo()
        Live -> TrimStrings.outLive()
        Gif -> TrimStrings.outGif()
        Webp -> TrimStrings.outWebp()
        Zip -> FrameStrings.exportZip()
        Pdf -> FrameStrings.exportPdf()
        Contact -> SavedStrings.exportContact()
        Folder -> SavedStrings.exportFolder()
    }

    fun description(): String = when (this) {
        Video -> TrimStrings.outDesc("video")
        Live -> TrimStrings.outDesc("live")
        Gif -> TrimStrings.outDesc("gif")
        Webp -> TrimStrings.outDesc("webp")
        Zip -> SavedStrings.exportZipHint()
        Pdf -> SavedStrings.exportPdfHint()
        Contact -> SavedStrings.exportContactHint()
        Folder -> SavedStrings.exportFolderHint()
    }

    companion object {
        val VideoStyles = listOf(Video, Live, Gif, Webp)
        val ImageStyles = listOf(Zip, Pdf, Contact, Folder)
        fun fromKey(key: String?): ExportStyle? = entries.firstOrNull { it.key == key }
    }
}

@Composable
internal fun rememberPreviewPhase(reduceMotion: Boolean): State<Float> {
    if (reduceMotion) return remember { mutableFloatStateOf(0.62f) }
    val transition = rememberInfiniteTransition(label = "exportPreview")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(PREVIEW_MS, easing = LinearEasing)),
        label = "exportPhase"
    )
}

@Composable
internal fun ExportPreview(style: ExportStyle, phase: State<Float>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val id = remember(style) { context.resources.getIdentifier(style.asset, "drawable", context.packageName) }
    val colors = FrameTheme.colors
    val accent = colors.primary
    val ink = colors.onSurface
    val play = !LocalSystemA11y.current.reduceMotion
    var failed by remember(style) { mutableStateOf(false) }
    val glassId = remember(style) { context.resources.getIdentifier("glass_${style.asset}", "drawable", context.packageName) }
    val vector = remember(id, glassId) { glassId != 0 || (id != 0 && isXmlDrawable(context, id)) }
    val stage = if (vector) glassStageOrNull() else null
    Box(modifier.background(stage ?: Brush.linearGradient(listOf(accent.copy(alpha = 0.30f), colors.containerHigh, accent.copy(alpha = 0.10f))))) {
        if (vector) {
            GlassArtwork(
                resId = if (glassId != 0) glassId else id,
                primary = colors.onDarkAccent,
                secondary = FrameTheme.accent.c,
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    val w = if (play) kotlin.math.sin(phase.value * 2f * Math.PI.toFloat()) else 0f
                    translationY = w * size.height * 0.012f
                    val sc = 1f + w * 0.012f
                    scaleX = sc
                    scaleY = sc
                }
            )
        } else if (id != 0 && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P && !failed) {
            AnimatedPreviewImage(id, play, onFailed = { failed = true }, modifier = Modifier.fillMaxSize())
        } else {
            Canvas(Modifier.fillMaxSize()) { drawPreview(style, phase.value, accent, ink) }
        }
    }
}

internal fun pvEase(x: Float) = x * x * (3f - 2f * x)
internal fun pvTri(t: Float) = if (t < 0.5f) t * 2f else (1f - t) * 2f
internal fun pvSeg(t: Float, from: Float, to: Float) = ((t - from) / (to - from)).coerceIn(0f, 1f)
internal fun pvLerp(a: Float, b: Float, f: Float) = a + (b - a) * f

private fun DrawScope.drawPreview(style: ExportStyle, t: Float, accent: Color, ink: Color) {
    when (style) {
        ExportStyle.Video -> drawVideo(t, accent, ink)
        ExportStyle.Live -> pvDrawLive(t, accent, ink)
        ExportStyle.Gif -> pvDrawGif(t, accent)
        ExportStyle.Webp -> pvDrawWebp(t, accent, ink)
        ExportStyle.Zip -> pvDrawZip(t, accent)
        ExportStyle.Pdf -> pvDrawPdf(t, accent, ink)
        ExportStyle.Contact -> pvDrawContact(t, accent)
        ExportStyle.Folder -> pvDrawFolder(t, accent)
    }
}

internal fun DrawScope.pvTile(cx: Float, cy: Float, side: Float, color: Color) {
    drawRoundRect(color, Offset(cx - side / 2f, cy - side / 2f), Size(side, side), CornerRadius(side * 0.18f))
}

private fun DrawScope.drawVideo(t: Float, accent: Color, ink: Color) {
    val w = size.width
    val h = size.height
    val screenW = w * 0.46f
    val screenH = h * 0.44f
    val sx = (w - screenW) / 2f
    val sy = h * 0.10f
    drawRoundRect(ink.copy(alpha = 0.14f), Offset(sx, sy), Size(screenW, screenH), CornerRadius(h * 0.06f))
    val cx = w / 2f
    val cy = sy + screenH / 2f
    val r = screenH * 0.22f
    val play = Path().apply {
        moveTo(cx - r * 0.5f, cy - r)
        lineTo(cx - r * 0.5f, cy + r)
        lineTo(cx + r, cy)
        close()
    }
    drawPath(play, accent.copy(alpha = 0.9f))
    val fx = w * 0.14f
    val fw = w * 0.72f
    val fy = h * 0.66f
    val fh = h * 0.22f
    val n = 6
    val gap = fw * 0.012f
    val cell = (fw - gap * (n - 1)) / n
    for (i in 0 until n) {
        drawRoundRect(ink.copy(alpha = 0.22f), Offset(fx + i * (cell + gap), fy), Size(cell, fh), CornerRadius(fh * 0.12f))
    }
    val line = fh * 0.07f
    drawLine(accent, Offset(fx, fy - line), Offset(fx + fw, fy - line), strokeWidth = line)
    drawLine(accent, Offset(fx, fy + fh + line), Offset(fx + fw, fy + fh + line), strokeWidth = line)
    val hw = fw * 0.03f
    drawRoundRect(accent, Offset(fx - hw, fy - line * 2f), Size(hw, fh + line * 4f), CornerRadius(hw / 2f))
    drawRoundRect(accent, Offset(fx + fw, fy - line * 2f), Size(hw, fh + line * 4f), CornerRadius(hw / 2f))
    val px = fx + fw * t
    drawLine(Color.White, Offset(px, fy), Offset(px, fy + fh), strokeWidth = line)
    drawCircle(color = Color.White, radius = fh * 0.17f, center = Offset(px, fy + fh / 2f))
}
