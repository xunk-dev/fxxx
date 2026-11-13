package com.frameout.app.splash

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import com.frameout.designsystem.theme.FrameAccent
import kotlin.math.PI
import kotlin.math.sin

private fun DrawScope.layerIn(p: Float, shiftX: Float, shiftY: Float, block: DrawScope.(Float) -> Unit) {
    val a = seg(p, 0f, 0.45f)
    if (a <= 0f) return
    val k = outBack(p)
    withTransform({
        translate(shiftX * (1f - k), shiftY * (1f - k))
        scale(0.92f + 0.08f * k, 0.92f + 0.08f * k, FRAME_PIVOT)
    }) { block(a) }
}

internal fun DrawScope.drawIcon(t: Float, accent: FrameAccent, paths: SplashPaths) {
    val b = logoBrushes(accent)
    layerIn(seg(t, 100f, 520f), -90f, 90f) { a -> drawPath(paths.strip, b.strip, alpha = a) }
    layerIn(seg(t, 200f, 620f), 0f, -80f) { a ->
        drawPath(paths.bandBase, b.bandBase, alpha = a)
        drawPath(paths.bandShade, b.bandShade, alpha = a)
    }
    layerIn(seg(t, 280f, 680f), -50f, 50f) { a -> drawPath(paths.connector, b.connector, alpha = a) }
    layerIn(seg(t, 200f, 620f), 0f, -80f) { a -> drawPath(paths.face, b.face, alpha = a) }
    layerIn(seg(t, 320f, 760f), 90f, 0f) { a -> drawPath(paths.trailOuter, b.trailOuter, alpha = a) }
    layerIn(seg(t, 380f, 820f), 70f, 0f) { a -> drawPath(paths.trailMid, b.trailMid, alpha = a) }
    val frameP = seg(t, 480f, 920f)
    val frameA = seg(frameP, 0f, 0.45f)
    if (frameA <= 0f) return
    val k = 0.78f + 0.22f * outBack(frameP)
    withTransform({ scale(k, k, FRAME_PIVOT) }) {
        val mountainA = outCubic(seg(t, 680f, 1020f)) * frameA
        if (mountainA > 0f) drawPath(paths.mountain, b.mountain, alpha = mountainA)
        drawSun(t, paths, b, frameA)
        drawPath(paths.ring, b.ring, alpha = frameA)
        drawGlint(t, paths)
    }
}

private fun DrawScope.drawSun(t: Float, paths: SplashPaths, b: LogoBrushes, alpha: Float) {
    val sunP = seg(t, 800f, 1120f)
    if (sunP > 0f) {
        val k = outBack(sunP)
        withTransform({ scale(k, k, SUN_CENTER) }) { drawPath(paths.sun, b.sun, alpha = alpha) }
    }
    val pulse = outCubic(seg(t, 900f, 1400f))
    if (pulse > 0f && pulse < 1f) {
        drawCircle(
            color = Color.White.copy(alpha = 0.7f * (1f - pulse)),
            radius = SUN_R + 100f * pulse,
            center = SUN_CENTER,
            style = Stroke(width = 5f * (1f - pulse) + 0.5f)
        )
    }
}

private fun DrawScope.drawGlint(t: Float, paths: SplashPaths) = drawGlintAt(inOut(seg(t, 980f, 1450f)), paths)

internal fun DrawScope.drawGlintAt(glint: Float, paths: SplashPaths) {
    if (glint <= 0f || glint >= 1f) return
    val mx = 480f + 560f * glint
    val my = 960f - 420f * glint
    val band = Brush.linearGradient(
        colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.55f * sin(PI.toFloat() * glint)), Color.Transparent),
        start = Offset(mx - 100f, my + 100f),
        end = Offset(mx + 100f, my - 100f)
    )
    clipPath(paths.ring) { drawRect(band, Offset(450f, 520f), Size(620f, 460f)) }
}
