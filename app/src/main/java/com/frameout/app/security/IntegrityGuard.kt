package com.frameout.app.security

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Debug
import com.frameout.app.BuildConfig
import java.io.File
import java.security.MessageDigest

enum class Verdict { Clean, Hooked, Tampered }

enum class RootVerdict { Clean, Detected, Unknown }

object IntegrityGuard {
    fun rootStatus(context: Context): RootVerdict {
        val native = NativeGuard.root()
        if (native == NativeGuard.ROOT_DETECTED || kotlinRootSignals(context)) return RootVerdict.Detected
        if (native == NativeGuard.UNAVAILABLE) return RootVerdict.Unknown
        return RootVerdict.Clean
    }

    private fun kotlinRootSignals(context: Context): Boolean =
        Build.TAGS?.contains("test-keys") == true || knownRootPackageInstalled(context)

    private fun knownRootPackageInstalled(context: Context): Boolean {
        val pm = context.packageManager
        return ROOT_PACKAGES.any { pkg -> runCatching { pm.getPackageInfo(pkg, 0) }.isSuccess }
    }

    private val ROOT_PACKAGES = listOf(
        "com.topjohnwu.magisk", "eu.chainfire.supersu", "com.noshufou.android.su",
        "com.koushikdutta.superuser", "com.thirdparty.superuser", "com.yellowes.su",
        "com.kingroot.kinguser", "com.kingouser.com", "me.weishu.kernelsu",
    )

    fun inspect(context: Context): Verdict {
        if (BuildConfig.DEBUG) return Verdict.Clean
        val native = nativeVerdict(context)
        if (debuggableFlag(context) || signerMismatch(context) || native == NativeGuard.MISMATCH) return Verdict.Tampered
        if (activeInstrumentation() || xposedPresent() || native == NativeGuard.UNAVAILABLE) return Verdict.Hooked
        return Verdict.Clean
    }

    private fun nativeVerdict(context: Context): Int =
        if (BuildConfig.CERT_SHA256.isEmpty()) NativeGuard.UNCHECKED else NativeGuard.verdict(context.applicationInfo.sourceDir)

    private fun debuggableFlag(context: Context): Boolean =
        context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

    private fun signerMismatch(context: Context): Boolean {
        val expected = BuildConfig.CERT_SHA256
        if (expected.isEmpty()) return !BuildConfig.ALLOW_UNSIGNED
        val fromFile = ApkSigningBlock.signerDigests(File(context.applicationInfo.sourceDir))
        val fromManager = runCatching { managerDigests(context) }.getOrNull().orEmpty()
        if (fromFile.isEmpty() || fromManager.isEmpty()) return true
        return expected !in fromFile || expected !in fromManager || fromFile != fromManager
    }

    @Suppress("DEPRECATION")
    private fun managerDigests(context: Context): Set<String> {
        val pm = context.packageManager
        val signatures = if (Build.VERSION.SDK_INT >= 28) {
            pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES).signingInfo?.apkContentsSigners
        } else {
            pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES).signatures
        }
        return signatures.orEmpty().map { hex(MessageDigest.getInstance("SHA-256").digest(it.toByteArray())) }.toSet()
    }

    private fun activeInstrumentation(): Boolean =
        Debug.isDebuggerConnected() || tracerAttached() || strongHookMapped()

    private fun tracerAttached(): Boolean = nativeTracerAttached() || kotlinTracerAttached()

    private fun nativeTracerAttached(): Boolean = NativeGuard.tracer() == NativeGuard.TRACER_ATTACHED

    private fun kotlinTracerAttached(): Boolean = runCatching {
        File("/proc/self/status").useLines { lines ->
            lines.firstOrNull { it.startsWith(Mask.tracer) }?.substringAfter(':')?.trim()?.let { it != "0" } ?: false
        }
    }.getOrDefault(false)

    private fun strongHookMapped(): Boolean = fridaPortOpen() || mapsMentionHookFramework()

    private fun fridaPortOpen(): Boolean = NativeGuard.fridaPort() == NativeGuard.FRIDA_PORT_OPEN

    private fun mapsMentionHookFramework(): Boolean = runCatching {
        val markers = listOf(Mask.frida, Mask.gum, Mask.substrate)
        File("/proc/self/maps").useLines { lines ->
            lines.any { line -> markers.any { line.contains(it, ignoreCase = true) } }
        }
    }.getOrDefault(false)

    private fun xposedPresent(): Boolean =
        runCatching { Class.forName(Mask.bridge); true }.getOrDefault(false) ||
            Thread.currentThread().stackTrace.any { it.className.contains(Mask.xposed, ignoreCase = true) } ||
            mapsContain(Mask.lspd) || mapsContain(Mask.xposed)

    private fun mapsContain(marker: String): Boolean = runCatching {
        File("/proc/self/maps").useLines { lines -> lines.any { it.contains(marker, ignoreCase = true) } }
    }.getOrDefault(false)

    private fun hex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it) }
}
