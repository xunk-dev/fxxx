package com.frameout.core.pro

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build

internal object DeviceRisk {
    fun blocked(context: Context, highWater: Long, skewMs: Long, wipes: Int): Boolean {
        val debuggable = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        val emulator = !debuggable && TrialRisk.looksLikeEmulator(Build.FINGERPRINT, Build.HARDWARE, Build.MODEL, Build.PRODUCT)
        val rolledBack = System.currentTimeMillis() < highWater - skewMs
        return TrialRisk.blocked(emulator, rolledBack, wipes)
    }
}
