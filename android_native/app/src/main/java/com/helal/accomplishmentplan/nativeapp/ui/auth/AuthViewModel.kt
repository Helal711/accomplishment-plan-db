package com.helal.accomplishmentplan.nativeapp.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.helal.accomplishmentplan.nativeapp.data.auth.AuthRepository
import com.helal.accomplishmentplan.nativeapp.domain.auth.AuthErrorMapper
import com.helal.accomplishmentplan.nativeapp.domain.auth.AuthValidator
import com.helal.accomplishmentplan.nativeapp.domain.model.AuthSessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    val sessionState: StateFlow<AuthSessionState> = authRepository.sessionState

    private val _uiState = MutableStateFlow(
        AuthUiState(configStatus = authRepository.configStatus)
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun setMode(mode: AuthMode) {
        _uiState.update {
            it.copy(
                mode = mode,
                fullNameError = null,
                emailError = null,
                passwordError = null,
                confirmPasswordError = null,
                errorMessage = null,
                infoMessage = null
            )
        }
    }

    fun onFullNameChange(value: String) {
        _uiState.update { it.copy(fullName = value, fullNameError = null, errorMessage = null) }
    }

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, emailError = null, errorMessage = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, passwordError = null, errorMessage = null) }
    }

    fun onConfirmPasswordChange(value: String) {
        _uiState.update {
            it.copy(confirmPassword = value, confirmPasswordError = null, errorMessage = null)
        }
    }

    fun submitEmailAuth() {
        val current = _uiState.value
        if (current.isAnyActionLoading) return

        val validation = when (current.mode) {
            AuthMode.SIGN_IN -> AuthValidator.validateLogin(
                email = current.email,
                password = current.password
            )
            AuthMode.REGISTER -> AuthValidator.validateRegistration(
                fullName = current.fullName,
                email = current.email,
                password = current.password,
                confirmPassword = current.confirmPassword
            )
        }

        if (!validation.isValid) {
            _uiState.update {
                it.copy(
                    fullNameError = validation.fullNameError,
                    emailError = validation.emailError,
                    passwordError = validation.passwordError,
                    confirmPasswordError = validation.confirmPasswordError
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
            val result = when (current.mode) {
                AuthMode.SIGN_IN -> authRepository.signInWithEmail(current.email, current.password)
                AuthMode.REGISTER -> authRepository.registerWithEmail(
                    fullName = current.fullName,
                    email = current.email,
                    password = current.password
                )
            }
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            password = "",
                            confirmPassword = "",
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = AuthErrorMapper.toUserMessage(error)
                        )
                    }
                }
            )
        }
    }

    fun onGoogleSignInStarted() {
        _uiState.update { it.copy(isGoogleLoading = true, errorMessage = null, infoMessage = null) }
    }

    fun onGoogleIdTokenReceived(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGoogleLoading = true, errorMessage = null, infoMessage = null) }
            authRepository.signInWithGoogleIdToken(idToken).fold(
                onSuccess = {
                    _uiState.update { it.copy(isGoogleLoading = false, errorMessage = null) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isGoogleLoading = false,
                            errorMessage = AuthErrorMapper.toUserMessage(error)
                        )
                    }
                }
            )
        }
    }

    fun onGoogleSignInError(throwable: Throwable) {
        _uiState.update {
            it.copy(
                isGoogleLoading = false,
                errorMessage = AuthErrorMapper.toUserMessage(throwable)
            )
        }
    }

    fun openForgotPasswordDialog() {
        _uiState.update {
            it.copy(
                showForgotPasswordDialog = true,
                resetEmailInput = it.email,
                resetEmailError = null,
                errorMessage = null,
                infoMessage = null
            )
        }
    }

    fun dismissForgotPasswordDialog() {
        _uiState.update {
            it.copy(showForgotPasswordDialog = false, resetEmailError = null)
        }
    }

    fun onResetEmailChange(value: String) {
        _uiState.update { it.copy(resetEmailInput = value, resetEmailError = null) }
    }

    fun submitPasswordReset() {
        val current = _uiState.value
        val emailError = AuthValidator.validateResetEmail(current.resetEmailInput)
        if (emailError != null) {
            _uiState.update { it.copy(resetEmailError = emailError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
            authRepository.sendPasswordResetEmail(current.resetEmailInput).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            showForgotPasswordDialog = false,
                            infoMessage = "Password reset link sent to ${current.resetEmailInput.trim()}."
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            resetEmailError = AuthErrorMapper.toUserMessage(error)
                        )
                    }
                }
            )
        }
    }

    fun signOut(clearCredentialState: suspend () -> Unit = {}) {
        viewModelScope.launch {
            authRepository.signOut(clearCredentialState).onFailure { error ->
                _uiState.update {
                    it.copy(errorMessage = AuthErrorMapper.toUserMessage(error))
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, infoMessage = null) }
    }

    class Factory(
        private val authRepository: AuthRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                "Unsupported ViewModel class: ${modelClass.name}"
            }
            return AuthViewModel(authRepository) as T
        }
    }
}
