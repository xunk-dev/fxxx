package com.frameout.app

import android.app.Activity
import android.content.Context
import com.google.android.play.core.assetpacks.AssetPackException
import com.google.android.play.core.assetpacks.AssetPackManager
import com.google.android.play.core.assetpacks.AssetPackManagerFactory
import com.google.android.play.core.assetpacks.AssetPackState
import com.google.android.play.core.assetpacks.AssetPackStateUpdateListener
import com.google.android.play.core.assetpacks.model.AssetPackErrorCode
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AraModelPack {
    private const val PACK = "ara_model"
    private const val FILE = "tessdata/ara.traineddata"
    private val current = MutableStateFlow<PackState>(PackState.Idle)
    private val listener = AssetPackStateUpdateListener { update -> onUpdate(update) }
    private var started = false
    private var listening = false
    val state: StateFlow<PackState> = current.asStateFlow()

    fun root(context: Context): File? {
        val location = runCatching { AssetPackManagerFactory.getInstance(context).getPackLocation(PACK) }.getOrNull() ?: return null
        val dir = location.assetsPath()?.let(::File) ?: return null
        return dir.takeIf { File(it, FILE).length() > 0L }
    }

    fun start(context: Context) {
        val app = context.applicationContext
        com.frameout.core.quality.ArabicOcr.packRoot = { root(app) }
        if (started) return
        started = true
        fetch(app)
    }

    fun retry(context: Context) {
        if (PackStates.action(current.value) != PackAction.Retry) return
        fetch(context.applicationContext)
    }

    fun confirm(activity: Activity) {
        if (current.value != PackState.NeedsConfirmation) return
        val manager = manager(activity.applicationContext) ?: return
        runCatching { manager.showConfirmationDialog(activity) }
    }

    private fun fetch(app: Context) {
        if (root(app) != null) {
            current.value = PackState.Ready
            return
        }
        val manager = manager(app) ?: return
        if (!listening) {
            listening = true
            manager.registerListener(listener)
        }
        current.value = PackState.Downloading(0)
        runCatching { manager.fetch(listOf(PACK)) }
            .onSuccess { task ->
                task.addOnSuccessListener { states -> states.packStates()[PACK]?.let { onUpdate(it) } }
                task.addOnFailureListener { e -> current.value = PackStates.failure((e as? AssetPackException)?.errorCode ?: AssetPackErrorCode.INTERNAL_ERROR) }
            }
            .onFailure { current.value = PackState.Unavailable }
    }

    private fun manager(app: Context): AssetPackManager? =
        runCatching { AssetPackManagerFactory.getInstance(app) }.getOrNull().also { if (it == null) current.value = PackState.Unavailable }

    private fun onUpdate(update: AssetPackState) {
        if (update.name() != PACK) return
        current.value = PackStates.fromStatus(update.status(), update.errorCode(), update.bytesDownloaded(), update.totalBytesToDownload())
    }
}
