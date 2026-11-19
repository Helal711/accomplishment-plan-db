package com.helal.accomplishmentplan.nativeapp.ui.auth

import com.helal.accomplishmentplan.nativeapp.core.firebase.FirebaseConfigStatus
import com.helal.accomplishmentplan.nativeapp.core.session.SessionCleanupCoordinator
import com.helal.accomplishmentplan.nativeapp.data.auth.AuthRepository
import com.helal.accomplishmentplan.nativeapp.domain.model.AuthSessionState
import com.helal.accomplishmentplan.nativeapp.domain.model.AuthUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `email sign-in transitions session state to Authenticated and clears password`() = runTest {
        val repo = FakeAuthRepository()
        val viewModel = AuthViewModel(repo)

        viewModel.onEmailChange("planner@company.com")
        viewModel.onPasswordChange("secret123")
        viewModel.submitEmailAuth()
        advanceUntilIdle()

        val session = viewModel.sessionState.value
        assertTrue(session is AuthSessionState.Authenticated)
        assertEquals("planner@company.com", (session as AuthSessionState.Authenticated).user.email)
        assertEquals("", viewModel.uiState.value.password)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `registration validates mismatched passwords before invoking repository`() = runTest {
        val repo = FakeAuthRepository()
        val viewModel = AuthViewModel(repo)

        viewModel.setMode(AuthMode.REGISTER)
        viewModel.onFullNameChange("Helal")
        viewModel.onEmailChange("helal@example.com")
        viewModel.onPasswordChange("pass1234")
        viewModel.onConfirmPasswordChange("different")
        viewModel.submitEmailAuth()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.confirmPasswordError)
        assertEquals(0, repo.registerCalls)
    }

    @Test
    fun `google id token sign-in authenticates user`() = runTest {
        val repo = FakeAuthRepository()
        val viewModel = AuthViewModel(repo)

        viewModel.onGoogleIdTokenReceived("fake-id-token")
        advanceUntilIdle()

        val session = viewModel.sessionState.value
        assertTrue(session is AuthSessionState.Authenticated)
        assertEquals("google-user-1", (session as AuthSessionState.Authenticated).user.uid)
    }

    @Test
    fun `signOut cleans up Firestore listeners before clearing credentials and signing out`() = runTest {
        val callOrder = mutableListOf<String>()
        val coordinator = SessionCleanupCoordinator().apply {
            register("tasks_listener") { callOrder += "listeners_stopped" }
        }
        val repo = FakeAuthRepository(coordinator, callOrder)
        val viewModel = AuthViewModel(repo)

        viewModel.onGoogleIdTokenReceived("fake-id-token")
        advanceUntilIdle()

        viewModel.signOut { callOrder += "credentials_cleared" }
        advanceUntilIdle()

        assertEquals(AuthSessionState.Unauthenticated, viewModel.sessionState.value)
        assertEquals(
            listOf("listeners_stopped", "credentials_cleared", "firebase_signed_out"),
            callOrder
        )
    }

    private class FakeAuthRepository(
        private val coordinator: SessionCleanupCoordinator = SessionCleanupCoordinator(),
        private val events: MutableList<String> = mutableListOf()
    ) : AuthRepository {
        override val configStatus = FirebaseConfigStatus(
            hasGoogleServicesFile = true,
            isFirebaseInitialized = true,
            googleWebClientId = "test-web-client-id",
            expectedProjectId = "accomplishment-plan-db"
        )

        private val _sessionState = MutableStateFlow<AuthSessionState>(AuthSessionState.Unauthenticated)
        override val sessionState: StateFlow<AuthSessionState> = _sessionState.asStateFlow()

        var registerCalls = 0

        override suspend fun signInWithEmail(email: String, password: String): Result<AuthUser> {
            val user = AuthUser(uid = "email-user-1", email = email.trim(), displayName = "Planner")
            _sessionState.value = AuthSessionState.Authenticated(user)
            return Result.success(user)
        }

        override suspend fun registerWithEmail(
            fullName: String,
            email: String,
            password: String
        ): Result<AuthUser> {
            registerCalls++
            val user = AuthUser(uid = "email-user-2", email = email.trim(), displayName = fullName.trim())
            _sessionState.value = AuthSessionState.Authenticated(user)
            return Result.success(user)
        }

        override suspend fun signInWithGoogleIdToken(idToken: String): Result<AuthUser> {
            val user = AuthUser(
                uid = "google-user-1",
                email = "google@example.com",
                displayName = "Google Planner"
            )
            _sessionState.value = AuthSessionState.Authenticated(user)
            return Result.success(user)
        }

        override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = Result.success(Unit)

        override suspend fun signOut(clearCredentialState: suspend () -> Unit): Result<Unit> {
            coordinator.cleanupAll()
            clearCredentialState()
            events += "firebase_signed_out"
            _sessionState.value = AuthSessionState.Unauthenticated
            return Result.success(Unit)
        }

        override fun dispose() = Unit
    }
}
