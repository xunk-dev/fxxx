package com.frameout.app.collage

import android.graphics.Bitmap
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.frameout.core.collage.CellCrop
import com.frameout.core.collage.CropGeometry

private class CropHolder {
    var crop = CellCrop()
    var bitmap: Bitmap? = null
    var cellWidth = 1f
    var cellHeight = 1f
}

@Composable
fun Modifier.cropGestures(bitmap: Bitmap?, cellWidth: Float, cellHeight: Float, crop: CellCrop, onCrop: (CellCrop) -> Unit): Modifier {
    val holder = remember { CropHolder() }
    holder.crop = crop
    holder.bitmap = bitmap
    holder.cellWidth = cellWidth
    holder.cellHeight = cellHeight
    val onChange by rememberUpdatedState(onCrop)
    if (bitmap == null) return this
    return pointerInput(Unit) {
        detectTransformGestures { centroid, pan, zoom, _ ->
            val current = holder.bitmap ?: return@detectTransformGestures
            val geometry = CropGeometry(current.width.toFloat(), current.height.toFloat(), holder.cellWidth, holder.cellHeight)
            val next = geometry.transform(holder.crop, pan.x, pan.y, zoom, centroid.x, centroid.y)
            holder.crop = next
            onChange(next)
        }
    }
}
