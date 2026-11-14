package com.frameout.app.billing

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.frameout.app.R
import com.frameout.core.pro.ProShowcase
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.theme.FrameTheme

@Composable
internal fun SmartBlurryScene(p: Float) {
    val judge = smartRamp(p, 0.15f, 0.7f)
    val tag = smartRamp(p, 0.3f, 0.8f)
    val sharp = ProShowcase.smartSharp()
    val blurry = ProShowcase.smartBlurry()
    val items = listOf(
        R.drawable.smart_bird to true,
        R.drawable.smart_bird_blur to false,
        R.drawable.smart_butterfly_blur to false,
        R.drawable.smart_butterfly to true
    )
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(bottom = 8.dp)) {
        items.forEach { (img, good) ->
            SmartPic(
                img, 56.dp, 74.dp,
                dim = if (good) 0f else judge,
                lift = if (good) judge else 0f,
                ring = if (good) tag else 0f,
                overlay = { SmartVerdict(if (good) sharp else blurry, good, tag) }
            )
        }
    }
}

@Composable
internal fun SmartDuplicateScene(p: Float) {
    val merge = smartRamp(p, 0.15f, 0.7f)
    val tag = smartRamp(p, 0.4f, 0.85f)
    val dup = ProShowcase.smartDup(2)
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(bottom = 14.dp)) {
        listOf(R.drawable.smart_flower, R.drawable.smart_tree).forEach { img ->
            Box(Modifier.size(width = 134.dp, height = 58.dp)) {
                SmartPic(img, 42.dp, 56.dp, modifier = Modifier.offset(x = (46f * merge).dp), ring = merge)
                SmartPic(img, 42.dp, 56.dp, modifier = Modifier.offset(x = 46.dp).graphicsLayer { alpha = 1f - merge })
                SmartPic(img, 42.dp, 56.dp, modifier = Modifier.offset(x = (92f - 46f * merge).dp).graphicsLayer { alpha = 1f - merge })
                SmartPill(
                    dup, SmartCoral, SmartCoralInk, tag,
                    modifier = Modifier.align(Alignment.BottomCenter).offset(y = 28.dp)
                )
            }
        }
    }
}

@Composable
private fun Person(from: Color, to: Color, headY: Float, face: Float, modifier: Modifier = Modifier) {
    Box(modifier.clip(SmartCardShape).background(Brush.linearGradient(listOf(from, to))).border(1.dp, Color.White.copy(alpha = 0.22f), SmartCardShape)) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val body = Color.White.copy(alpha = 0.9f)
            drawCircle(body, radius = w * 0.17f, center = Offset(w / 2f, h * headY))
            drawRoundRect(
                body,
                topLeft = Offset(w * 0.16f, h * (headY + 0.22f)),
                size = Size(w * 0.68f, h),
                cornerRadius = CornerRadius(w * 0.34f)
            )
            if (face > 0f) drawRoundRect(
                SmartMint.copy(alpha = face),
                topLeft = Offset(w / 2f - w * 0.22f, h * headY - w * 0.24f),
                size = Size(w * 0.44f, w * 0.48f),
                cornerRadius = CornerRadius(8.dp.toPx()),
                style = Stroke(2.dp.toPx())
            )
        }
    }
}

@Composable
internal fun SmartPeopleScene(p: Float) {
    val on = smartRamp(p, 0.1f, 0.4f)
    val judge = smartRamp(p, 0.35f, 0.8f)
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FrameText(ProShowcase.smartNames().getOrElse(2) { "" }, style = FrameTheme.typography.label, color = FrameTheme.colors.onSurface)
            Box(
                Modifier.size(38.dp, 22.dp).clip(CircleShape)
                    .background(androidx.compose.ui.graphics.lerp(Color.White.copy(alpha = 0.22f), SmartMint, on))
            ) {
                Box(Modifier.offset(x = (3f + 16f * on).dp, y = 3.dp).size(16.dp).clip(CircleShape).background(Color.White))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            val cardW = 56.dp
            val cardH = 74.dp
            Box {
                Person(Color(0xFF7C6CFF), Color(0xFFB58CFF), 0.38f, judge,
                    Modifier.size(cardW, cardH).graphicsLayer { translationY = -6.dp.toPx() * judge })
            }
            SmartPic(R.drawable.smart_tree, cardW, cardH, dim = judge, overlay = { SmartVerdict("", false, judge) })
            Box {
                Person(Color(0xFF2FC9A0), Color(0xFF5A8CFF), 0.4f, judge,
                    Modifier.size(cardW, cardH).graphicsLayer { translationY = -6.dp.toPx() * judge })
            }
            SmartPic(R.drawable.smart_butterfly, cardW, cardH, dim = judge, overlay = { SmartVerdict("", false, judge) })
        }
    }
}

@Composable
internal fun SmartSceneChangeScene(p: Float) {
    val open = smartRamp(p, 0.15f, 0.7f)
    val tag = smartRamp(p, 0.45f, 0.9f)
    val scenes = listOf(R.drawable.smart_flower, R.drawable.smart_bird, R.drawable.smart_tree)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            scenes.forEachIndexed { index, img ->
                if (index > 0) {
                    Box(Modifier.width((3f + 19f * open).dp).height(60.dp), Alignment.Center) {
                        Box(
                            Modifier.width(2.dp).height((22f + 38f * open).dp)
                                .clip(CircleShape)
                                .background(SmartLilac.copy(alpha = open))
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(3) { n ->
                        SmartPic(img, 28.dp, 38.dp, shape = RoundedCornerShape(7.dp), ring = if (n == 0) tag else 0f)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        if (tag > 0.01f) FrameText(
            ProShowcase.smartKeepFirst(),
            style = FrameTheme.typography.label,
            color = FrameTheme.colors.onSurface.copy(alpha = 0.8f * tag),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SlideFace(modifier: Modifier, kind: Int) {
    val ink = Color(0xFF4C3FB0)
    val soft = Color(0xFF8D86D9)
    Box(modifier.clip(RoundedCornerShape(8.dp)).background(Brush.linearGradient(listOf(Color(0xFFF7F6FF), Color(0xFFD9D5FF))))) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val r = CornerRadius(3.dp.toPx())
            drawRoundRect(ink, Offset(w * 0.1f, h * 0.14f), Size(w * 0.42f, h * 0.1f), r)
            when (kind) {
                0 -> {
                    drawRoundRect(soft, Offset(w * 0.1f, h * 0.46f), Size(w * 0.78f, h * 0.06f), r)
                    drawRoundRect(soft, Offset(w * 0.1f, h * 0.62f), Size(w * 0.6f, h * 0.06f), r)
                }
                1 -> listOf(0.16f, 0.4f, 0.28f).forEachIndexed { i, bh ->
                    drawRoundRect(ink.copy(alpha = 0.8f), Offset(w * (0.16f + 0.22f * i), h * (0.88f - bh * 1.5f)), Size(w * 0.14f, h * bh * 1.5f), r)
                }
                else -> {
                    drawCircle(ink, radius = h * 0.17f, center = Offset(w * 0.24f, h * 0.62f))
                    drawRoundRect(soft, Offset(w * 0.5f, h * 0.5f), Size(w * 0.38f, h * 0.06f), r)
                    drawRoundRect(soft, Offset(w * 0.5f, h * 0.66f), Size(w * 0.28f, h * 0.06f), r)
                }
            }
        }
    }
}

@Composable
internal fun SmartSlidesScene(p: Float) {
    val flash = 1f - kotlin.math.abs(smartRamp(p, 0f, 0.3f) - 0.5f) * 2f
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box {
            SlideFace(Modifier.size(112.dp, 72.dp), 0)
            Box(Modifier.size(112.dp, 72.dp).background(Color.White.copy(alpha = 0.7f * flash.coerceAtLeast(0f))))
        }
        Box(Modifier.size(width = 104.dp, height = 74.dp)) {
            for (i in 0..2) {
                val t = smartRamp(p, 0.15f + 0.18f * i, 0.55f + 0.18f * i)
                SlideFace(
                    Modifier
                        .size(72.dp, 46.dp)
                        .offset(x = (10 * i).dp, y = (28 - 14 * i).dp)
                        .graphicsLayer { alpha = t; translationX = -40.dp.toPx() * (1f - t) },
                    i
                )
            }
            SmartPill("PDF", SmartLilac, SmartLilacInk, smartRamp(p, 0.6f, 1f), modifier = Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-8).dp))
        }
    }
}

@Composable
internal fun SmartSharpestScene(p: Float) {
    val judge = smartRamp(p, 0.2f, 0.7f)
    val bars = smartRamp(p, 0.1f, 0.6f)
    val best = ProShowcase.smartBest()
    val blurs = listOf(4.dp, 2.dp, 0.dp, 3.dp, 5.dp)
    val heights = listOf(12, 24, 44, 18, 8)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            blurs.forEachIndexed { i, b ->
                val win = i == 2
                SmartPic(
                    R.drawable.smart_butterfly, 46.dp, 62.dp,
                    dim = if (win) 0f else judge, lift = if (win) judge else 0f, blur = b,
                    overlay = {
                        if (win) SmartPill(
                            best, SmartMint, SmartMintInk, judge,
                            modifier = Modifier.align(Alignment.TopCenter).offset(y = (-10).dp)
                        )
                    }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom, modifier = Modifier.height(46.dp)) {
            heights.forEachIndexed { i, h ->
                Box(
                    Modifier
                        .size(46.dp, (h * bars).dp.coerceAtLeast(2.dp))
                        .clip(RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
                        .background(if (i == 2) SmartMint else Color.White.copy(alpha = 0.3f))
                )
            }
        }
    }
}
