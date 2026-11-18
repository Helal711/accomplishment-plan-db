package com.helal.accomplishmentplan.webview

import android.net.Uri
import com.helal.accomplishmentplan.webview.config.AppConfig
import com.helal.accomplishmentplan.webview.webview.UrlNavigationPolicy
import com.helal.accomplishmentplan.webview.webview.UrlNavigationPolicy.Action
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class UrlNavigationPolicyTest {

    @Test
    fun `primary live app url loads inside webview`() {
        val uri = Uri.parse(AppConfig.LIVE_APP_URL)
        assertTrue(UrlNavigationPolicy.isPrimaryAppUri(uri))
        assertEquals(Action.LOAD_IN_WEBVIEW, UrlNavigationPolicy.evaluateNavigation(uri))
    }

    @Test
    fun `google oauth and firebase auth urls load inside webview`() {
        val authUrls = listOf(
            "https://accounts.google.com/o/oauth2/auth?client_id=123",
            "https://accomplishment-plan-db.firebaseapp.com/__/auth/handler",
            "https://accomplishment-plan-db.web.app/__/auth/handler",
            "https://apis.google.com/js/api.js"
        )
        authUrls.forEach { raw ->
            assertEquals(
                "Expected LOAD_IN_WEBVIEW for $raw",
                Action.LOAD_IN_WEBVIEW,
                UrlNavigationPolicy.evaluateNavigation(Uri.parse(raw), isPopupWindow = true)
            )
        }
    }

    @Test
    fun `external https and safe action schemes open externally`() {
        assertEquals(
            Action.OPEN_EXTERNAL,
            UrlNavigationPolicy.evaluateNavigation(Uri.parse("https://example.com/docs"))
        )
        assertEquals(
            Action.OPEN_EXTERNAL,
            UrlNavigationPolicy.evaluateNavigation(Uri.parse("mailto:support@example.com"))
        )
        assertEquals(
            Action.OPEN_EXTERNAL,
            UrlNavigationPolicy.evaluateNavigation(Uri.parse("tel:+8801700000000"))
        )
    }

    @Test
    fun `insecure http file and javascript urls are blocked`() {
        assertEquals(
            Action.BLOCK,
            UrlNavigationPolicy.evaluateNavigation(Uri.parse("http://helal711.github.io/accomplishment-plan-db/"))
        )
        assertEquals(
            Action.BLOCK,
            UrlNavigationPolicy.evaluateNavigation(Uri.parse("file:///sdcard/secret.txt"))
        )
        assertEquals(
            Action.BLOCK,
            UrlNavigationPolicy.evaluateNavigation(Uri.parse("javascript:alert(1)"))
        )
    }

    @Test
    fun `sanitizeUserAgent removes wv marker while keeping Chrome token`() {
        val raw = "Mozilla/5.0 (Linux; Android 14; Pixel 8 Build/AP1A; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/131.0.6778.100 Mobile Safari/537.36"
        val clean = UrlNavigationPolicy.sanitizeUserAgent(raw)
        assertFalse(clean.contains("; wv"))
        assertTrue(clean.contains("Chrome/131.0.6778.100"))
    }
}
