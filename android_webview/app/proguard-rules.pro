# Keep WebView JavaScript download bridge intact under R8 minification
-keepclassmembers class com.helal.accomplishmentplan.webview.webview.DownloadBridge {
    @android.webkit.JavascriptInterface <methods>;
}
