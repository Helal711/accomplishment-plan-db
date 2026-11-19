package com.helal.accomplishmentplan.nativeapp.ui.auth

import com.helal.accomplishmentplan.nativeapp.core.firebase.FirebaseConfigStatus

enum class AuthMode {
    SIGN_IN,
    REGISTER
}

data class AuthUiState(
    val mode: AuthMode = AuthMode.SIGN_IN,
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val fullNameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val isLoading: Boolean = false,
    val isGoogleLoading: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val showForgotPasswordDialog: Boolean = false,
    val resetEmailInput: String = "",
    val resetEmailError: String? = null,
    val configStatus: FirebaseConfigStatus = FirebaseConfigStatus(
        hasGoogleServicesFile = false,
        isFirebaseInitialized = false,
        googleWebClientId = null,
        expectedProjectId = "accomplishment-plan-db"
    )
) {
    val isAnyActionLoading: Boolean
        get() = isLoading || isGoogleLoading
}
