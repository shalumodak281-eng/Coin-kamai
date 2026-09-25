package com.coinkamai.app

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var bannerContainer: LinearLayout
    private var bannerAdView: AdView? = null

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null

    private val AD_UNIT_BANNER = "ca-app-pub-7573874317444899/4104868674"
    private val AD_UNIT_INTERSTITIAL = "ca-app-pub-7573874317444899/4813203271"
    private val AD_UNIT_REWARDED = "ca-app-pub-7573874317444899/6182125262"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webview)
        bannerContainer = findViewById(R.id.banner_container)

        setupWebView()

        MobileAds.initialize(this) {
            Log.d("CoinKamaiAds", "AdMob initialized")
            loadBanner()
            loadInterstitial()
            loadRewarded()
        }

        onBackPressedCallback()
    }

    private fun setupWebView() {
        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.setSupportZoom(false)

        webView.webViewClient = WebViewClient()
        webView.addJavascriptInterface(AndroidBridge(), "AndroidBridge")
        webView.loadUrl("file:///android_asset/earn-app.html")
    }

    private fun loadBanner() {
        val adView = AdView(this)
        adView.adUnitId = AD_UNIT_BANNER
        adView.setAdSize(AdSize.BANNER)
        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                Log.d("CoinKamaiAds", "Banner loaded")
            }
            override fun onAdFailedToLoad(error: LoadAdError) {
                Log.e("CoinKamaiAds", "Banner FAILED to load: ${error.message} (code ${error.code})")
            }
        }
        bannerContainer.removeAllViews()
        bannerContainer.addView(adView)
        bannerAdView = adView
        adView.loadAd(AdRequest.Builder().build())
    }

    private fun loadInterstitial() {
        InterstitialAd.load(this, AD_UNIT_INTERSTITIAL, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                interstitialAd = ad
                Log.d("CoinKamaiAds", "Interstitial loaded")
            }
            override fun onAdFailedToLoad(error: LoadAdError) {
                interstitialAd = null
                Log.e("CoinKamaiAds", "Interstitial FAILED to load: ${error.message} (code ${error.code})")
            }
        })
    }

    private fun showInterstitialThenOpenWithdraw() {
        val ad = interstitialAd
        if (ad == null) {
            openWithdrawInWebView()
            loadInterstitial()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                openWithdrawInWebView()
                loadInterstitial()
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitialAd = null
                Log.e("CoinKamaiAds", "Interstitial FAILED to show: ${error.message}")
                openWithdrawInWebView()
                loadInterstitial()
            }
        }
        ad.show(this)
    }

    private fun openWithdrawInWebView() {
        runOnUiThread {
            webView.evaluateJavascript("window.__nativeOpenWithdraw && window.__nativeOpenWithdraw();", null)
        }
    }

    private fun loadRewarded() {
        RewardedAd.load(this, AD_UNIT_REWARDED, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                rewardedAd = ad
                Log.d("CoinKamaiAds", "Rewarded loaded")
            }
            override fun onAdFailedToLoad(error: LoadAdError) {
                rewardedAd = null
                Log.e("CoinKamaiAds", "Rewarded FAILED to load: ${error.message} (code ${error.code})")
            }
        })
    }

    private fun showRewardedAd() {
        val ad = rewardedAd
        if (ad == null) {
            notifyJs("window.__nativeAdFailed && window.__nativeAdFailed();")
            loadRewarded()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                loadRewarded()
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                rewardedAd = null
                Log.e("CoinKamaiAds", "Rewarded FAILED to show: ${error.message}")
                notifyJs("window.__nativeAdFailed && window.__nativeAdFailed();")
                loadRewarded()
            }
        }
        ad.show(this) { _ ->
            notifyJs("window.__nativeRewardEarned && window.__nativeRewardEarned();")
        }
    }

    private fun notifyJs(script: String) {
        runOnUiThread { webView.evaluateJavascript(script, null) }
    }

    inner class AndroidBridge {
        @JavascriptInterface
        fun showRewardedAd() {
            runOnUiThread { this@MainActivity.showRewardedAd() }
        }

        @JavascriptInterface
        fun requestOpenWithdraw() {
            runOnUiThread { showInterstitialThenOpenWithdraw() }
        }
    }

    private fun onBackPressedCallback() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun onDestroy() {
        bannerAdView?.destroy()
        super.onDestroy()
    }
}
