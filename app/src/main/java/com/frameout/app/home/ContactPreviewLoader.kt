package com.frameout.app.home

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.frameout.app.frames.ContactPreview
import com.frameout.app.library.SavedMediaDecoder
import com.frameout.core.util.logFailure
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val THUMB_LIMIT = 96
private const val THUMB_SIDE = 128
private const val THUMB_NAME = "preview.jpg"
private const val DEFAULT_ASPECT = 16f / 9f

@Composable
internal fun rememberContactPreview(uris: List<Uri>?, onAdd: () -> Unit): ContactPreview {
    val context = LocalContext.current.applicationContext
    val decoder = remember { SavedMediaDecoder(context) }
    val cache = remember { mutableStateMapOf<Uri, ImageBitmap>() }
    var aspect by remember { mutableFloatStateOf(DEFAULT_ASPECT) }
    val list = uris.orEmpty()
    LaunchedEffect(list) {
        if (list.isEmpty()) cache.clear()
        aspect = withContext(Dispatchers.IO) { probeAspect(decoder, list.firstOrNull()) }
        list.take(THUMB_LIMIT).filter { it !in cache }.forEach { uri -> loadThumb(decoder, uri)?.let { cache[uri] = it } }
    }
    return ContactPreview(aspect, list.take(THUMB_LIMIT).map { cache[it] }, onAdd)
}

private fun probeAspect(decoder: SavedMediaDecoder, uri: Uri?): Float {
    val size = uri?.let { runCatching { decoder.probe(it) }.getOrNull() } ?: return DEFAULT_ASPECT
    return if (size.second > 0) size.first.toFloat() / size.second else DEFAULT_ASPECT
}

private suspend fun loadThumb(decoder: SavedMediaDecoder, uri: Uri): ImageBitmap? = withContext(Dispatchers.IO) {
    try {
        decoder.decode(uri, THUMB_NAME, THUMB_SIDE)?.asImageBitmap()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        logFailure("ContactPreview", e)
        null
    }
}
