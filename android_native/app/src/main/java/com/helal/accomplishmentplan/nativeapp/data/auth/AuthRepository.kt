package com.helal.accomplishmentplan.nativeapp.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.helal.accomplishmentplan.nativeapp.core.firebase.FirebaseConfigStatus
import com.helal.accomplishmentplan.nativeapp.core.session.SessionCleanupCoordinator
import com.helal.accomplishmentplan.nativeapp.domain.model.AuthSessionState
import com.helal.accomplishmentplan.nativeapp.domain.model.AuthUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

interface AuthRepository {
    val configStatus: FirebaseConfigStatus
    val sessionState: StateFlow<AuthSessionState>

    suspend fun signInWithEmail(email: String, password: String): Result<AuthUser>
    suspend fun registerWithEmail(fullName: String, email: String, password: String): Result<AuthUser>
    suspend fun signInWithGoogleIdToken(idToken: String): Result<AuthUser>
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    suspend fun signOut(clearCredentialState: suspend () -> Unit = {}): Result<Unit>
    fun dispose()
}

/**
 * Production Firebase Authentication repository supporting:
 * - Email/Password Login & Registration
 * - Google Sign-In via Credential Manager ID Token exchange
 * - Automatic FirebaseAuth session persistence via AuthStateListener
 * - Safe sign-out ordering: cancels active Firestore listeners BEFORE signing out
 *
 * Anonymous authentication is strictly prohibited and never invoked.
 */
class FirebaseAuthRepository(
    override val configStatus: FirebaseConfigStatus,
    private val sessionCleanupCoordinator: SessionCleanupCoordinator,
    private val authProvider: () -> FirebaseAuth = { FirebaseAuth.getInstance() }
) : AuthRepository {

    private val _sessionState = MutableStateFlow<AuthSessionState>(
        if (configStatus.isFirebaseInitialized) {
            AuthSessionState.Initializing
        } else {
            AuthSessionState.FirebaseNotConfigured
        }
    )
    override val sessionState: StateFlow<AuthSessionState> = _sessionState.asStateFlow()

    private var firebaseAuth: FirebaseAuth? = null
    private var authStateListener: FirebaseAuth.AuthStateListener? = null

    init {
        if (configStatus.isFirebaseInitialized) {
            val auth = authProvider()
            firebaseAuth = auth
            _sessionState.value = auth.currentUser?.toAuthUser()?.let {
                AuthSessionState.Authenticated(it)
            } ?: AuthSessionState.Unauthenticated

            val listener = FirebaseAuth.AuthStateListener { instance ->
                val user = instance.currentUser
                _sessionState.value = if (user != null) {
                    AuthSessionState.Authenticated(user.toAuthUser())
                } else {
                    AuthSessionState.Unauthenticated
                }
            }
            authStateListener = listener
            auth.addAuthStateListener(listener)
        }
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<AuthUser> {
        val auth = requireAuth() ?: return Result.failure(notConfiguredError())
        return runAuthCall {
            val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
            val user = result.user ?: error("Sign-in succeeded but Firebase user was null.")
            user.toAuthUser()
        }
    }

    override suspend fun registerWithEmail(
        fullName: String,
        email: String,
        password: String
    ): Result<AuthUser> {
        val auth = requireAuth() ?: return Result.failure(notConfiguredError())
        return runAuthCall {
            val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
            val user = result.user ?: error("Registration succeeded but Firebase user was null.")
            val trimmedName = fullName.trim()
            if (trimmedName.isNotEmpty()) {
                val profileUpdate = UserProfileChangeRequest.Builder()
                    .setDisplayName(trimmedName)
                    .build()
                user.updateProfile(profileUpdate).await()
                user.reload().await()
            }
            (auth.currentUser ?: user).toAuthUser().also { updated ->
                _sessionState.value = AuthSessionState.Authenticated(updated)
            }
        }
    }

    override suspend fun signInWithGoogleIdToken(idToken: String): Result<AuthUser> {
        val auth = requireAuth() ?: return Result.failure(notConfiguredError())
        return runAuthCall {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user ?: error("Google Sign-In succeeded but Firebase user was null.")
            user.toAuthUser()
        }
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        val auth = requireAuth() ?: return Result.failure(notConfiguredError())
        return runAuthCall {
            auth.sendPasswordResetEmail(email.trim()).await()
        }
    }

    override suspend fun signOut(clearCredentialState: suspend () -> Unit): Result<Unit> {
        return runAuthCall {
            // 1. Always stop all user-scoped Firestore snapshot listeners first
            sessionCleanupCoordinator.cleanupAll()
            // 2. Clear Android Credential Manager state
            clearCredentialState()
            // 3. Sign out from FirebaseAuth
            firebaseAuth?.signOut()
            if (configStatus.isFirebaseInitialized) {
                _sessionState.value = AuthSessionState.Unauthenticated
            }
        }
    }

    override fun dispose() {
        val auth = firebaseAuth
        val listener = authStateListener
        if (auth != null && listener != null) {
            auth.removeAuthStateListener(listener)
        }
        authStateListener = null
    }

    private fun requireAuth(): FirebaseAuth? = firebaseAuth

    private fun notConfiguredError(): IllegalStateException = IllegalStateException(
        "Firebase is not configured yet. Add google-services.json for com.helal.accomplishmentplan.nativeapp in Firebase project ${configStatus.expectedProjectId}."
    )

    private suspend inline fun <T> runAuthCall(block: () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    private fun FirebaseUser.toAuthUser(): AuthUser = AuthUser(
        uid = uid,
        email = email.orEmpty(),
        displayName = displayName.orEmpty(),
        photoUrl = photoUrl?.toString(),
        providerIds = providerData.mapNotNull { it.providerId }.distinct(),
        isEmailVerified = isEmailVerified
    )
}
