package com.frameout.app.billing

enum class OffersFailure { NoConnection, NoOffering, StoreFailed }

sealed interface OfferingLoad {
    data class Loaded(val offers: List<PlanOffer>) : OfferingLoad

    data class Failed(val reason: OffersFailure, val detail: String) : OfferingLoad
}

internal object OffersDiagnosis {
    private val offline = setOf("NetworkError", "OfflineConnectionError")

    fun classify(errorCode: String?, hasCurrent: Boolean, packages: Int, mapped: Int): OffersFailure? = when {
        errorCode != null -> if (errorCode in offline) OffersFailure.NoConnection else OffersFailure.StoreFailed
        !hasCurrent || packages == 0 || mapped == 0 -> OffersFailure.NoOffering
        else -> null
    }

    fun describeEmpty(offeringIds: Collection<String>, currentId: String?, packages: List<String>, mapped: Int): String = when {
        currentId == null -> "no current offering, offerings=${offeringIds.sorted()}"
        packages.isEmpty() -> "current offering '$currentId' has no purchasable packages"
        else -> "current offering '$currentId' has ${packages.size} packages but $mapped match a plan, packages=$packages"
    }

    fun describeError(code: String, message: String?): String = "code=$code message=$message"
}
