package com.helal.accomplishmentplan.nativeapp.domain.model

data class AuthUser(
    val uid: String,
    val email: String,
    val displayName: String,
    val photoUrl: String? = null,
    val providerIds: List<String> = emptyList(),
    val isEmailVerified: Boolean = false
) {
    val greetingName: String
        get() = displayName.trim().ifBlank {
            email.substringBefore("@").ifBlank { "User" }
        }
}

sealed interface AuthSessionState {
    data object Initializing : AuthSessionState
    data object FirebaseNotConfigured : AuthSessionState
    data object Unauthenticated : AuthSessionState
    data class Authenticated(val user: AuthUser) : AuthSessionState
}
