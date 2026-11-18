package com.helal.accomplishmentplan.webview.webview

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView

object WebViewConfigurator {

    @SuppressLint("SetJavaScriptEnabled")
    fun configure(
        webView: WebView,
        downloadBridge: DownloadBridge? = null
    ) {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true

            // Responsive mobile viewport handling
            useWideViewPort = true
            loadWithOverviewMode = true
            builtInZoomControls = true
            displayZoomControls = false
            setSupportZoom(true)

            // Support OAuth popups (Firebase signInWithPopup)
            setSupportMultipleWindows(true)
            javaScriptCanOpenWindowsAutomatically = true

            // Strict HTTPS & file-access hardening
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            allowFileAccess = false
            allowContentAccess = true
            @Suppress("DEPRECATION")
            allowFileAccessFromFileURLs = false
            @Suppress("DEPRECATION")
            allowUniversalAccessFromFileURLs = false
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                safeBrowsingEnabled = true
            }

            cacheMode = WebSettings.LOAD_DEFAULT
            userAgentString = UrlNavigationPolicy.sanitizeUserAgent(userAgentString)
        }

        if (downloadBridge != null) {
            webView.addJavascriptInterface(downloadBridge, "AndroidDownloadBridge")
        }
    }
}
