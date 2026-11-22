package com.frameout.app.trim

import android.net.Uri
import com.frameout.app.billing.LockGate
import com.frameout.core.pro.ProAccess
import com.frameout.core.util.EditStrings
import kotlinx.coroutines.CoroutineScope

internal class TrimFlow(
    private val ui: TrimUiState,
    private val scope: CoroutineScope,
    private val savers: OutputSavers,
    private val lockGate: LockGate
) {
    fun save(source: Uri, durationMs: Long, positionNow: () -> Long, onSaved: () -> Unit = {}) {
        val lock = TrimLock.check(ui.output) { feature, tier -> ProAccess.allows(feature, tier) }
        val start = { note: String? ->
            val range = ui.range.clamped(durationMs).let { it.copy(useFull = it.startMs <= 0L && it.endMs >= durationMs) }
            startOutputSave(ui, scope, savers, source, range, positionNow(), durationMs, note, onSaved)
        }
        if (lock == null) {
            start(null)
            return
        }
        lockGate.gateOutput(
            lock.toReport(),
            remove = {
                ui.selectOutput(lock.fallback, positionNow())
                EditStrings.exportLockSwitched(outputLabel(lock.output))
            },
            run = start
        )
    }
}
