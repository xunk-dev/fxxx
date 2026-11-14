package com.frameout.app.security

internal object NativeGuard {
    const val OK = 0
    const val MISMATCH = 1
    const val ERROR = 2
    const val UNCHECKED = 3
    const val UNAVAILABLE = -1

    const val TRACER_NONE = 0
    const val TRACER_ATTACHED = 1
    const val TRACER_UNKNOWN = 2

    const val ROOT_CLEAN = 0
    const val ROOT_DETECTED = 1

    const val FRIDA_PORT_CLOSED = 0
    const val FRIDA_PORT_OPEN = 1
    const val FRIDA_PORT_UNKNOWN = 2

    private val loaded: Boolean = runCatching { System.loadLibrary("frameout_guard") }.isSuccess

    @JvmStatic
    private external fun check(apkPath: String): Int

    @JvmStatic
    private external fun signerFingerprint(apkPath: String): ByteArray?

    @JvmStatic
    private external fun tracerCheck(): Int

    @JvmStatic
    private external fun rootCheck(): Int

    @JvmStatic
    private external fun fridaPortCheck(): Int

    fun verdict(apkPath: String): Int =
        if (!loaded) UNAVAILABLE else runCatching { check(apkPath) }.getOrDefault(UNAVAILABLE)

    fun tracer(): Int =
        if (!loaded) UNAVAILABLE else runCatching { tracerCheck() }.getOrDefault(UNAVAILABLE)

    fun root(): Int =
        if (!loaded) UNAVAILABLE else runCatching { rootCheck() }.getOrDefault(UNAVAILABLE)

    fun fridaPort(): Int =
        if (!loaded) UNAVAILABLE else runCatching { fridaPortCheck() }.getOrDefault(UNAVAILABLE)

    fun fingerprint(apkPath: String): ByteArray? {
        if (!loaded) return null
        val bytes = runCatching { signerFingerprint(apkPath) }.getOrNull() ?: return null
        if (bytes.size == PayloadCrypto.FINGERPRINT_BYTES) return bytes
        bytes.fill(0)
        return null
    }
}
