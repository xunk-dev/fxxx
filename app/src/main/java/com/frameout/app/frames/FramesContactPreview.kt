package com.frameout.app.frames

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.frameout.core.util.ThumbCache
import com.frameout.core.util.logFailure
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

private const val THUMB_LIMIT = 96
private const val THUMB_SIDE = 128
private const val BATCH = 12
private const val DEFAULT_ASPECT = 16f / 9f

@Composable
internal fun rememberFramesContactPreview(vm: FramesViewModel, open: Boolean, selected: Set<Long>, total: Int): ContactPreview {
    var aspect by remember { mutableFloatStateOf(DEFAULT_ASPECT) }
    var thumbs by remember { mutableStateOf<List<ImageBitmap?>>(emptyList()) }
    val shown = minOf(total, THUMB_LIMIT)
    LaunchedEffect(open, selected, shown) {
        if (!open) return@LaunchedEffect
        try {
            val frames = vm.contactPreviewFrames(THUMB_LIMIT)
            if (frames.isEmpty()) {
                thumbs = emptyList()
                return@LaunchedEffect
            }
            val loaded = arrayOfNulls<ImageBitmap>(frames.size)
            thumbs = loaded.toList()
            var probed = false
            frames.indices.chunked(BATCH).forEach { chunk ->
                coroutineScope {
                    chunk.map { i ->
                        async {
                            val path = frames[i].thumbnailPath ?: frames[i].fullPath
                            loaded[i] = ThumbCache.load(path, THUMB_SIDE)?.asImageBitmap()
                        }
                    }.awaitAll()
                }
                if (!probed) {
                    loaded.firstOrNull { it != null }?.let {
                        if (it.height > 0) aspect = it.width.toFloat() / it.height
                        probed = true
                    }
                }
                thumbs = loaded.toList()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logFailure("FramesContactPreview", e)
        }
    }
    return ContactPreview(aspect, if (open) thumbs else emptyList(), null)
}
