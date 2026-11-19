package com.helal.accomplishmentplan.nativeapp.domain.auth

import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException

object AuthErrorMapper {

    fun toUserMessage(throwable: Throwable): String {
        return when (throwable) {
            is GetCredentialCancellationException -> "Google Sign-In was cancelled."
            is NoCredentialException ->
                "No Google account is available on this device. Add a Google account or use Email & Password."
            is GetCredentialException ->
                throwable.errorMessage?.toString()?.takeIf { it.isNotBlank() }
                    ?: "Google Sign-In could not be completed. Verify SHA-1/SHA-256 and OAuth configuration."
            is FirebaseNetworkException ->
                "Network connection unavailable. Check your internet connection and try again."
            is FirebaseAuthException -> mapFirebaseAuthCode(throwable.errorCode)
            is IllegalStateException ->
                throwable.message?.takeIf { it.isNotBlank() }
                    ?: "Firebase authentication is not configured yet."
            else -> throwable.localizedMessage?.takeIf { it.isNotBlank() }
                ?: "Authentication failed. Please try again."
        }
    }

    internal fun mapFirebaseAuthCode(errorCode: String): String {
        return when (errorCode) {
            "ERROR_INVALID_EMAIL" -> "The email address is badly formatted."
            "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL" ->
                "Invalid email or password. Please check your credentials and try again."
            "ERROR_USER_NOT_FOUND" -> "No account found with this email address."
            "ERROR_USER_DISABLED" -> "This account has been disabled. Contact your administrator."
            "ERROR_EMAIL_ALREADY_IN_USE" ->
                "An account with this email already exists. Try signing in instead."
            "ERROR_WEAK_PASSWORD" -> "Password is too weak. Choose at least 6 characters."
            "ERROR_TOO_MANY_REQUESTS" ->
                "Too many failed attempts. Please wait a moment and try again."
            "ERROR_OPERATION_NOT_ALLOWED" ->
                "This sign-in method is not enabled in Firebase Authentication settings."
            "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" ->
                "An account already exists with this email using a different sign-in provider."
            else -> "Authentication error ($errorCode). Please try again."
        }
    }
}
