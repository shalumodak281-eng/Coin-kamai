package com.coinkamai.app

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd

class AppOpenAdManager(
    private val application: Application,
    private val adUnitId: String
) : Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var isShowingAd = false
    private var currentActivity: Activity? = null

    init {
        application.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    fun preload() {
        loadAd()
    }

    private fun loadAd() {
        if (isLoadingAd || appOpenAd != null) return
        isLoadingAd = true
        val request = AdRequest.Builder().build()
        AppOpenAd.load(application, adUnitId, request, object : AppOpenAd.AppOpenAdLoadCallback() {
            override fun onAdLoaded(ad: AppOpenAd) {
                appOpenAd = ad
                isLoadingAd = false
                Log.d("CoinKamaiAds", "App Open ad loaded")
            }
            override fun onAdFailedToLoad(error: LoadAdError) {
                isLoadingAd = false
                Log.e("CoinKamaiAds", "App Open ad FAILED to load: ${error.message} (code ${error.code})")
            }
        })
    }

    private fun showAdIfAvailable() {
        if (isShowingAd) return
        val ad = appOpenAd
        val activity = currentActivity
        if (ad == null || activity == null) {
            loadAd()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                appOpenAd = null
                isShowingAd = false
                loadAd()
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                appOpenAd = null
                isShowingAd = false
                Log.e("CoinKamaiAds", "App Open ad FAILED to show: ${error.message}")
                loadAd()
            }
            override fun onAdShowedFullScreenContent() {
                isShowingAd = true
            }
        }
        ad.show(activity)
    }

    override fun onStart(owner: LifecycleOwner) {
        showAdIfAvailable()
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) { currentActivity = activity }
    override fun onActivityResumed(activity: Activity) { currentActivity = activity }
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
