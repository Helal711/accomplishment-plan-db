package com.helal.accomplishmentplan.nativeapp.core.firebase

import android.content.Context
import com.google.firebase.FirebaseApp
import com.helal.accomplishmentplan.nativeapp.BuildConfig

data class FirebaseConfigStatus(
    val hasGoogleServicesFile: Boolean,
    val isFirebaseInitialized: Boolean,
    val googleWebClientId: String?,
    val expectedProjectId: String
) {
    val isReadyForEmailAuth: Boolean
        get() = isFirebaseInitialized

    val isReadyForGoogleSignIn: Boolean
        get() = isFirebaseInitialized && !googleWebClientId.isNullOrBlank()
}

/**
 * Inspects real runtime Firebase initialization state without inventing credentials
 * or falling back to hardcoded keys.
 */
object FirebaseRuntimeConfig {

    fun inspect(context: Context): FirebaseConfigStatus {
        val initialized = runCatching { FirebaseApp.getApps(context).isNotEmpty() }
            .getOrDefault(false)
        val webClientId = if (initialized) {
            resolveDefaultWebClientId(context)
        } else {
            null
        }
        return FirebaseConfigStatus(
            hasGoogleServicesFile = BuildConfig.HAS_GOOGLE_SERVICES_CONFIG,
            isFirebaseInitialized = initialized,
            googleWebClientId = webClientId,
            expectedProjectId = BuildConfig.EXPECTED_FIREBASE_PROJECT_ID
        )
    }

    private fun resolveDefaultWebClientId(context: Context): String? {
        val resId = context.resources.getIdentifier(
            "default_web_client_id",
            "string",
            context.packageName
        )
        if (resId == 0) return null
        return runCatching { context.getString(resId).trim() }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
    }
}
