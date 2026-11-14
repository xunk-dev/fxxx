package com.frameout.app.billing

import android.app.Activity
import com.frameout.core.pro.Entitlements
import com.frameout.core.util.DiagLog
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesTransactionException
import com.revenuecat.purchases.awaitPurchase
import com.revenuecat.purchases.restorePurchasesWith
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class BillingNote { None, Unavailable, Error, Network, AlreadyOwned, Pending, Restored, NothingToRestore }

enum class OffersState { Loading, Ready, Failed }

class BillingManager(
    private val entitlements: Entitlements,
    private val scope: CoroutineScope
) {
    private val _offers = MutableStateFlow<List<PlanOffer>>(emptyList())
    val offers: StateFlow<List<PlanOffer>> = _offers.asStateFlow()

    private val _note = MutableStateFlow(BillingNote.None)
    val note: StateFlow<BillingNote> = _note.asStateFlow()

    private val _state = MutableStateFlow(OffersState.Loading)
    val state: StateFlow<OffersState> = _state.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    fun start() {
        if (!entitlements.enforced) return
        if (!RevenueCatSync.isActive) {
            _note.value = BillingNote.Unavailable
            _state.value = OffersState.Failed
            return
        }
        scope.launch { refresh() }
        autoRestore()
    }

    fun retry() {
        if (!RevenueCatSync.isActive) return
        _note.value = BillingNote.None
        scope.launch { refresh() }
    }

    private fun autoRestore() {
        if (!entitlements.shouldAutoRestore()) return
        Purchases.sharedInstance.restorePurchasesWith(
            onError = { error -> DiagLog.append("billing", "RevenueCat auto restore failed: ${error.code}") },
            onSuccess = { info ->
                RevenueCatSync.apply(info)
                entitlements.markAutoRestored()
            }
        )
    }

    suspend fun refresh() {
        _state.value = OffersState.Loading
        val loaded = BillingCatalog.offersOf(RevenueCatSync.currentOffering())
        if (loaded.isNotEmpty()) _offers.value = loaded
        _state.value = if (_offers.value.isEmpty()) OffersState.Failed else OffersState.Ready
        RevenueCatSync.refresh()
    }

    fun restore() {
        if (!RevenueCatSync.isActive) {
            _note.value = BillingNote.Unavailable
            return
        }
        if (_busy.value) return
        _busy.value = true
        _note.value = BillingNote.None
        Purchases.sharedInstance.restorePurchasesWith(
            onError = { error ->
                DiagLog.append("billing", "RevenueCat restore failed: ${error.code}")
                _busy.value = false
                _note.value = if (error.code == PurchasesErrorCode.NetworkError) BillingNote.Network else BillingNote.Error
            },
            onSuccess = { info ->
                _busy.value = false
                RevenueCatSync.apply(info)
                _note.value = if (RevenueCatSync.isEntitled(info)) BillingNote.Restored else BillingNote.NothingToRestore
            }
        )
    }

    fun launch(activity: Activity, offer: PlanOffer) {
        if (!RevenueCatSync.isActive) {
            _note.value = BillingNote.Unavailable
            return
        }
        if (_busy.value) return
        _busy.value = true
        _note.value = BillingNote.None
        scope.launch(Dispatchers.Main.immediate) {
            try {
                val result = Purchases.sharedInstance.awaitPurchase(PurchaseParams.Builder(activity, offer.rcPackage).build())
                RevenueCatSync.apply(result.customerInfo)
            } catch (e: PurchasesTransactionException) {
                when {
                    e.userCancelled -> Unit
                    e.code == PurchasesErrorCode.PaymentPendingError -> _note.value = BillingNote.Pending
                    e.code == PurchasesErrorCode.NetworkError -> _note.value = BillingNote.Network
                    e.code == PurchasesErrorCode.ProductAlreadyPurchasedError -> _note.value = BillingNote.AlreadyOwned
                    else -> {
                        DiagLog.append("billing", "RevenueCat purchase failed: ${e.code} ${e.underlyingErrorMessage}")
                        _note.value = BillingNote.Error
                    }
                }
            } finally {
                _busy.value = false
            }
        }
    }

    fun clearNote() { _note.value = BillingNote.None }
}
