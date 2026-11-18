package com.helal.accomplishmentplan.webview

import android.Manifest
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.test.core.app.ApplicationProvider
import com.helal.accomplishmentplan.webview.webview.WebViewConfigurator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MainActivityRobolectricTest {

    @Test
    fun `app identity permissions and https enforcement match requirements`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("com.helal.accomplishmentplan.webview", context.packageName)
        assertEquals("Accomplishment Plan DB", context.getString(R.string.app_name))

        val pkgInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS or PackageManager.GET_PROVIDERS
        )
        val permissions = pkgInfo.requestedPermissions?.toSet().orEmpty()
        assertTrue(permissions.contains(Manifest.permission.INTERNET))
        assertTrue(permissions.contains(Manifest.permission.ACCESS_NETWORK_STATE))
        assertTrue(permissions.contains(Manifest.permission.CAMERA))

        // Play policy: zero broad storage permissions
        assertFalse(permissions.contains(Manifest.permission.READ_EXTERNAL_STORAGE))
        assertFalse(permissions.contains(Manifest.permission.WRITE_EXTERNAL_STORAGE))
        assertFalse(permissions.contains("android.permission.READ_MEDIA_IMAGES"))
        assertFalse(permissions.contains("android.permission.READ_MEDIA_VIDEO"))

        // Strict HTTPS: cleartext traffic disabled
        val usesCleartext = (context.applicationInfo.flags and ApplicationInfo.FLAG_USES_CLEARTEXT_TRAFFIC) != 0
        assertFalse(usesCleartext)

        val providerAuthorities = pkgInfo.providers?.mapNotNull { it.authority }.orEmpty()
        assertTrue(providerAuthorities.contains("com.helal.accomplishmentplan.webview.fileprovider"))
    }

    @Test
    fun `webview configurator enables js dom storage popups and blocks mixed content and file access`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val webView = WebView(context)
        WebViewConfigurator.configure(webView)

        assertTrue(webView.settings.javaScriptEnabled)
        assertTrue(webView.settings.domStorageEnabled)
        assertTrue(webView.settings.useWideViewPort)
        assertTrue(webView.settings.loadWithOverviewMode)
        assertTrue(webView.settings.supportMultipleWindows())
        assertTrue(webView.settings.javaScriptCanOpenWindowsAutomatically)
        assertEquals(WebSettings.MIXED_CONTENT_NEVER_ALLOW, webView.settings.mixedContentMode)
        assertFalse(webView.settings.allowFileAccess)
        assertFalse(webView.settings.userAgentString.contains("; wv"))
    }

    @Test
    fun `main activity launches cleanly`() {
        ShadowLooper.pauseMainLooper()
        val controller = Robolectric.buildActivity(MainActivity::class.java).create().start()
        val activity = controller.get()
        assertNotNull(activity)
        controller.stop().destroy()
    }
}
