package com.frameout.core.quality

import android.content.Context
import com.frameout.core.util.DetectorHealth
import com.frameout.core.util.DiagLog
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.mlkit.common.MlKitException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate

object TextModelHealth {
    private const val TAG = "text-model"
    private val current = MutableStateFlow(TextModelState.Unknown)
    val state: StateFlow<TextModelState> = current.asStateFlow()

    suspend fun check(context: Context) {
        val found = try {
            probe(context.applicationContext)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            DetectorHealth.failed(TAG, e)
            classify(e)
        }
        publish(found)
    }

    fun noteSuccess() = publish(TextModelState.Ready)

    fun noteFailure(error: Throwable) {
        val code = (error as? MlKitException)?.errorCode ?: return
        val seen = TextModelRules.fromMlKitCode(code) ?: return
        if (!current.value.problem) publish(seen)
    }

    private suspend fun probe(context: Context): TextModelState {
        val client = ModuleInstall.getClient(context)
        val api = TextScanner.recognizer
        val available = client.areModulesAvailable(api).await().areModulesAvailable()
        if (available) return TextModelState.Ready
        client.installModules(ModuleInstallRequest.newBuilder().addApi(api).build())
            .addOnSuccessListener { publish(TextModelState.Ready) }
            .addOnFailureListener { publish(classify(it)) }
        return TextModelState.Downloading
    }

    private fun classify(error: Throwable): TextModelState =
        (error as? ApiException)?.let { TextModelRules.fromStatusCode(it.statusCode) } ?: TextModelState.Unavailable

    private fun publish(observed: TextModelState) {
        val before = current.getAndUpdate { observed }
        if (before != observed) DiagLog.append(TAG, "$before -> $observed")
    }
}
