package com.frameout.engine.player

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display

object HdrDisplayPolicy {
    const val MIN_COVERAGE = 0.6f

    const val CAPPED_HEADROOM = 2f
    private const val HEADROOM_MIN_SDK = 35

    fun useNativeSurface(sdkInt: Int, displaySupportsHdr: Boolean, coverage: Float): Boolean =
        sdkInt >= 26 && displaySupportsHdr && coverage >= MIN_COVERAGE

    fun coverage(playerPx: Long, windowPx: Long): Float =
        if (playerPx <= 0L || windowPx <= 0L) 0f else playerPx.toFloat() / windowPx.toFloat()

    fun headroomCap(sdkInt: Int): Float? = if (sdkInt >= HEADROOM_MIN_SDK) CAPPED_HEADROOM else null
}

object HdrDisplay {
    fun supported(context: Context): Boolean = runCatching {
        if (Build.VERSION.SDK_INT < 26) return false
        val manager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager ?: return false
        manager.getDisplay(Display.DEFAULT_DISPLAY)?.isHdr == true
    }.getOrDefault(false)
}
