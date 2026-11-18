package com.helal.accomplishmentplan.webview.webview

import android.net.Uri
import com.helal.accomplishmentplan.webview.config.AppConfig

object UrlNavigationPolicy {

    enum class Action {
        LOAD_IN_WEBVIEW,
        OPEN_EXTERNAL,
        BLOCK
    }

    private val AUTH_EXACT_HOSTS = setOf(
        "accounts.google.com",
        "accounts.youtube.com",
        "apis.google.com",
        "ogs.google.com",
        "consent.google.com",
        "gds.google.com",
        "myaccount.google.com",
        "ssl.gstatic.com",
        "www.gstatic.com",
        "content.googleapis.com",
        "securetoken.googleapis.com",
        "identitytoolkit.googleapis.com"
    )

    private val AUTH_SUFFIX_HOSTS = listOf(
        ".firebaseapp.com",
        ".web.app",
        ".accounts.google.com"
    )

    private val EXTERNAL_SAFE_SCHEMES = setOf(
        "mailto",
        "tel",
        "sms"
    )

    fun isPrimaryAppUri(uri: Uri?): Boolean {
        if (uri == null) return false
        val scheme = uri.scheme?.lowercase() ?: return false
        val host = uri.host?.lowercase() ?: return false
        return scheme == "https" && host == AppConfig.PRIMARY_HOST
    }

    fun isAuthHost(host: String?): Boolean {
        val normalized = host?.lowercase()?.trim() ?: return false
        if (normalized in AUTH_EXACT_HOSTS) return true
        return AUTH_SUFFIX_HOSTS.any { suffix -> normalized.endsWith(suffix) }
    }

    fun evaluateNavigation(uri: Uri?, isPopupWindow: Boolean = false): Action {
        if (uri == null) return Action.BLOCK
        val scheme = uri.scheme?.lowercase() ?: return Action.BLOCK

        if (scheme == "about") {
            return if (uri.toString().equals("about:blank", ignoreCase = true)) {
                Action.LOAD_IN_WEBVIEW
            } else {
                Action.BLOCK
            }
        }

        if (scheme in EXTERNAL_SAFE_SCHEMES) {
            return Action.OPEN_EXTERNAL
        }

        // Enforce strict HTTPS for all web navigation; block plain http://, file://, etc.
        if (scheme != "https") {
            return Action.BLOCK
        }

        val host = uri.host?.lowercase() ?: return Action.BLOCK
        if (host == AppConfig.PRIMARY_HOST || isAuthHost(host)) {
            return Action.LOAD_IN_WEBVIEW
        }

        // Keep auth redirect helpers within popup window if initiated by OAuth flow
        if (isPopupWindow && (host.endsWith(".google.com") || host == "google.com")) {
            return Action.LOAD_IN_WEBVIEW
        }

        return Action.OPEN_EXTERNAL
    }

    /**
     * Strips the WebView "; wv" token from the system User-Agent so Google OAuth
     * does not reject sign-in with 403 disallowed_useragent, while retaining the
     * genuine Chromium and Android OS version tokens.
     */
    fun sanitizeUserAgent(defaultUserAgent: String?): String {
        val base = defaultUserAgent?.trim().orEmpty()
        if (base.isEmpty()) {
            return "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
        }
        return base
            .replace("; wv)", ")")
            .replace("; wv;", ";")
            .replace(" wv)", ")")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
