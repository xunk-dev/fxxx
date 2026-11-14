package com.frameout.app.viewer

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.icons.FrameIcons
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

@Composable
internal fun HideStatusBarWhile(hide: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(hide) {
        val window = view.context.findHostActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val previous = controller?.systemBarsBehavior
        if (hide && controller != null) {
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.statusBars())
        }
        onDispose {
            if (hide && controller != null) {
                controller.show(WindowInsetsCompat.Type.statusBars())
                if (previous != null) controller.systemBarsBehavior = previous
            }
        }
    }
}

private tailrec fun Context.findHostActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findHostActivity()
    else -> null
}

@Composable
internal fun FullscreenBack(rotated: Boolean, onBack: () -> Unit) {
    val label = FrameStrings.back()
    Box(Modifier.fillMaxSize().landscapeRotated(rotated)) {
        Box(
            Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f))
                .frameClickable(onClickLabel = label, onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Image(painterResource(FrameIcons.Back), label, Modifier.size(24.dp), colorFilter = ColorFilter.tint(Color.White))
        }
    }
}
