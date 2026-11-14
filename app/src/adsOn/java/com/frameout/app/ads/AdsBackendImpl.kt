package com.frameout.app.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.frameout.app.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewarded.ServerSideVerificationOptions
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.ads.rewardverification.RewardVerificationResult
import com.revenuecat.purchases.interfaces.PollRewardVerificationCallback

object AdsBackendImpl : AdsBackend {
    private const val TTL_MS = 55 * 60 * 1000L
    private var consentInfo: ConsentInformation? = null
    private var interstitial: InterstitialAd? = null
    private var interstitialAt = 0L
    private var rewarded: RewardedAd? = null
    private var rewardedAt = 0L
    private var rewardedTxId: String? = null
    private val main = Handler(Looper.getMainLooper())

    override val available: Boolean = true

    override fun gatherConsent(activity: Activity, done: (Boolean) -> Unit) {
        val info = UserMessagingPlatform.getConsentInformation(activity)
        consentInfo = info
        info.requestConsentInfoUpdate(
            activity,
            consentParams(activity),
            { UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { done(info.canRequestAds()) } },
            { done(info.canRequestAds()) }
        )
        if (info.canRequestAds()) done(true)
    }

    private fun consentParams(activity: Activity): ConsentRequestParameters {
        val builder = ConsentRequestParameters.Builder()
        if (BuildConfig.DEBUG && BuildConfig.ADS_DEBUG_EEA) {
            val debug = ConsentDebugSettings.Builder(activity)
                .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
            if (BuildConfig.ADS_TEST_DEVICE.isNotEmpty()) debug.addTestDeviceHashedId(BuildConfig.ADS_TEST_DEVICE)
            builder.setConsentDebugSettings(debug.build())
        }
        return builder.build()
    }

    override fun privacyOptionsRequired(): Boolean =
        consentInfo?.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    override fun showPrivacyOptions(activity: Activity, done: () -> Unit) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { done() }
    }

    override fun initialize(context: Context, ready: () -> Unit) {
        val main = Handler(Looper.getMainLooper())
        Thread { MobileAds.initialize(context) { main.post(ready) } }.start()
    }

    override fun loadInterstitial(context: Context, unitId: String, result: (AdLoad) -> Unit) {
        InterstitialAd.load(context, unitId, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                interstitial = ad
                interstitialAt = SystemClock.elapsedRealtime()
                result(AdLoad(true))
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                interstitial = null
                result(AdLoad(false, error.code))
            }
        })
    }

    override fun interstitialReady(): Boolean =
        interstitial != null && SystemClock.elapsedRealtime() - interstitialAt < TTL_MS

    override fun showInterstitial(activity: Activity, shown: () -> Unit, closed: (Boolean) -> Unit): Boolean {
        val ad = interstitial?.takeIf { interstitialReady() } ?: return false
        interstitial = null
        ad.fullScreenContentCallback = fullScreen(shown, closed)
        ad.show(activity)
        return true
    }

    override fun loadRewarded(context: Context, unitId: String, result: (AdLoad) -> Unit) {
        RewardedAd.load(context, unitId, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                rewardedTxId = attachVerification(ad)
                rewarded = ad
                rewardedAt = SystemClock.elapsedRealtime()
                result(AdLoad(true))
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                rewarded = null
                result(AdLoad(false, error.code))
            }
        })
    }

    override fun rewardedReady(): Boolean =
        rewarded != null && SystemClock.elapsedRealtime() - rewardedAt < TTL_MS

    private fun attachVerification(ad: RewardedAd): String? {
        if (!Purchases.isConfigured) return null
        return runCatching {
            val token = Purchases.sharedInstance.generateRewardVerificationToken(ad.responseInfo.responseId ?: "")
            ad.setServerSideVerificationOptions(
                ServerSideVerificationOptions.Builder().setUserId(token.appUserID).setCustomData(token.customData).build()
            )
            token.clientTransactionId
        }.getOrNull()
    }

    override fun showRewarded(activity: Activity, earned: () -> Unit, closed: () -> Unit): Boolean {
        val ad = rewarded?.takeIf { rewardedReady() } ?: return false
        val txId = rewardedTxId
        rewarded = null
        rewardedTxId = null
        var verifying = false
        var closeDeferred = false
        ad.fullScreenContentCallback = fullScreen({}) { if (verifying) { closeDeferred = true } else { closed() } }
        ad.show(activity, OnUserEarnedRewardListener {
            if (BuildConfig.DEBUG) { earned(); return@OnUserEarnedRewardListener }
            if (txId == null) return@OnUserEarnedRewardListener
            verifying = true
            Purchases.sharedInstance.pollRewardVerification(
                clientTransactionId = txId,
                callback = object : PollRewardVerificationCallback {
                    override fun onCompleted(result: RewardVerificationResult) {
                        main.post {
                            verifying = false
                            if (result.verifiedReward != null) earned()
                            if (closeDeferred) closed()
                        }
                    }
                }
            )
        })
        return true
    }

    private fun fullScreen(shown: () -> Unit, closed: (Boolean) -> Unit) = object : FullScreenContentCallback() {
        private var onScreen = false
        private var finished = false

        override fun onAdShowedFullScreenContent() {
            onScreen = true
            shown()
        }

        override fun onAdDismissedFullScreenContent() = finish(onScreen)
        override fun onAdFailedToShowFullScreenContent(error: AdError) = finish(false)

        private fun finish(completed: Boolean) {
            if (finished) return
            finished = true
            closed(completed)
        }
    }

    override fun preloadBanner(context: Context, unitId: String, widthDp: Int, result: (AdLoad) -> Unit) {
        BannerCache.obtain(context, unitId, widthDp, result)
    }

    override fun bannerReady(): Boolean = BannerCache.loaded

    override fun bannerHeightDp(context: Context, widthDp: Int): Int = BannerCache.heightDp(context, widthDp)

    override fun setBannerActive(active: Boolean) = BannerCache.setActive(active)

    override fun bindActivity(activity: Activity?) = BannerCache.bind(activity)
}
