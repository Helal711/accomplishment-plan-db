package com.helal.accomplishmentplan.nativeapp.data.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.security.MessageDigest
import java.util.UUID

/**
 * Android Credential Manager integration for Google Sign-In.
 * Uses GetSignInWithGoogleOption for explicit button-driven sign-in and clears
 * credential state cleanly on sign-out.
 */
class GoogleCredentialHelper {

    suspend fun requestGoogleIdToken(
        activityContext: Context,
        serverClientId: String
    ): Result<String> = runCatching {
        require(serverClientId.isNotBlank()) {
            "Google Web Client ID (default_web_client_id) is missing. Register com.helal.accomplishmentplan.nativeapp with SHA-1/SHA-256 in Firebase Console and add google-services.json."
        }

        val credentialManager = CredentialManager.create(activityContext)
        val rawNonce = UUID.randomUUID().toString()
        val hashedNonce = sha256(rawNonce)

        val googleOption = GetSignInWithGoogleOption.Builder(serverClientId)
            .setNonce(hashedNonce)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleOption)
            .build()

        val response = credentialManager.getCredential(
            context = activityContext,
            request = request
        )

        val credential = response.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
            googleCredential.idToken
        } else {
            error("Unexpected credential type received from Credential Manager.")
        }
    }

    suspend fun clearCredentialState(context: Context) {
        runCatching {
            val credentialManager = CredentialManager.create(context)
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        }
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }
}
