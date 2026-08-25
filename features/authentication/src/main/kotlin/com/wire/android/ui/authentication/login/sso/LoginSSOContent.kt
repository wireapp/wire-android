/* Wire Copyright (C) 2026 Wire Swiss GmbH */
package com.wire.android.ui.authentication.login.sso

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.wire.android.feature.authentication.R
import com.wire.android.ui.authentication.login.email.LoginButtonContent
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.theme.wireDimensions

@Immutable
data class LoginSSOPresentationState(
    val loading: Boolean = false,
    val loginEnabled: Boolean = false,
    val invalidCode: Boolean = false,
)

@Immutable
data class LoginSSOText(val login: String, val loggingIn: String)

@Composable
fun LoginSSOContent(
    state: LoginSSOPresentationState,
    text: LoginSSOText,
    ssoCodeTextState: TextFieldState,
    onLoginButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
) {
    Column(
        modifier = modifier.fillMaxHeight().verticalScroll(scrollState).padding(MaterialTheme.wireDimensions.spacing16x),
    ) {
        Spacer(Modifier.height(MaterialTheme.wireDimensions.spacing32x))
        WireTextField(
            textState = ssoCodeTextState,
            labelText = stringResource(R.string.login_sso_code_label),
            semanticDescription = stringResource(R.string.content_description_login_sso_code_field),
            state = if (state.invalidCode) {
                WireTextFieldState.Error(stringResource(R.string.login_error_invalid_sso_code_format))
            } else {
                WireTextFieldState.Default
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth().padding(bottom = MaterialTheme.wireDimensions.spacing16x).testTag("ssoCodeField"),
        )
        Spacer(Modifier.weight(1f))
        LoginButtonContent(
            loading = state.loading,
            enabled = state.loginEnabled,
            onClick = onLoginButtonClick,
            modifier = Modifier.fillMaxWidth().testTag("ssoLoginButton"),
            text = text.login,
            loadingText = text.loggingIn,
        )
    }
}
