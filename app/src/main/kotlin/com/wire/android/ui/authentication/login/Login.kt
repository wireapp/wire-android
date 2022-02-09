package com.wire.android.ui.authentication.login

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.wire.android.BuildConfig
import com.wire.android.R
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.textfield.WirePasswordTextField
import com.wire.android.ui.common.textfield.WirePrimaryButton
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.DialogErrorStrings
import com.wire.android.util.dialogErrorStrings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.wire.kalium.logic.configuration.ServerConfig

@ExperimentalMaterialApi
@Composable
fun LoginScreen(
    navController: NavController,
    serverConfig: ServerConfig
) {
    val scope = rememberCoroutineScope()
    val loginViewModel: LoginViewModel = hiltViewModel()
    val loginState: LoginState = loginViewModel.loginState
    LoginContent(
        navController = navController,
        loginState = loginState,
        onUserIdentifierChange = { loginViewModel.onUserIdentifierChange(it) },
        onPasswordChange = { loginViewModel.onPasswordChange(it) },
        onDialogDismiss = { loginViewModel.onDialogDismissed() },
        onLoginButtonClick = suspend { loginViewModel.login(serverConfig) },
        scope = scope
    )
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun LoginContent(
    navController: NavController,
    loginState: LoginState,
    onUserIdentifierChange: (TextFieldValue) -> Unit,
    onPasswordChange: (TextFieldValue) -> Unit,
    onDialogDismiss: () -> Unit,
    onLoginButtonClick: suspend () -> Unit,
    scope: CoroutineScope
) {
    Scaffold(
        topBar = { LoginTopBar(onBackNavigationPressed = { navController.popBackStack() }) }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Column(
                modifier = Modifier.weight(1f, true),
                verticalArrangement = Arrangement.Center,
            ) {
                UserIdentifierInput(
                    modifier = Modifier.fillMaxWidth(),
                    userIdentifier = loginState.userIdentifier,
                    onUserIdentifierChange = onUserIdentifierChange,
                    error = when (loginState.loginError) {
                        LoginError.TextFieldError.InvalidUserIdentifierError -> stringResource(R.string.login_error_invalid_user_identifier)
                        else -> null
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                PasswordInput(
                    modifier = Modifier.fillMaxWidth(),
                    password = loginState.password,
                    onPasswordChange = onPasswordChange
                )
                Spacer(modifier = Modifier.height(16.dp))
                ForgotPasswordLabel(modifier = Modifier.fillMaxWidth())
            }

            LoginButton(
                modifier = Modifier.fillMaxWidth(),
                loading = loginState.loading,
                enabled = loginState.loginEnabled
            ) {
                scope.launch {
                    onLoginButtonClick()
                }
            }
        }

        if (loginState.loginError is LoginError.DialogError) {
            val (title, message) = when (loginState.loginError) {
                LoginError.DialogError.InvalidCredentialsError -> DialogErrorStrings(
                    stringResource(id = R.string.login_error_invalid_credentials_title),
                    stringResource(id = R.string.login_error_invalid_credentials_message)
                )
                is LoginError.DialogError.GenericError ->
                    loginState.loginError.coreFailure.dialogErrorStrings(LocalContext.current.resources)
            }
            WireDialog(
                title = title,
                text = message,
                onDismiss = onDialogDismiss,
                confirmButtonProperties = WireDialogButtonProperties(
                    onClick = onDialogDismiss,
                    text = stringResource(id = R.string.label_ok),
                    type = WireDialogButtonType.Primary,
                )
            )
        }
    }
}

@Composable
private fun UserIdentifierInput(
    modifier: Modifier,
    userIdentifier: TextFieldValue,
    error: String?,
    onUserIdentifierChange: (TextFieldValue) -> Unit
) {
    WireTextField(
        value = userIdentifier,
        onValueChange = onUserIdentifierChange,
        placeholderText = stringResource(R.string.login_user_identifier_placeholder),
        labelText = stringResource(R.string.login_user_identifier_label),
        state = if (error != null) WireTextFieldState.Error(error) else WireTextFieldState.Default,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        modifier = modifier,
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun PasswordInput(modifier: Modifier, password: TextFieldValue, onPasswordChange: (TextFieldValue) -> Unit) {
    val keyboardController = LocalSoftwareKeyboardController.current
    WirePasswordTextField(
        value = password,
        onValueChange = onPasswordChange,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
        modifier = modifier,
    )
}

@Composable
private fun ForgotPasswordLabel(modifier: Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        val context = LocalContext.current
        val backgroundColor = MaterialTheme.colorScheme.background
        Text(
            text = stringResource(R.string.login_forgot_password),
            style = MaterialTheme.wireTypography.body02.copy(
                textDecoration = TextDecoration.Underline,
                color = MaterialTheme.colorScheme.primary
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        // TODO: refactor this to open the browser
                        openForgotPasswordPage(context, backgroundColor.toArgb())
                    }
                )
        )
    }
}

private fun openForgotPasswordPage(context: Context, @ColorInt color: Int) {
    // TODO: get the link from the serverConfig
    val url = "${BuildConfig.ACCOUNTS_URL}/forgot"

    // TODO: extract the custom tab code to it's own destination
    val builder = CustomTabsIntent.Builder()
    val colors = CustomTabColorSchemeParams.Builder()
        .setNavigationBarColor(color)
        .setToolbarColor(color)
        .build()
    builder.setDefaultColorSchemeParams(colors)
    builder.setCloseButtonIcon(BitmapFactory.decodeResource(context.resources, R.drawable.ic_close))
    builder.setShareState(CustomTabsIntent.SHARE_STATE_OFF)
    builder.setShowTitle(true)
    val customTabsIntent = builder.build()
    customTabsIntent.intent.putExtra(Intent.EXTRA_REFERRER, Uri.parse("android-app://" + context.packageName))
    customTabsIntent.launchUrl(context, Uri.parse(url))
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun LoginButton(modifier: Modifier, loading: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(modifier = modifier) {
        val text = if (loading) stringResource(R.string.label_logging_in) else stringResource(R.string.label_login)
        WirePrimaryButton(
            text = text,
            onClick = onClick,
            state = if (enabled) WireButtonState.Default else WireButtonState.Disabled,
            loading = loading,
            interactionSource = interactionSource,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview
@Composable
private fun LoginScreenPreview() {
    val scope = rememberCoroutineScope()
    WireTheme(isPreview = true) {
        LoginContent(
            navController = rememberNavController(),
            loginState = LoginState(),
            onUserIdentifierChange = { },
            onPasswordChange = { },
            onDialogDismiss = { },
            onLoginButtonClick = suspend { },
            scope = scope
        )
    }
}
