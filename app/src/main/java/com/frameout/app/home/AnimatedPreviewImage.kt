package com.frameout.app.home

import android.content.Context
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.os.Build
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import com.frameout.core.util.logFailure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

@RequiresApi(Build.VERSION_CODES.P)
@Composable
internal fun AnimatedPreviewImage(resId: Int, play: Boolean, onFailed: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val density = LocalDensity.current
    BoxWithConstraints(modifier) {
        val widthPx = with(density) { maxWidth.roundToPx() }
        val heightPx = with(density) { maxHeight.roundToPx() }
        val decoded by produceState<Result<Drawable>?>(null, resId, widthPx, heightPx) {
            value = withContext(Dispatchers.IO) { runCatching { decode(context, resId, widthPx, heightPx) } }
        }
        val result = decoded
        val drawable = result?.getOrNull()
        LaunchedEffect(result) {
            result?.exceptionOrNull()?.let {
                logFailure("ExportPreview", it)
                onFailed()
            }
        }
        DisposableEffect(drawable) {
            onDispose { (drawable as? Animatable)?.stop() }
        }
        if (drawable != null) {
            AndroidView(
                factory = { ImageView(it).apply { scaleType = ImageView.ScaleType.CENTER_CROP } },
                update = { view ->
                    if (view.drawable !== drawable) view.setImageDrawable(drawable)
                    val animatable = drawable as? Animatable
                    if (play) {
                        if (animatable?.isRunning == false) animatable.start()
                    } else if (animatable?.isRunning == true) {
                        animatable.stop()
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.P)
private fun decode(context: Context, resId: Int, widthPx: Int, heightPx: Int): Drawable {
    val source = ImageDecoder.createSource(context.resources, resId)
    val drawable = ImageDecoder.decodeDrawable(source) { decoder, info, _ ->
        val w = info.size.width
        val h = info.size.height
        if (widthPx > 0 && heightPx > 0 && w > 0 && h > 0) {
            val scale = max(widthPx.toFloat() / w, heightPx.toFloat() / h)
            if (scale < 1f) decoder.setTargetSize((w * scale).roundToInt().coerceAtLeast(1), (h * scale).roundToInt().coerceAtLeast(1))
        }
    }
    (drawable as? AnimatedImageDrawable)?.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
    return drawable
}
