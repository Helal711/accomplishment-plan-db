package com.helal.accomplishmentplan.nativeapp.domain.auth

import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthErrorMapperTest {

    @Test
    fun `maps Credential Manager cancellation and no-credential exceptions cleanly`() {
        val cancelledMsg = AuthErrorMapper.toUserMessage(GetCredentialCancellationException("cancelled"))
        val noCredMsg = AuthErrorMapper.toUserMessage(NoCredentialException("none"))

        assertTrue(cancelledMsg.contains("cancelled", ignoreCase = true))
        assertTrue(noCredMsg.contains("Google account", ignoreCase = true))
    }

    @Test
    fun `maps common FirebaseAuth error codes to friendly messages`() {
        assertTrue(AuthErrorMapper.mapFirebaseAuthCode("ERROR_EMAIL_ALREADY_IN_USE").contains("already exists"))
        assertTrue(AuthErrorMapper.mapFirebaseAuthCode("ERROR_WRONG_PASSWORD").contains("Invalid email or password"))
        assertTrue(AuthErrorMapper.mapFirebaseAuthCode("ERROR_TOO_MANY_REQUESTS").contains("Too many"))
    }
}
