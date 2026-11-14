package com.frameout.app

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.frameout.app.settings.appLanguageKeys
import com.frameout.designsystem.icons.FrameIconsExtra
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val HIDE_DELAY_MS = 160L

@Composable
fun DebugShotHost() {
    if (!BuildConfig.DEBUG) return
    val context = LocalContext.current
    val prefs = (context.applicationContext as FrameOutApp).container.prefs
    val enabled by prefs.debugShotButton.collectAsState()
    val allLanguages by prefs.debugShotLanguages.collectAsState()
    if (DebugShotRun.active) {
        LaunchedEffect(Unit) { DebugShotRun.step(context) }
        return
    }
    if (!enabled) return
    val scope = rememberCoroutineScope()
    ShotLayer { capturing -> shoot(context, scope, allLanguages, capturing) }
}

private fun shoot(context: Context, scope: CoroutineScope, allLanguages: Boolean, capturing: (Boolean) -> Unit) {
    if (allLanguages) {
        DebugShotRun.start(context, appLanguageKeys())
        return
    }
    scope.launch {
        capturing(true)
        try {
            delay(HIDE_DELAY_MS)
            Toast.makeText(context, DebugShot.capture(context), Toast.LENGTH_SHORT).show()
        } finally {
            capturing(false)
        }
    }
}

@Composable
private fun ShotLayer(onTap: ((Boolean) -> Unit) -> Unit) {
    var capturing by remember { mutableStateOf(false) }
    var area by remember { mutableStateOf(IntSize.Zero) }
    val x = remember { mutableFloatStateOf(Float.NaN) }
    val y = remember { mutableFloatStateOf(Float.NaN) }
    val sizePx = with(LocalDensity.current) { 52.dp.toPx() }
    Box(Modifier.fillMaxSize().onSizeChanged {
        area = it
        if (x.floatValue.isNaN() && it.width > 0) { x.floatValue = it.width - sizePx - 12f; y.floatValue = it.height * 0.62f }
    }) {
        if (!capturing && !x.floatValue.isNaN()) {
            ShotButton(x.floatValue, y.floatValue, onDrag = { dx, dy ->
                x.floatValue = (x.floatValue + dx).coerceIn(0f, (area.width - sizePx).coerceAtLeast(0f))
                y.floatValue = (y.floatValue + dy).coerceIn(0f, (area.height - sizePx).coerceAtLeast(0f))
            }) { onTap { capturing = it } }
        }
    }
}

@Composable
private fun ShotButton(x: Float, y: Float, onDrag: (Float, Float) -> Unit, onTap: () -> Unit) {
    Box(
        Modifier
            .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
            .size(52.dp)
            .background(Color(0xCC111111), CircleShape)
            .border(1.5.dp, Color.White.copy(alpha = 0.7f), CircleShape)
            .pointerInput(Unit) { detectDragGestures { change, drag -> change.consume(); onDrag(drag.x, drag.y) } }
            .pointerInput(Unit) { detectTapGestures { onTap() } },
        contentAlignment = Alignment.Center
    ) {
        Image(painterResource(FrameIconsExtra.Snapshot), "Screenshot", Modifier.size(24.dp), colorFilter = ColorFilter.tint(Color.White))
    }
}
