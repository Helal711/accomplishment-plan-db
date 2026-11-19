package com.helal.accomplishmentplan.nativeapp.domain.auth

data class AuthValidationResult(
    val fullNameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null
) {
    val isValid: Boolean
        get() = fullNameError == null &&
            emailError == null &&
            passwordError == null &&
            confirmPasswordError == null
}

object AuthValidator {

    private val EMAIL_REGEX = Regex(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    )
    const val MIN_PASSWORD_LENGTH = 6

    fun validateLogin(email: String, password: String): AuthValidationResult {
        val trimmedEmail = email.trim()
        val emailError = when {
            trimmedEmail.isEmpty() -> "Please enter your email address."
            !EMAIL_REGEX.matches(trimmedEmail) -> "Please enter a valid email address."
            else -> null
        }
        val passwordError = when {
            password.isEmpty() -> "Please enter your password."
            password.length < MIN_PASSWORD_LENGTH ->
                "Password must be at least $MIN_PASSWORD_LENGTH characters."
            else -> null
        }
        return AuthValidationResult(
            emailError = emailError,
            passwordError = passwordError
        )
    }

    fun validateRegistration(
        fullName: String,
        email: String,
        password: String,
        confirmPassword: String
    ): AuthValidationResult {
        val trimmedName = fullName.trim()
        val base = validateLogin(email, password)
        val fullNameError = when {
            trimmedName.isEmpty() -> "Please enter your full name."
            trimmedName.length < 2 -> "Name must be at least 2 characters."
            trimmedName.length > 80 -> "Name must be 80 characters or fewer."
            else -> null
        }
        val confirmError = when {
            confirmPassword.isEmpty() -> "Please confirm your password."
            confirmPassword != password -> "Passwords do not match."
            else -> null
        }
        return base.copy(
            fullNameError = fullNameError,
            confirmPasswordError = confirmError
        )
    }

    fun validateResetEmail(email: String): String? {
        val trimmedEmail = email.trim()
        return when {
            trimmedEmail.isEmpty() -> "Please enter your email address."
            !EMAIL_REGEX.matches(trimmedEmail) -> "Please enter a valid email address."
            else -> null
        }
    }
}
