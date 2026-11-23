package com.frameout.app.billing

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

@Composable
fun SecureWhile(active: Boolean) {
    val window = LocalContext.current.findActivity()?.window
    DisposableEffect(window, active) {
        val alreadySecure = window != null && (window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE) != 0
        if (active && window != null && !alreadySecure) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            if (active && window != null && !alreadySecure) window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
