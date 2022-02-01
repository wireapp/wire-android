package com.wire.android.ui.authentication.login

import android.widget.Toast
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
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
import com.wire.android.R
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.textfield.WirePasswordTextField
import com.wire.android.ui.common.textfield.WirePrimaryButton
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.EMPTY
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


@Composable
fun LoginScreen() {
    val scope = rememberCoroutineScope()
    val loginViewModel: LoginViewModel = hiltViewModel()
    LoginContent(
        userIdentifier = loginViewModel.userIdentifier,
        onUserIdentifierChange = { loginViewModel.onUserIdentifierChange(it) },
        password = loginViewModel.password,
        onPasswordChange = { loginViewModel.onPasswordChange(it) },
        onLoginButtonClick = suspend { loginViewModel.navigateToConvScreen() },
        scope = scope
    )
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun LoginContent(
    userIdentifier: TextFieldValue,
    onUserIdentifierChange: (TextFieldValue) -> Unit,
    password: TextFieldValue,
    onPasswordChange: (TextFieldValue) -> Unit,
    onLoginButtonClick: suspend () -> Unit,
    scope: CoroutineScope
) {

    Scaffold(topBar = { LoginTopBar() }) {
        Column(modifier = Modifier.padding(16.dp)) {
            Column(
                modifier = Modifier.weight(1f, true),
                verticalArrangement = Arrangement.Center,
            ) {
                UserIdentifierInput(
                    modifier = Modifier.fillMaxWidth(),
                    userIdentifier = userIdentifier,
                    onUserIdentifierChange = onUserIdentifierChange
                )

                Spacer(modifier = Modifier.height(16.dp))

                PasswordInput(
                    modifier = Modifier.fillMaxWidth(),
                    password = password,
                    onPasswordChange = onPasswordChange
                )
                Spacer(modifier = Modifier.height(16.dp))
                ForgotPasswordLabel(modifier = Modifier.fillMaxWidth())
            }

            LoginButton(
                modifier = Modifier.fillMaxWidth(),
                userIdentifier = userIdentifier.text,
                password = password.text
            ) {
                scope.launch {
                    onLoginButtonClick()
                }
            }
        }
    }
}

@Composable
private fun UserIdentifierInput(modifier: Modifier, userIdentifier: TextFieldValue, onUserIdentifierChange: (TextFieldValue) -> Unit) {
    WireTextField(
        value = userIdentifier,
        onValueChange = onUserIdentifierChange,
        placeholderText = stringResource(R.string.login_user_identifier_placeholder),
        labelText = stringResource(R.string.login_user_identifier_label),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
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
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
        modifier = modifier,
    )
}

@Composable
private fun ForgotPasswordLabel(modifier: Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        val context = LocalContext.current
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
                    onClick = { Toast.makeText(context, "Forgot password click 💥", Toast.LENGTH_SHORT).show() } //TODO
                )
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun LoginButton(modifier: Modifier, userIdentifier: String, password: String, onClick: () -> Unit) {
    var isLoading by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    Column(modifier = modifier) {
        val enabled = validInput(userIdentifier, password) && !isLoading
        val text = if (isLoading) stringResource(R.string.label_logging_in) else stringResource(R.string.label_login)

        WirePrimaryButton(
            text = text,
            onClick = {
                isLoading = true
                onClick()
            },
            state = if (enabled) WireButtonState.Default else WireButtonState.Disabled,
            loading = isLoading,
            interactionSource = interactionSource,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun validInput(userIdentifier: String, password: String): Boolean =
    userIdentifier.isNotEmpty() && password.isNotEmpty()

@Preview
@Composable
fun LoginScreenPreview() {
    val scope = rememberCoroutineScope()
    var userIdentifier by remember { mutableStateOf(TextFieldValue(String.EMPTY)) }
    var password by remember { mutableStateOf(TextFieldValue(String.EMPTY)) }
    WireTheme(useDarkColors = false, isPreview = true) {
        LoginContent(
            userIdentifier = userIdentifier,
            onUserIdentifierChange = {userIdentifier = it},
            password = password,
            onPasswordChange = { password = it },
            onLoginButtonClick = suspend {  },
            scope = scope
        )
    }
}
