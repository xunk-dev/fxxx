package com.frameout.app.update

import android.app.Activity
import android.content.Context
import android.text.format.Formatter
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.frameout.app.rating.StoreLauncher
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability

class UpdateController(
    private val activity: Activity,
    private val launcher: ActivityResultLauncher<IntentSenderRequest>,
    private val blocked: () -> Boolean
) {
    private val manager: AppUpdateManager = AppUpdateManagerFactory.create(activity.applicationContext)
    private val prefs = activity.getSharedPreferences("update_prompt", Context.MODE_PRIVATE)
    private var latest: AppUpdateInfo? = null
    private var checking = false
    private var launching = false
    private var quietUntil = 0L

    var info by mutableStateOf<UpdateInfo?>(null)
        private set
    var visible by mutableStateOf(false)
        private set

    fun check() {
        if (checking || launching || System.currentTimeMillis() < quietUntil) return
        checking = true
        val task = manager.appUpdateInfo
        task.addOnSuccessListener { handle(it) }
        task.addOnCompleteListener { checking = false }
    }

    private fun handle(found: AppUpdateInfo) {
        latest = found
        when (found.updateAvailability()) {
            UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS ->
                if (!snoozed(found.availableVersionCode())) launch(found, resume = true)
            UpdateAvailability.UPDATE_AVAILABLE -> offer(found)
            else -> visible = false
        }
    }

    private fun offer(found: AppUpdateInfo) {
        val code = found.availableVersionCode()
        if (visible || blocked() || snoozed(code)) return
        info = UpdateInfo(sizeLabel(found))
        visible = true
    }

    fun later() {
        visible = false
        snooze(latest?.availableVersionCode())
    }

    fun updateNow() {
        visible = false
        val found = latest
        if (found == null) StoreLauncher.openStore(activity) else launch(found, resume = false)
    }

    fun onFlowResult(resultCode: Int) {
        launching = false
        when (resultCode) {
            Activity.RESULT_OK -> quietUntil = System.currentTimeMillis() + QUIET_AFTER_OK_MS
            Activity.RESULT_CANCELED -> snooze(latest?.availableVersionCode())
            else -> StoreLauncher.openStore(activity)
        }
    }

    private fun launch(found: AppUpdateInfo, resume: Boolean) {
        if (!resume && !found.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
            StoreLauncher.openStore(activity)
            return
        }
        launching = true
        val started = runCatching {
            manager.startUpdateFlowForResult(found, launcher, AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build())
        }.getOrDefault(false)
        if (!started) fail()
    }

    private fun fail() {
        launching = false
        StoreLauncher.openStore(activity)
    }

    private fun sizeLabel(found: AppUpdateInfo): String {
        val bytes = found.totalBytesToDownload()
        return if (bytes > 0L) Formatter.formatShortFileSize(activity, bytes) else "Google Play"
    }

    private fun snoozed(code: Int): Boolean =
        prefs.getInt(KEY_CODE, -1) == code && System.currentTimeMillis() - prefs.getLong(KEY_AT, 0L) < SNOOZE_MS

    private fun snooze(code: Int?) {
        if (code == null) return
        prefs.edit().putInt(KEY_CODE, code).putLong(KEY_AT, System.currentTimeMillis()).apply()
    }

    private companion object {
        const val KEY_CODE = "snooze_code"
        const val KEY_AT = "snooze_at"
        const val SNOOZE_MS = 24L * 60L * 60L * 1000L
        const val QUIET_AFTER_OK_MS = 60_000L
    }
}
