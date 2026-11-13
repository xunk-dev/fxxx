package com.frameout.app.billing

import android.content.Context
import com.frameout.app.security.IntegrityGuard
import com.frameout.app.security.Verdict

object ExportIntegrityGate {
    fun verifiedClean(context: Context): Boolean = IntegrityGuard.inspect(context) == Verdict.Clean
}
