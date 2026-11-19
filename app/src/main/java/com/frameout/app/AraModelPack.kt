package com.frameout.app

import android.content.Context
import com.google.android.play.core.assetpacks.AssetPackManagerFactory
import com.google.android.play.core.assetpacks.AssetPackState
import com.google.android.play.core.assetpacks.AssetPackStateUpdateListener
import com.google.android.play.core.assetpacks.model.AssetPackStatus
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PackPhase { Idle, Downloading, Ready, Failed }

object AraModelPack {
    private const val PACK = "ara_model"
    private const val FILE = "tessdata/ara.traineddata"
    private val phase = MutableStateFlow(PackPhase.Idle)
    private val percent = MutableStateFlow(0)
    private var started = false
    val state: StateFlow<PackPhase> = phase.asStateFlow()
    val progress: StateFlow<Int> = percent.asStateFlow()

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
        if (root(app) != null) {
            phase.value = PackPhase.Ready
            return
        }
        val manager = AssetPackManagerFactory.getInstance(app)
        manager.registerListener(AssetPackStateUpdateListener { update -> onUpdate(update) })
        phase.value = PackPhase.Downloading
        runCatching { manager.fetch(listOf(PACK)) }.onFailure { phase.value = PackPhase.Failed }
    }

    fun retry(context: Context) {
        started = false
        start(context)
    }

    private fun onUpdate(update: AssetPackState) {
        if (update.name() != PACK) return
        when (update.status()) {
            AssetPackStatus.COMPLETED -> { percent.value = 100; phase.value = PackPhase.Ready }
            AssetPackStatus.FAILED, AssetPackStatus.CANCELED, AssetPackStatus.NOT_INSTALLED -> phase.value = PackPhase.Failed
            else -> {
                val total = update.totalBytesToDownload()
                if (total > 0L) percent.value = (update.bytesDownloaded() * 100L / total).toInt().coerceIn(0, 99)
                phase.value = PackPhase.Downloading
            }
        }
    }
}
