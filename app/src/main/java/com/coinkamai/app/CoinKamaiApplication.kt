package com.coinkamai.app

import android.app.Application
import com.google.android.gms.ads.MobileAds

class CoinKamaiApplication : Application() {

    lateinit var appOpenAdManager: AppOpenAdManager
        private set

    private val AD_UNIT_APP_OPEN = "ca-app-pub-7573874317444899/4272389636"

    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this) {}
        appOpenAdManager = AppOpenAdManager(this, AD_UNIT_APP_OPEN)
        appOpenAdManager.preload()
    }
}
