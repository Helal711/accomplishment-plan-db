package com.helal.accomplishmentplan.nativeapp.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.helal.accomplishmentplan.nativeapp.R
import com.helal.accomplishmentplan.nativeapp.data.auth.GoogleCredentialHelper
import com.helal.accomplishmentplan.nativeapp.ui.components.BlueGradientHeader
import com.helal.accomplishmentplan.nativeapp.ui.components.GradientPrimaryButton
import com.helal.accomplishmentplan.nativeapp.ui.components.InfoBannerCard
import com.helal.accomplishmentplan.nativeapp.ui.theme.LocalExtendedColors
import com.helal.accomplishmentplan.nativeapp.ui.theme.PureWhite
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    uiState: AuthUiState,
    googleCredentialHelper: GoogleCredentialHelper,
    onModeChange: (AuthMode) -> Unit,
    onFullNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onSubmitEmailAuth: () -> Unit,
    onGoogleSignInStarted: () -> Unit,
    onGoogleIdTokenReceived: (String) -> Unit,
    onGoogleSignInError: (Throwable) -> Unit,
    onOpenForgotPassword: () -> Unit,
    onDismissForgotPassword: () -> Unit,
    onResetEmailChange: (String) -> Unit,
    onSubmitPasswordReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val extendedColors = LocalExtendedColors.current
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BlueGradientHeader {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = PureWhite.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, PureWhite.copy(alpha = 0.28f))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_art),
                        contentDescription = stringResource(R.string.cd_app_logo),
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = PureWhite
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.app_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = PureWhite.copy(alpha = 0.85f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = if (uiState.mode == AuthMode.SIGN_IN) {
                    stringResource(R.string.auth_welcome_title)
                } else {
                    stringResource(R.string.auth_create_account_title)
                },
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = PureWhite
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (uiState.mode == AuthMode.SIGN_IN) {
                    stringResource(R.string.auth_welcome_subtitle)
                } else {
                    stringResource(R.string.auth_create_account_subtitle)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = PureWhite.copy(alpha = 0.88f)
            )
        }

        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (!uiState.configStatus.isFirebaseInitialized) {
                InfoBannerCard(
                    title = stringResource(R.string.auth_firebase_pending_title),
                    message = stringResource(R.string.auth_firebase_pending_body),
                    icon = Icons.Filled.Info,
                    containerColor = extendedColors.warningContainer,
                    contentColor = extendedColors.warning,
                    modifier = Modifier.testTag("firebase_config_pending_banner")
                )
            }

            AnimatedVisibility(visible = uiState.errorMessage != null) {
                uiState.errorMessage?.let { message ->
                    InfoBannerCard(
                        title = "Authentication Notice",
                        message = message,
                        icon = Icons.Filled.ErrorOutline,
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag("auth_error_banner")
                    )
                }
            }

            AnimatedVisibility(visible = uiState.infoMessage != null) {
                uiState.infoMessage?.let { message ->
                    InfoBannerCard(
                        title = "Success",
                        message = message,
                        icon = Icons.Filled.CheckCircle,
                        containerColor = extendedColors.successContainer,
                        contentColor = extendedColors.success,
                        modifier = Modifier.testTag("auth_info_banner")
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Mode Toggle
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ModeTogglePill(
                                label = stringResource(R.string.auth_tab_sign_in),
                                selected = uiState.mode == AuthMode.SIGN_IN,
                                onClick = { onModeChange(AuthMode.SIGN_IN) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("auth_mode_sign_in")
                            )
                            ModeTogglePill(
                                label = stringResource(R.string.auth_tab_register),
                                selected = uiState.mode == AuthMode.REGISTER,
                                onClick = { onModeChange(AuthMode.REGISTER) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("auth_mode_register")
                            )
                        }
                    }

                    if (uiState.mode == AuthMode.REGISTER) {
                        OutlinedTextField(
                            value = uiState.fullName,
                            onValueChange = onFullNameChange,
                            label = { Text(stringResource(R.string.auth_label_full_name)) },
                            leadingIcon = {
                                Icon(Icons.Filled.Person, contentDescription = null)
                            },
                            isError = uiState.fullNameError != null,
                            supportingText = uiState.fullNameError?.let { error ->
                                { Text(text = error) }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_full_name_input")
                        )
                    }

                    OutlinedTextField(
                        value = uiState.email,
                        onValueChange = onEmailChange,
                        label = { Text(stringResource(R.string.auth_label_email)) },
                        leadingIcon = {
                            Icon(Icons.Filled.Email, contentDescription = null)
                        },
                        isError = uiState.emailError != null,
                        supportingText = uiState.emailError?.let { error ->
                            { Text(text = error) }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_email_input")
                    )

                    OutlinedTextField(
                        value = uiState.password,
                        onValueChange = onPasswordChange,
                        label = { Text(stringResource(R.string.auth_label_password)) },
                        leadingIcon = {
                            Icon(Icons.Filled.Lock, contentDescription = null)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) {
                                        Icons.Filled.VisibilityOff
                                    } else {
                                        Icons.Filled.Visibility
                                    },
                                    contentDescription = stringResource(R.string.cd_toggle_password)
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        isError = uiState.passwordError != null,
                        supportingText = uiState.passwordError?.let { error ->
                            { Text(text = error) }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = if (uiState.mode == AuthMode.SIGN_IN) {
                                ImeAction.Done
                            } else {
                                ImeAction.Next
                            }
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { if (uiState.mode == AuthMode.SIGN_IN) onSubmitEmailAuth() }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_password_input")
                    )

                    if (uiState.mode == AuthMode.REGISTER) {
                        OutlinedTextField(
                            value = uiState.confirmPassword,
                            onValueChange = onConfirmPasswordChange,
                            label = { Text(stringResource(R.string.auth_label_confirm_password)) },
                            leadingIcon = {
                                Icon(Icons.Filled.Lock, contentDescription = null)
                            },
                            visualTransformation = if (passwordVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            isError = uiState.confirmPasswordError != null,
                            supportingText = uiState.confirmPasswordError?.let { error ->
                                { Text(text = error) }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { onSubmitEmailAuth() }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_confirm_password_input")
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            TextButton(
                                onClick = onOpenForgotPassword,
                                enabled = !uiState.isAnyActionLoading,
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .testTag("auth_forgot_password_button")
                            ) {
                                Text(text = stringResource(R.string.auth_action_forgot_password))
                            }
                        }
                    }

                    GradientPrimaryButton(
                        text = if (uiState.mode == AuthMode.SIGN_IN) {
                            stringResource(R.string.auth_action_sign_in)
                        } else {
                            stringResource(R.string.auth_action_register)
                        },
                        onClick = onSubmitEmailAuth,
                        enabled = !uiState.isAnyActionLoading,
                        loading = uiState.isLoading,
                        leadingIcon = if (uiState.mode == AuthMode.SIGN_IN) {
                            Icons.AutoMirrored.Filled.Login
                        } else {
                            Icons.Filled.PersonAdd
                        },
                        modifier = Modifier.testTag("auth_submit_button")
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f))
                        Text(
                            text = stringResource(R.string.auth_or_continue_with),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f))
                    }

                    OutlinedButton(
                        onClick = {
                            val webClientId = uiState.configStatus.googleWebClientId
                            if (!uiState.configStatus.isReadyForGoogleSignIn || webClientId == null) {
                                onGoogleSignInError(
                                    IllegalStateException(
                                        "Google Sign-In requires google-services.json with OAuth Web Client ID for package com.helal.accomplishmentplan.nativeapp."
                                    )
                                )
                                return@OutlinedButton
                            }
                            onGoogleSignInStarted()
                            coroutineScope.launch {
                                googleCredentialHelper.requestGoogleIdToken(
                                    activityContext = context,
                                    serverClientId = webClientId
                                ).fold(
                                    onSuccess = { token -> onGoogleIdTokenReceived(token) },
                                    onFailure = { error -> onGoogleSignInError(error) }
                                )
                            }
                        },
                        enabled = !uiState.isAnyActionLoading,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("auth_google_sign_in_button")
                    ) {
                        if (uiState.isGoogleLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.AccountCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.auth_action_google),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }

    if (uiState.showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = onDismissForgotPassword,
            title = { Text(text = stringResource(R.string.auth_reset_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.auth_reset_message),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = uiState.resetEmailInput,
                        onValueChange = onResetEmailChange,
                        label = { Text(stringResource(R.string.auth_label_email)) },
                        isError = uiState.resetEmailError != null,
                        supportingText = uiState.resetEmailError?.let { error ->
                            { Text(text = error) }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_reset_email_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = onSubmitPasswordReset,
                    enabled = !uiState.isLoading,
                    modifier = Modifier.testTag("auth_reset_confirm_button")
                ) {
                    Text(text = stringResource(R.string.auth_reset_send))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissForgotPassword) {
                    Text(text = stringResource(R.string.auth_cancel))
                }
            }
        )
    }
}

@Composable
private fun ModeTogglePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = if (selected) 2.dp else 0.dp,
        modifier = modifier.height(48.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}
