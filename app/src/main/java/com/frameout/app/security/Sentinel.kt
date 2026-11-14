package com.frameout.app.security

import android.content.Context
import com.frameout.core.pro.Entitlements
import com.frameout.core.util.DiagLog
import com.frameout.core.util.RuntimeGate
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object Sentinel {
    private const val RECHECK_MS = 45_000L
    @Volatile private var verdict = Verdict.Clean
    @Volatile private var checkedAt = 0L

    fun install(context: Context, entitlements: Entitlements, scope: CoroutineScope) {
        val app = context.applicationContext
        RuntimeGate.probe = { verdict(app) == Verdict.Clean }
        scope.launch(Dispatchers.IO) {
            delay(Random.nextLong(4_000L, 20_000L))
            val root = IntegrityGuard.rootStatus(app)
            if (root != RootVerdict.Clean) DiagLog.append("integrity", "root=$root")
            while (isActive) {
                val v = verdict(app)
                entitlements.applyIntegrity(tampered = v == Verdict.Tampered, hooked = v == Verdict.Hooked)
                delay(Random.nextLong(40_000L, 120_000L))
            }
        }
    }

    fun verdict(context: Context): Verdict {
        val now = System.currentTimeMillis()
        if (checkedAt != 0L && now - checkedAt < RECHECK_MS) return verdict
        val fresh = IntegrityGuard.inspect(context)
        if (fresh != Verdict.Clean && fresh != verdict) DiagLog.append("integrity", "verdict=$fresh")
        verdict = fresh
        checkedAt = now
        return fresh
    }
}
