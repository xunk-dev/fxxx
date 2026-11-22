package com.frameout.designsystem.dock

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.motion.frameSpring
import com.frameout.designsystem.theme.FrameTheme

@Composable
internal fun FabContent(icon: Int, contentDescription: String, percent: String?, reduceMotion: Boolean, content: Color) {
    val iconSize by animateDpAsState(if (percent != null) 22.dp else 26.dp, frameSpring(), label = "chargeIconSize")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(iconSize), contentAlignment = Alignment.Center) {
            ChargeIcon(icon, contentDescription, reduceMotion, content)
        }
        if (percent != null) {
            FrameText(
                percent,
                style = FrameTheme.typography.monoSmall.copy(
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    shadow = Shadow(Color.Black.copy(alpha = if (content.luminance() > 0.5f) 0.4f else 0f), blurRadius = 3f)
                ),
                color = content
            )
        }
    }
}

@Composable
private fun ChargeIcon(icon: Int, contentDescription: String, reduceMotion: Boolean, content: Color) {
    AnimatedContent(
        targetState = icon,
        transitionSpec = { chargeTransition(reduceMotion) },
        contentAlignment = Alignment.Center,
        label = "chargeIcon"
    ) { current ->
        Image(
            painter = painterResource(current),
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            colorFilter = ColorFilter.tint(content)
        )
    }
}

private fun chargeTransition(reduceMotion: Boolean): ContentTransform =
    if (reduceMotion) {
        ContentTransform(EnterTransition.None, ExitTransition.None, 0f, SizeTransform(clip = false))
    } else {
        ContentTransform(
            fadeIn(tween(220, delayMillis = 60)) + scaleIn(tween(260), initialScale = 0.55f),
            fadeOut(tween(120)) + scaleOut(tween(160), targetScale = 0.55f),
            0f,
            SizeTransform(clip = false)
        )
    }
