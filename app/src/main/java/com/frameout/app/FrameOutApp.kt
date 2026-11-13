package com.frameout.app

import android.app.Application
import androidx.work.Configuration
import com.frameout.app.billing.BillingManager
import com.frameout.core.hdr.HdrSupport
import com.frameout.core.util.AppContainer
import com.frameout.engine.decode.HdrFrameFetcher
import com.frameout.core.util.StringSource
import com.frameout.core.storage.StorageManager
import com.frameout.engine.work.ExtractReconciler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FrameOutApp : Application(), Configuration.Provider {
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setMinimumLoggingLevel(android.util.Log.INFO).build()

    private val maintenance = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    lateinit var container: AppContainer
        private set
    lateinit var billing: BillingManager
        private set
    lateinit var ads: com.frameout.app.ads.AdsHub
        private set

    override fun onCreate() {
        super.onCreate()
        initServices()
        initBilling()
        initSecurity()
        container.rating.noteSession()
        initMedia()
        com.frameout.engine.nudge.NudgeScheduler.start(this)
        scheduleMaintenance()
    }

    private fun initServices() {
        com.frameout.app.push.Push.init(this)
        StringSource.init(this)
        com.frameout.core.util.DiagLog.init(this)
        com.frameout.core.quality.ArabicOcr.init(this)
        container = AppContainer(this, BuildConfig.PLAY_BILLING)
        settleAccentTrial()
        container.entitlements.welcome.adoptExisting(container.prefs.onboardingDone(), container.prefs.accent.value)
        guardAccent()
    }

    private fun initBilling() {
        if (BuildConfig.PLAY_BILLING) com.frameout.app.billing.RevenueCatSync.configure(this)
        billing = BillingManager(container.entitlements, appScope)
        ads = com.frameout.app.ads.AdsHub(this, container.entitlements, appScope, onboardingDone = { container.prefs.onboardingDone() })
        com.frameout.app.ads.AdsGateway.hub = ads
        if (BuildConfig.PLAY_BILLING) {
            billing.start()
        }
    }

    private fun initSecurity() {
        appScope.launch { container.entitlements.accentForced.collect { key -> if (key != null) applyAccent(key) } }
        watchAccentTrial()
        com.frameout.core.storage.SaveFolderGuard.onNotice = { container.notice.value = it }
        com.frameout.core.edit.ProFilterSlot.install { com.frameout.app.security.ProPayload.load(this) }
        maintenance.launch { com.frameout.core.edit.ProFilterSlot.warm() }
        com.frameout.app.security.Sentinel.install(this, container.entitlements, appScope)
        if (BuildConfig.PLAY_BILLING) {
            container.entitlements.attachVault(com.frameout.app.security.BlockStoreTrialVault(this))
            registerVaultSync()
        }
    }

    private fun watchAccentTrial() {
        appScope.launch {
            container.entitlements.welcome.trial.collectLatest { on ->
                while (on) {
                    delay(1000)
                    if (container.entitlements.trialExpired()) {
                        settleAccentTrial()
                        if (!container.entitlements.welcome.trial.value) break
                    }
                }
            }
        }
        registerActivityLifecycleCallbacks(object : android.app.Application.ActivityLifecycleCallbacks {
            override fun onActivityStopped(activity: android.app.Activity) {
                if (!activity.isChangingConfigurations) settleAccentTrial()
            }
            override fun onActivityCreated(activity: android.app.Activity, savedInstanceState: android.os.Bundle?) {}
            override fun onActivityStarted(activity: android.app.Activity) {}
            override fun onActivityResumed(activity: android.app.Activity) {}
            override fun onActivityPaused(activity: android.app.Activity) {}
            override fun onActivitySaveInstanceState(activity: android.app.Activity, outState: android.os.Bundle) {}
            override fun onActivityDestroyed(activity: android.app.Activity) {}
        })
    }

    private fun registerVaultSync() {
        registerActivityLifecycleCallbacks(object : android.app.Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: android.app.Activity) { container.entitlements.syncVault() }
            override fun onActivityCreated(activity: android.app.Activity, savedInstanceState: android.os.Bundle?) {}
            override fun onActivityStarted(activity: android.app.Activity) {}
            override fun onActivityPaused(activity: android.app.Activity) {}
            override fun onActivityStopped(activity: android.app.Activity) {}
            override fun onActivitySaveInstanceState(activity: android.app.Activity, outState: android.os.Bundle) {}
            override fun onActivityDestroyed(activity: android.app.Activity) {}
        })
    }

    private fun initMedia() {
        HdrSupport.source = HdrFrameFetcher(this)
        com.frameout.core.storage.AnimatedFrameLookup.provider = com.frameout.engine.decode.AnimatedSources.lookup(this)
        com.frameout.core.media.VideoThumbLookup.provider = com.frameout.engine.decode.VideoThumbSources.lookup(this)
        com.frameout.core.media.FolderMediaIndex.prewarm(this)
        com.frameout.core.media.MediaPrewarm.start(this)
    }

    private fun scheduleMaintenance() {
        val storage = StorageManager(this)
        maintenance.launch { runCatching { ExtractReconciler.reconcile(this@FrameOutApp) } }
        maintenance.launch {
            storage.cleanupTemporaryFiles()
            storage.cleanupShareCache()
            storage.cleanupMotionCache()
            storage.cleanupArchiveStages()
            storage.cleanupUndoCache()
            runCatching { IncomingMedia.cleanup(this@FrameOutApp, container.projects.allSourceUris(), container.prefs.lastVideoUri()) }
            runCatching { IncomingMedia.releaseStaleGrants(this@FrameOutApp, container.projects.allSourceUris(), container.prefs.lastVideoUri()) }
        }
    }
}
