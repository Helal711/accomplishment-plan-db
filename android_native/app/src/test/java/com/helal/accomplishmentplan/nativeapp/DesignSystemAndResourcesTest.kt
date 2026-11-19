package com.helal.accomplishmentplan.nativeapp

import android.content.Context
import androidx.core.content.res.ResourcesCompat
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DesignSystemAndResourcesTest {

    @Test
    fun `app identity and Bengali plus Latin fonts are bundled in resources`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        assertEquals("Accomplishment Plan DB", context.getString(R.string.app_name))
        assertEquals("com.helal.accomplishmentplan.nativeapp", context.packageName)
        assertEquals("accomplishment-plan-db", BuildConfig.EXPECTED_FIREBASE_PROJECT_ID)
        assertFalse(
            "google-services.json must not be invented before real Firebase config is provided",
            BuildConfig.HAS_GOOGLE_SERVICES_CONFIG
        )

        assertNotNull(ResourcesCompat.getFont(context, R.font.plus_jakarta_sans))
        assertNotNull(ResourcesCompat.getFont(context, R.font.inter))
        assertNotNull(ResourcesCompat.getFont(context, R.font.noto_sans_bengali))
    }

    @Test
    fun `adaptive launcher icon resources resolve cleanly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        assertNotNull(ResourcesCompat.getDrawable(context.resources, R.mipmap.ic_launcher, null))
        assertNotNull(ResourcesCompat.getDrawable(context.resources, R.mipmap.ic_launcher_round, null))
        assertNotNull(ResourcesCompat.getDrawable(context.resources, R.drawable.ic_launcher_art, null))
    }
}
