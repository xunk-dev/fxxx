package com.frameout.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.frameout.core.prefs.SessionPrefs

object NotifAsk {
    var pending by mutableStateOf(false)

    fun shouldAsk(context: Context, prefs: SessionPrefs): Boolean {
        if (Build.VERSION.SDK_INT < 33) return false
        if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return false
        return !prefs.notificationAsked()
    }

    fun afterSuccessfulExtraction(context: Context, prefs: SessionPrefs) {
        if (shouldAsk(context, prefs)) pending = true
    }
}
