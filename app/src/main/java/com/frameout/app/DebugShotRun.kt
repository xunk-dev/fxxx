package com.frameout.app

import android.content.Context
import android.widget.Toast
import com.frameout.core.util.LocaleHelper
import kotlinx.coroutines.delay

internal object DebugShotRun {
    private const val SETTLE_MS = 1400L
    private var queue = ArrayDeque<String>()
    private var current: String? = null
    private var original = "auto"
    private var folder = ""
    private var saved = 0
    private val captured = HashSet<String>()

    val active: Boolean get() = current != null

    fun start(context: Context, keys: List<String>) {
        if (active || keys.isEmpty()) return
        original = prefsOf(context).language.value
        folder = "langs_" + java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
        queue = ArrayDeque(keys)
        captured.clear()
        saved = 0
        switchTo(context, queue.removeFirst())
    }

    suspend fun step(context: Context) {
        val lang = current ?: return
        delay(SETTLE_MS)
        if (captured.add(lang) && DebugShot.capture(context, folder, "$lang.png").startsWith("Saved")) saved++
        val next = queue.removeFirstOrNull()
        if (next != null) switchTo(context, next) else finish(context)
    }

    private fun prefsOf(context: Context) = (context.applicationContext as FrameOutApp).container.prefs

    private fun switchTo(context: Context, lang: String) {
        current = lang
        apply(context, lang)
    }

    private fun finish(context: Context) {
        val count = saved
        current = null
        apply(context, original)
        Toast.makeText(context, "Saved $count languages in Pictures/FrameOut-Screenshots/$folder", Toast.LENGTH_LONG).show()
    }

    private fun apply(context: Context, lang: String) {
        prefsOf(context).setLanguage(lang)
        LocaleHelper.apply(context, lang)
        DebugShot.activityOf(context)?.recreate()
    }
}
