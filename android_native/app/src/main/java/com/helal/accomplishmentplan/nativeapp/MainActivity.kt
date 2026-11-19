package com.helal.accomplishmentplan.nativeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.helal.accomplishmentplan.nativeapp.domain.logic.MonthUtils
import com.helal.accomplishmentplan.nativeapp.domain.model.AuthSessionState
import com.helal.accomplishmentplan.nativeapp.ui.auth.AuthScreen
import com.helal.accomplishmentplan.nativeapp.ui.auth.AuthViewModel
import com.helal.accomplishmentplan.nativeapp.ui.navigation.FoundationWorkspaceShell
import com.helal.accomplishmentplan.nativeapp.ui.theme.AccomplishmentPlanTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as AccomplishmentPlanApp).container

        setContent {
            AccomplishmentPlanTheme {
                val authViewModel: AuthViewModel = viewModel(
                    factory = AuthViewModel.Factory(container.authRepository)
                )
                val sessionState by authViewModel.sessionState.collectAsStateWithLifecycle()
                val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (val state = sessionState) {
                        AuthSessionState.Initializing -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        AuthSessionState.FirebaseNotConfigured,
                        AuthSessionState.Unauthenticated -> {
                            AuthScreen(
                                uiState = authUiState,
                                googleCredentialHelper = container.googleCredentialHelper,
                                onModeChange = authViewModel::setMode,
                                onFullNameChange = authViewModel::onFullNameChange,
                                onEmailChange = authViewModel::onEmailChange,
                                onPasswordChange = authViewModel::onPasswordChange,
                                onConfirmPasswordChange = authViewModel::onConfirmPasswordChange,
                                onSubmitEmailAuth = authViewModel::submitEmailAuth,
                                onGoogleSignInStarted = authViewModel::onGoogleSignInStarted,
                                onGoogleIdTokenReceived = authViewModel::onGoogleIdTokenReceived,
                                onGoogleSignInError = authViewModel::onGoogleSignInError,
                                onOpenForgotPassword = authViewModel::openForgotPasswordDialog,
                                onDismissForgotPassword = authViewModel::dismissForgotPasswordDialog,
                                onResetEmailChange = authViewModel::onResetEmailChange,
                                onSubmitPasswordReset = authViewModel::submitPasswordReset
                            )
                        }

                        is AuthSessionState.Authenticated -> {
                            val uid = state.user.uid
                            val selectedMonthFlow = remember(uid) {
                                container.userMonthPreferencesStore.observeSelectedMonthDisplay(uid)
                            }
                            val selectedMonth by selectedMonthFlow.collectAsStateWithLifecycle(
                                initialValue = MonthUtils.currentDisplayMonth()
                            )

                            LaunchedEffect(uid) {
                                container.masterDataRepository.initializeUserWorkspaceAdditive(uid)
                            }

                            FoundationWorkspaceShell(
                                userDisplayName = state.user.greetingName,
                                userEmail = state.user.email,
                                selectedMonthLabel = selectedMonth,
                                onSignOut = {
                                    authViewModel.signOut {
                                        container.googleCredentialHelper.clearCredentialState(
                                            applicationContext
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
