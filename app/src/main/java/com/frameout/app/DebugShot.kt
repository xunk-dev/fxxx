package com.frameout.app

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.PixelCopy
import android.view.Window
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

internal object DebugShot {
    private const val FOLDER = "FrameOut-Screenshots"

    suspend fun capture(context: Context, folder: String? = null, name: String? = null): String {
        val window = activityOf(context)?.window ?: return "Screenshot failed: no window"
        val decor = window.decorView
        if (decor.width == 0 || decor.height == 0) return "Screenshot failed: empty window"
        val bitmap = Bitmap.createBitmap(decor.width, decor.height, Bitmap.Config.ARGB_8888)
        return try {
            if (!copy(window, bitmap)) "Screenshot failed" else withContext(Dispatchers.IO) { save(context, bitmap, folder, name ?: fileName()) }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            "Screenshot failed: ${e.message}"
        } finally {
            bitmap.recycle()
        }
    }

    internal fun activityOf(context: Context): Activity? {
        var c: Context? = context
        while (c is ContextWrapper) {
            if (c is Activity) return c
            c = c.baseContext
        }
        return null
    }

    private suspend fun copy(window: Window, bitmap: Bitmap): Boolean {
        if (Build.VERSION.SDK_INT < 26) {
            window.decorView.draw(Canvas(bitmap))
            return true
        }
        return suspendCancellableCoroutine { cont ->
            PixelCopy.request(window, bitmap, { code -> if (cont.isActive) cont.resume(code == PixelCopy.SUCCESS) }, Handler(Looper.getMainLooper()))
        }
    }

    private fun fileName(): String =
        "FrameOut_" + java.text.SimpleDateFormat("yyyyMMdd_HHmmss_SSS", java.util.Locale.US).format(java.util.Date()) + ".png"

    private fun save(context: Context, bitmap: Bitmap, folder: String?, name: String): String {
        val sub = if (folder == null) FOLDER else "$FOLDER/$folder"
        if (Build.VERSION.SDK_INT >= 29) return saveToGallery(context, bitmap, sub, name)
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), sub).apply { mkdirs() }
        File(dir, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return "Saved: ${dir.absolutePath}/$name"
    }

    private fun saveToGallery(context: Context, bitmap: Bitmap, sub: String, name: String): String {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/" + sub)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: error("MediaStore insert failed")
        try {
            resolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } ?: error("cannot open output")
            resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
        return "Saved: Pictures/$sub/$name"
    }
}
