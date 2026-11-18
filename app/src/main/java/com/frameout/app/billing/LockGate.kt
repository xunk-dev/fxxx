package com.frameout.app.billing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.frameout.core.ads.AdStrings
import com.frameout.core.edit.toEdit
import com.frameout.core.pro.ProAccess
import com.frameout.core.pro.ProEditGuard
import com.frameout.core.util.AppContainer
import com.frameout.core.util.EditStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.DialogButton
import com.frameout.designsystem.components.DialogTone
import com.frameout.designsystem.components.FrameDialog
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class LockGate(private val show: (ProEditGuard.LockReport?) -> Unit) {
    private var report: ProEditGuard.LockReport? = null
    private var resume: ((String?) -> Unit)? = null

    fun gate(report: ProEditGuard.LockReport?, run: (String?) -> Unit) {
        if (report == null) {
            run(null)
            return
        }
        this.report = report
        resume = run
        show(report)
    }

    private fun take(): ((String?) -> Unit)? {
        val run = resume
        resume = null
        report = null
        show(null)
        return run
    }

    fun dismiss() {
        take()
    }

    fun removeAndSave() {
        take()?.invoke(EditStrings.lockSavedWithout())
    }

    fun getPro() {
        val current = report ?: return
        take()
        val feature = current.proFeature ?: current.adFeatures.firstOrNull() ?: return
        ProAccess.source?.showPaywall(feature, adEligible = false)
    }

    fun watchAd() {
        val current = report ?: return
        val run = take() ?: return
        val first = current.adFeatures.firstOrNull() ?: return
        val source = ProAccess.source ?: return
        val note = if (current.pro > 0) EditStrings.lockSavedWithout() else null
        source.showPaywall(first, adEligible = true, after = {
            source.shareReward(first, current.adFeatures)
            run(note)
        }, direct = true)
    }
}

internal suspend fun AppContainer.lockReport(frameIds: Collection<Long>): ProEditGuard.LockReport? {
    if (frameIds.isEmpty()) return null
    val edits = withContext(Dispatchers.IO) { frameIds.mapNotNull { editHistory.current(it)?.toEdit() } }
    return ProEditGuard.report(edits)
}

@Composable
internal fun LockPromptDialog(report: ProEditGuard.LockReport, gate: LockGate) {
    val colors = FrameTheme.colors
    FrameDialog(
        onDismiss = gate::dismiss,
        tone = DialogTone.Warning,
        title = EditStrings.lockTitle(),
        body = EditStrings.lockBody(),
        icon = FrameIconsExtra.Warning,
        primary = if (report.pro > 0) DialogButton(AdStrings.rewardPro(), gate::getPro) else DialogButton(AdStrings.rewardWatch(), gate::watchAd),
        secondary = DialogButton(EditStrings.lockRemoveSave(), gate::removeAndSave),
        content = {
            Column(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (report.pro > 0) FrameText(EditStrings.lockProCount(report.pro), style = FrameTheme.typography.label, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
                if (report.ad > 0) FrameText(EditStrings.lockAdCount(report.ad), style = FrameTheme.typography.label, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
                if (report.mixed) FrameText(
                    EditStrings.lockWatchPartial(),
                    style = FrameTheme.typography.label,
                    color = colors.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp).clip(FrameShapes.pill).frameClickable(onClick = gate::watchAd).padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    )
}
