package com.frameout.app.billing

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

internal fun isOnline(context: Context): Boolean = runCatching {
    val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val caps = manager.getNetworkCapabilities(manager.activeNetwork) ?: return@runCatching false
    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}.getOrDefault(false)
