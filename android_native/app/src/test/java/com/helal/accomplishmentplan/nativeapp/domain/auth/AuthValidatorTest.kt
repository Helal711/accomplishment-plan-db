package com.helal.accomplishmentplan.nativeapp.domain.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthValidatorTest {

    @Test
    fun `valid login credentials pass validation`() {
        val result = AuthValidator.validateLogin("user@example.com", "secret123")
        assertTrue(result.isValid)
        assertNull(result.emailError)
        assertNull(result.passwordError)
    }

    @Test
    fun `invalid email and short password fail login validation`() {
        val result = AuthValidator.validateLogin("not-an-email", "123")
        assertFalse(result.isValid)
        assertNotNull(result.emailError)
        assertNotNull(result.passwordError)
    }

    @Test
    fun `registration checks name and matching confirm password`() {
        val mismatch = AuthValidator.validateRegistration(
            fullName = "H",
            email = "user@example.com",
            password = "password123",
            confirmPassword = "password456"
        )
        assertFalse(mismatch.isValid)
        assertNotNull(mismatch.fullNameError)
        assertNotNull(mismatch.confirmPasswordError)

        val valid = AuthValidator.validateRegistration(
            fullName = "Helal Uddin",
            email = "helal@example.com",
            password = "password123",
            confirmPassword = "password123"
        )
        assertTrue(valid.isValid)
    }

    @Test
    fun `reset email validation rejects blank or malformed email`() {
        assertNotNull(AuthValidator.validateResetEmail(""))
        assertNotNull(AuthValidator.validateResetEmail("bad@"))
        assertNull(AuthValidator.validateResetEmail("valid.user@company.org"))
    }
}
