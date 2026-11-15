package com.frameout.app.ads

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.frameout.core.ads.DnsVerdict
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object AdBlockProbe {
    private val adHosts = listOf("googleads.g.doubleclick.net", "pagead2.googlesyndication.com")
    private const val CONTROL_HOST = "www.google.com"
    private const val LOOKUP_TIMEOUT_MS = 2_500L
    private const val RECHECK_DELAY_MS = 1_500L

    private val pool = Executors.newCachedThreadPool { task -> Thread(task, "ad-probe").apply { isDaemon = true } }

    private class Lookup(val addresses: List<InetAddress>?, val timedOut: Boolean = false)

    fun online(context: Context): Boolean {
        val caps = activeCaps(context) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    fun vpnActive(context: Context): Boolean {
        val caps = activeCaps(context) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) ||
            !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
    }

    fun dnsVerdict(): DnsVerdict {
        val first = probe()
        if (first != DnsVerdict.Unresolved) return first
        Thread.sleep(RECHECK_DELAY_MS)
        return probe()
    }

    private fun probe(): DnsVerdict {
        if (lookup(CONTROL_HOST).addresses == null) return DnsVerdict.Inconclusive
        val results = adHosts.map { lookup(it) }
        if (results.any { it.timedOut }) return DnsVerdict.Inconclusive
        val blocked = results.all { r ->
            val found = r.addresses
            found == null || found.all(::isSink)
        }
        return when {
            !blocked -> DnsVerdict.Clean
            results.any { it.addresses != null } -> DnsVerdict.Sinkhole
            else -> DnsVerdict.Unresolved
        }
    }

    private fun lookup(host: String): Lookup {
        val task = pool.submit<Lookup> {
            try {
                Lookup(InetAddress.getAllByName(host).toList())
            } catch (e: UnknownHostException) {
                Lookup(null)
            } catch (e: SecurityException) {
                Lookup(null)
            }
        }
        return try {
            task.get(LOOKUP_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        } catch (e: Exception) {
            task.cancel(true)
            Lookup(null, timedOut = true)
        }
    }

    private fun activeCaps(context: Context): NetworkCapabilities? {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return null
        return cm.getNetworkCapabilities(cm.activeNetwork ?: return null)
    }

    private fun isSink(address: InetAddress): Boolean =
        address.isAnyLocalAddress || address.isLoopbackAddress
}
