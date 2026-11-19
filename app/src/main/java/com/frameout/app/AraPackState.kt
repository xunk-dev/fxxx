package com.frameout.app

import com.google.android.play.core.assetpacks.model.AssetPackErrorCode
import com.google.android.play.core.assetpacks.model.AssetPackStatus

sealed interface PackState {
    data object Idle : PackState
    data class Downloading(val percent: Int) : PackState
    data object WaitingWifi : PackState
    data object NeedsConfirmation : PackState
    data object Ready : PackState
    data class Failed(val errorCode: Int) : PackState
    data object Unavailable : PackState
}

enum class PackAction { None, Retry, Confirm }

internal object PackStates {
    private val NO_PLAY = setOf(
        AssetPackErrorCode.API_NOT_AVAILABLE,
        AssetPackErrorCode.UNRECOGNIZED_INSTALLATION,
        AssetPackErrorCode.APP_NOT_OWNED
    )

    fun fromStatus(status: Int, errorCode: Int, downloaded: Long, total: Long): PackState = when (status) {
        AssetPackStatus.COMPLETED -> PackState.Ready
        AssetPackStatus.WAITING_FOR_WIFI -> PackState.WaitingWifi
        AssetPackStatus.REQUIRES_USER_CONFIRMATION -> PackState.NeedsConfirmation
        AssetPackStatus.FAILED, AssetPackStatus.CANCELED, AssetPackStatus.NOT_INSTALLED -> failure(errorCode)
        else -> PackState.Downloading(percent(downloaded, total))
    }

    fun failure(errorCode: Int): PackState = if (errorCode in NO_PLAY) PackState.Unavailable else PackState.Failed(errorCode)

    fun percent(downloaded: Long, total: Long): Int =
        if (total <= 0L) 0 else (downloaded.coerceAtLeast(0L) * 100L / total).toInt().coerceIn(0, 99)

    fun visible(state: PackState): Boolean = state != PackState.Idle && state != PackState.Ready

    fun action(state: PackState): PackAction = when (state) {
        is PackState.Failed, PackState.Unavailable -> PackAction.Retry
        PackState.NeedsConfirmation -> PackAction.Confirm
        else -> PackAction.None
    }
}
