package com.frameout.core.quality

enum class TextModelState { Unknown, Ready, Downloading, NoPlayServices, PlayServicesOutdated, Unavailable;

    val problem: Boolean get() = this != Unknown && this != Ready
}

object TextModelRules {
    private const val SERVICE_MISSING = 1
    private const val SERVICE_VERSION_UPDATE_REQUIRED = 2
    private const val SERVICE_DISABLED = 3
    private const val SERVICE_INVALID = 9
    private const val API_NOT_CONNECTED = 17
    private const val MLKIT_UNAVAILABLE = 14

    fun fromStatusCode(code: Int): TextModelState = when (code) {
        SERVICE_MISSING, SERVICE_DISABLED, SERVICE_INVALID, API_NOT_CONNECTED -> TextModelState.NoPlayServices
        SERVICE_VERSION_UPDATE_REQUIRED -> TextModelState.PlayServicesOutdated
        else -> TextModelState.Unavailable
    }

    fun fromMlKitCode(code: Int): TextModelState? = if (code == MLKIT_UNAVAILABLE) TextModelState.Downloading else null
}
