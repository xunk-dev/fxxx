package com.frameout.app.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.min

internal fun DrawScope.pvDrawLive(t: Float, accent: Color, ink: Color) {
    val w = size.width
    val h = size.height
    val c = Offset(w / 2f, h / 2f)
    val base = min(w, h) * 0.14f
    drawRoundRect(ink.copy(alpha = 0.14f), Offset(c.x - w * 0.2f, c.y - h * 0.3f), Size(w * 0.4f, h * 0.6f), CornerRadius(h * 0.06f))
    for (k in 0..2) {
        val p = (t + k / 3f) % 1f
        drawCircle(
            color = accent.copy(alpha = (1f - p) * 0.6f),
            radius = base + p * min(w, h) * 0.34f,
            center = c,
            style = Stroke(width = base * 0.14f)
        )
    }
    drawCircle(color = accent, radius = base * 0.6f, center = c)
    drawCircle(color = Color.White, radius = base * 0.22f, center = c)
}

internal fun DrawScope.pvDrawGif(t: Float, accent: Color) {
    val w = size.width
    val h = size.height
    val side = min(w, h) * 0.24f
    val step = (t * 3f).toInt().coerceIn(0, 2)
    val local = t * 3f - step
    for (i in 0..2) {
        val active = i == step
        val s = side * if (active) 1f + 0.14f * pvTri(local) else 0.88f
        pvTile(w * (0.25f + 0.25f * i), h * 0.46f, s, accent.copy(alpha = if (active) 0.88f else 0.3f))
    }
    val d = min(w, h) * 0.12f
    drawArc(
        color = accent,
        startAngle = -90f,
        sweepAngle = 360f * t,
        useCenter = false,
        topLeft = Offset(w / 2f - d / 2f, h * 0.78f - d / 2f),
        size = Size(d, d),
        style = Stroke(width = d * 0.22f, cap = StrokeCap.Round)
    )
}

internal fun DrawScope.pvDrawWebp(t: Float, accent: Color, ink: Color) {
    val w = size.width
    val h = size.height
    val fw = w * 0.42f
    val fh = h * 0.56f
    val left = (w - fw) / 2f
    val top = h * 0.12f
    val corner = CornerRadius(h * 0.06f)
    drawRoundRect(ink.copy(alpha = 0.16f), Offset(left, top), Size(fw, fh), corner)
    val clip = Path().apply { addRoundRect(RoundRect(left, top, left + fw, top + fh, corner)) }
    clipPath(clip) {
        val x = left - fw * 0.5f + (fw * 2f) * t
        drawRect(
            brush = Brush.horizontalGradient(
                listOf(Color.Transparent, accent.copy(alpha = 0.7f), Color.Transparent),
                startX = x - fw * 0.4f,
                endX = x + fw * 0.4f
            ),
            topLeft = Offset(left, top),
            size = Size(fw, fh)
        )
    }
    val active = (t * 4f).toInt().coerceIn(0, 3)
    val r = min(w, h) * 0.022f
    for (i in 0..3) {
        drawCircle(
            color = if (i == active) accent else ink.copy(alpha = 0.3f),
            radius = if (i == active) r * 1.4f else r,
            center = Offset(w / 2f + (i - 1.5f) * r * 4.5f, h * 0.84f)
        )
    }
}

internal fun DrawScope.pvDrawZip(t: Float, accent: Color) {
    val w = size.width
    val h = size.height
    val m = min(w, h)
    val move = pvEase(pvSeg(t, 0.05f, 0.6f))
    val alpha = pvSeg(t, 0f, 0.1f) * (1f - pvSeg(t, 0.55f, 0.65f))
    val target = Offset(w / 2f, h * 0.60f)
    val starts = listOf(Offset(w * 0.25f, h * 0.2f), Offset(w * 0.75f, h * 0.2f), Offset(w * 0.3f, h * 0.45f), Offset(w * 0.7f, h * 0.45f))
    val boxW = m * 0.46f
    val boxH = m * 0.34f
    val pulse = 1f + 0.05f * pvTri(pvSeg(t, 0.55f, 0.85f))
    drawRoundRect(
        accent.copy(alpha = 0.85f),
        Offset(target.x - boxW * pulse / 2f, target.y - boxH * pulse / 2f),
        Size(boxW * pulse, boxH * pulse),
        CornerRadius(m * 0.05f)
    )
    for (i in 0 until 6) {
        drawRect(
            Color.White.copy(alpha = if (i % 2 == 0) 0.9f else 0.35f),
            Offset(target.x - m * 0.012f, target.y - boxH / 2f + i * boxH / 6f),
            Size(m * 0.024f, boxH / 6f)
        )
    }
    starts.forEach { s ->
        pvTile(pvLerp(s.x, target.x, move), pvLerp(s.y, target.y, move), m * 0.16f * (1f - 0.4f * move), accent.copy(alpha = 0.55f * alpha))
    }
}

internal fun DrawScope.pvDrawPdf(t: Float, accent: Color, ink: Color) {
    val w = size.width
    val h = size.height
    val pw = w * 0.26f
    val ph = h * 0.62f
    val pivot = Offset(w / 2f, h * 0.92f)
    val spread = 8f + 8f * pvTri(t)
    for (i in listOf(-1, 1, 0)) {
        rotate(degrees = i * spread, pivot = pivot) {
            val front = i == 0
            val tl = Offset(w / 2f - pw / 2f, pivot.y - ph)
            drawRoundRect(if (front) Color.White.copy(alpha = 0.92f) else ink.copy(alpha = 0.2f), tl, Size(pw, ph), CornerRadius(pw * 0.08f))
            if (front) {
                drawRoundRect(accent, Offset(tl.x + pw * 0.14f, tl.y + ph * 0.1f), Size(pw * 0.5f, ph * 0.07f), CornerRadius(ph * 0.03f))
                for (k in 0 until 4) {
                    drawRoundRect(
                        Color.Black.copy(alpha = 0.18f),
                        Offset(tl.x + pw * 0.14f, tl.y + ph * (0.27f + 0.13f * k)),
                        Size(pw * if (k == 3) 0.4f else 0.72f, ph * 0.04f),
                        CornerRadius(ph * 0.02f)
                    )
                }
            }
        }
    }
}

internal fun DrawScope.pvDrawContact(t: Float, accent: Color) {
    val w = size.width
    val h = size.height
    val cols = 3
    val rows = 2
    val cell = min(w * 0.7f / cols, h * 0.64f / rows)
    val gap = cell * 0.12f
    val gridW = cols * cell + (cols - 1) * gap
    val gridH = rows * cell + (rows - 1) * gap
    val fade = 1f - pvSeg(t, 0.9f, 1f)
    for (i in 0 until cols * rows) {
        val p = pvEase(pvSeg(t, i * 0.08f, i * 0.08f + 0.25f))
        val cx = (w - gridW) / 2f + (i % cols) * (cell + gap) + cell / 2f
        val cy = (h - gridH) / 2f + (i / cols) * (cell + gap) + cell / 2f
        pvTile(cx, cy, cell * p, accent.copy(alpha = (0.35f + 0.5f * ((i * 37) % 10) / 10f) * fade))
    }
}

internal fun DrawScope.pvDrawFolder(t: Float, accent: Color) {
    val w = size.width
    val h = size.height
    val m = min(w, h)
    val fw = m * 0.62f
    val fh = m * 0.38f
    val left = (w - fw) / 2f
    val top = h * 0.56f
    val bounce = 1f + 0.04f * pvTri(pvSeg(t, 0.5f, 0.9f))
    for (i in 0..2) {
        val p = pvEase(pvSeg(t, i * 0.12f, i * 0.12f + 0.4f))
        val x = w * (0.34f + 0.16f * i)
        val y = pvLerp(h * 0.05f, top + fh * 0.2f, p)
        pvTile(x, y, m * 0.15f, accent.copy(alpha = 0.7f * (1f - pvSeg(p, 0.85f, 1f))))
    }
    drawRoundRect(accent.copy(alpha = 0.6f), Offset(left, top - fh * 0.16f), Size(fw * 0.38f, fh * 0.3f), CornerRadius(m * 0.03f))
    drawRoundRect(
        accent.copy(alpha = 0.92f),
        Offset(left - fw * (bounce - 1f) / 2f, top),
        Size(fw * bounce, fh),
        CornerRadius(m * 0.05f)
    )
}
