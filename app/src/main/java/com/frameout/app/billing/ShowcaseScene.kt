package com.frameout.app.billing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

internal const val SCENE_W_DP = 248
internal const val SCENE_H_DP = 150
private const val SOFT_DIVISOR = 9
private const val BLOCK_DIVISOR = 16

internal object SceneZones {
    val face = Offset(0.27f, 0.50f)
    const val FACE_R = 0.115f
    val plate = Rect(0.55f, 0.66f, 0.90f, 0.84f)
}

internal class SceneSpots(val face: Offset, val faceR: Float, val plate: Rect, val box: Rect = plate, val ring: Float = 0.20f)

private val DefaultSpots = SceneSpots(SceneZones.face, SceneZones.FACE_R, SceneZones.plate, Rect(0.53f, 0.64f, 0.92f, 0.86f))

internal enum class ShowcaseArt(val asset: String, val pairAsset: String? = null, val spots: SceneSpots = DefaultSpots) {
    Filter("paywall/filters.webp"),
    Shapes("paywall/shapes.webp", spots = SceneSpots(Offset(0.265f, 0.70f), 0.115f, Rect(0.64f, 0.68f, 0.98f, 0.99f), ring = 0.25f)),
    Tool("paywall/tool.webp", spots = SceneSpots(Offset(0.254f, 0.43f), 0.075f, Rect(0.765f, 0.505f, 0.885f, 0.57f))),
    Collage("paywall/collage.webp"),
    Compare("paywall/compare_a.webp", "paywall/compare_b.webp"),
    Insights("paywall/insights.webp", spots = SceneSpots(Offset(0.30f, 0.53f), 0.075f, Rect(0.805f, 0.695f, 0.94f, 0.76f)))
}

internal class ShowcaseScene(
    val sharp: ImageBitmap,
    val soft: ImageBitmap,
    val blocky: ImageBitmap,
    val width: Int,
    val height: Int,
    val source: ImageBitmap?,
    val other: ImageBitmap? = null,
    val heat: ImageBitmap? = null,
    val spots: SceneSpots = DefaultSpots
) {
    fun matches(w: Int, h: Int, sample: ImageBitmap?): Boolean = width == w && height == h && source === sample
}

internal object ShowcaseSceneCache {
    var current: ShowcaseScene? by mutableStateOf(null)
        private set
    private val art = HashMap<ShowcaseArt, ShowcaseScene>()

    fun find(w: Int, h: Int, sample: ImageBitmap?): ShowcaseScene? = current?.takeIf { it.matches(w, h, sample) }

    fun findArt(kind: ShowcaseArt, w: Int, h: Int): ShowcaseScene? = synchronized(art) { art[kind]?.takeIf { it.width == w && it.height == h } }

    fun store(scene: ShowcaseScene) {
        current = scene
    }

    fun storeArt(kind: ShowcaseArt, scene: ShowcaseScene) {
        synchronized(art) { art[kind] = scene }
    }

    fun clear() {
        current = null
        synchronized(art) { art.clear() }
    }
}

@Composable
internal fun rememberShowcaseScene(art: ShowcaseArt? = null): ShowcaseScene? {
    val context = LocalContext.current
    val density = LocalDensity.current
    val sample = LocalPaywallSample.current
    val w = with(density) { SCENE_W_DP.dp.roundToPx() }
    val h = with(density) { SCENE_H_DP.dp.roundToPx() }
    val ready = if (art != null) ShowcaseSceneCache.findArt(art, w, h) else ShowcaseSceneCache.find(w, h, sample)
    return produceState(ready, art, w, h, sample) {
        if (value != null) return@produceState
        val appContext = context.applicationContext
        val built = withContext(Dispatchers.Default) {
            val fromArt = art?.takeIf { assetExists(appContext, it.asset) }?.let { buildArtScene(appContext, it, w, h, density) }
            fromArt ?: buildScene(w, h, density, sample)
        }
        if (art != null) ShowcaseSceneCache.storeArt(art, built) else ShowcaseSceneCache.store(built)
        value = built
    }.value
}

private fun assetExists(context: Context, path: String): Boolean = try {
    context.assets.list(path.substringBeforeLast('/'))?.contains(path.substringAfterLast('/')) == true
} catch (e: Exception) {
    false
}

private fun decodeAsset(context: Context, path: String): ImageBitmap? = try {
    context.assets.open(path).use { BitmapFactory.decodeStream(it) }?.asImageBitmap()
} catch (e: Exception) {
    null
}

private fun buildArtScene(context: Context, art: ShowcaseArt, w: Int, h: Int, density: Density): ShowcaseScene? {
    val main = decodeAsset(context, art.asset) ?: return null
    val sharp = render(w, h, density) { drawCover(main, null) }
    val soft = lowPass(sharp, w, h, SOFT_DIVISOR, FilterQuality.High)
    val tiny = lowPass(sharp, w, h, BLOCK_DIVISOR, FilterQuality.Medium)
    val pair = art.pairAsset?.let { decodeAsset(context, it) }?.let { b -> render(w, h, density) { drawCover(b, null) } }
    val heat = pair?.let { differenceHeat(sharp, it, w, h) }
    return ShowcaseScene(sharp, soft, tiny, w, h, null, pair, heat, art.spots)
}

private const val DIFF_THRESHOLD = 34
private const val DIFF_CELL = 4
private val HeatColor = intArrayOf(255, 77, 109)

private fun differenceHeat(a: ImageBitmap, b: ImageBitmap, w: Int, h: Int): ImageBitmap {
    val pa = IntArray(w * h)
    val pb = IntArray(w * h)
    a.readPixels(pa, 0, 0, w, h)
    b.readPixels(pb, 0, 0, w, h)
    val cw = (w + DIFF_CELL - 1) / DIFF_CELL
    val ch = (h + DIFF_CELL - 1) / DIFF_CELL
    val hits = FloatArray(cw * ch)
    for (y in 0 until h) {
        for (x in 0 until w) {
            val i = y * w + x
            val x1 = pa[i]
            val x2 = pb[i]
            val d = max(
                kotlin.math.abs(((x1 shr 16) and 0xFF) - ((x2 shr 16) and 0xFF)),
                max(kotlin.math.abs(((x1 shr 8) and 0xFF) - ((x2 shr 8) and 0xFF)), kotlin.math.abs((x1 and 0xFF) - (x2 and 0xFF)))
            )
            if (d > DIFF_THRESHOLD) hits[(y / DIFF_CELL) * cw + (x / DIFF_CELL)] += 1f
        }
    }
    val cells = DIFF_CELL * DIFF_CELL
    val mask = IntArray(cw * ch)
    for (cy in 0 until ch) {
        for (cx in 0 until cw) {
            var best = 0f
            for (dy in -1..1) for (dx in -1..1) {
                val nx = cx + dx
                val ny = cy + dy
                if (nx in 0 until cw && ny in 0 until ch) best = max(best, hits[ny * cw + nx] / cells)
            }
            val alpha = (best * 2.4f).coerceIn(0f, 1f) * 255f
            mask[cy * cw + cx] = (alpha.toInt() shl 24) or (HeatColor[0] shl 16) or (HeatColor[1] shl 8) or HeatColor[2]
        }
    }
    val bitmap = Bitmap.createBitmap(mask, cw, ch, Bitmap.Config.ARGB_8888)
    return lowPass(bitmap.asImageBitmap(), w, h, 6, FilterQuality.High)
}

private fun buildScene(w: Int, h: Int, density: Density, sample: ImageBitmap?): ShowcaseScene {
    val sharp = render(w, h, density) { if (sample != null) drawCover(sample, null) else drawSceneArt() }
    val soft = lowPass(sharp, w, h, SOFT_DIVISOR, FilterQuality.High)
    val tiny = lowPass(sharp, w, h, BLOCK_DIVISOR, FilterQuality.Medium)
    return ShowcaseScene(sharp, soft, tiny, w, h, sample)
}

private fun render(w: Int, h: Int, density: Density, block: DrawScope.() -> Unit): ImageBitmap {
    val bitmap = ImageBitmap(w, h)
    CanvasDrawScope().draw(density, LayoutDirection.Ltr, Canvas(bitmap), Size(w.toFloat(), h.toFloat()), block)
    return bitmap
}

private fun lowPass(src: ImageBitmap, w: Int, h: Int, divisor: Int, quality: FilterQuality): ImageBitmap {
    val sw = max(2, w / divisor)
    val sh = max(2, h / divisor)
    val small = render(sw, sh, Density(1f)) { drawImage(src, dstSize = IntSize(sw, sh), filterQuality = FilterQuality.Medium) }
    return render(w, h, Density(1f)) { drawImage(small, dstSize = IntSize(w, h), filterQuality = quality) }
}
