/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android.ui.home.appLock.unlock

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.input.ImeAction
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.utils.destination
import com.wire.android.R
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.annotation.app.WireDestination
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.button.WireTertiaryButton
import com.wire.android.ui.common.rememberBottomBarElevationState
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.textfield.DefaultPassword
import com.wire.android.ui.common.textfield.WirePasswordTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.destinations.AppUnlockWithBiometricsScreenDestination
import com.wire.android.ui.destinations.ForgotLockCodeScreenDestination
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes
import java.util.Locale

@WireDestination
@Composable
fun EnterLockCodeScreen(
    navigator: Navigator,
    viewModel: EnterLockScreenViewModel = hiltViewModel(),
) {
    EnterLockCodeScreenContent(
        state = viewModel.state,
        passwordTextState = viewModel.passwordTextState,
        scrollState = rememberScrollState(),
        onContinue = viewModel::onContinue,
        onForgotCodeClicked = { navigator.navigate(NavigationCommand(ForgotLockCodeScreenDestination)) }
    )
    BackHandler {
        if (navigator.navController.previousBackStackEntry?.destination() is AppUnlockWithBiometricsScreenDestination) {
            navigator.navigateBack()
        }
    }
    LaunchedEffect(viewModel.state.done) {
        if (viewModel.state.done) navigator.navigateBack()
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun EnterLockCodeScreenContent(
    state: EnterLockCodeViewState,
    passwordTextState: TextFieldState,
    scrollState: ScrollState,
    onContinue: () -> Unit,
    onForgotCodeClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WireScaffold { internalPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(internalPadding)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(weight = 1f, fill = true)
                    .verticalScroll(scrollState)
                    .padding(MaterialTheme.wireDimensions.spacing16x)
                    .semantics {
                        testTagsAsResourceId = true
                    }
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_wire_logo),
                    tint = MaterialTheme.colorScheme.onBackground,
                    contentDescription = stringResource(id = R.string.content_description_welcome_wire_logo),
                    modifier = Modifier.padding(top = MaterialTheme.wireDimensions.spacing56x)
                )

                Text(
                    text = stringResource(id = R.string.settings_enter_lock_screen_title),
                    style = MaterialTheme.wireTypography.title02,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(
                        top = MaterialTheme.wireDimensions.spacing32x,
                        bottom = MaterialTheme.wireDimensions.spacing56x
                    )
                )

                WirePasswordTextField(
                    textState = passwordTextState,
                    labelMandatoryIcon = true,
                    keyboardOptions = KeyboardOptions.DefaultPassword.copy(imeAction = ImeAction.Done),
                    modifier = Modifier
                        .testTag("password"),
                    state = when (state.error) {
                        EnterLockCodeError.InvalidValue -> WireTextFieldState.Error(
                            errorText = stringResource(R.string.settings_enter_lock_screen_wrong_passcode_label)
                        )

                        EnterLockCodeError.None -> WireTextFieldState.Default
                    },
                    autoFill = false,
                    placeholderText = stringResource(R.string.settings_set_lock_screen_passcode_label),
                    labelText = stringResource(R.string.settings_set_lock_screen_passcode_label).uppercase(
                        Locale.getDefault()
                    )
                )

                WireTertiaryButton(
                    text = stringResource(id = R.string.settings_enter_lock_screen_forgot_passcode_label),
                    onClick = onForgotCodeClicked,
                    modifier = Modifier.padding(MaterialTheme.wireDimensions.spacing16x)
                )

                Spacer(modifier = Modifier.weight(1f))
            }

            Surface(
                shadowElevation = scrollState.rememberBottomBarElevationState().value,
                color = MaterialTheme.wireColorScheme.background,
                modifier = Modifier.semantics {
                    testTagsAsResourceId = true
                }
            ) {
                Box(modifier = Modifier.padding(MaterialTheme.wireDimensions.spacing16x)) {
                    val enabled = passwordTextState.text.isNotBlank() && state.isUnlockEnabled && !state.loading
                    ContinueButton(
                        enabled = enabled,
                        onContinue = onContinue
                    )
                }
            }
        }
    }
}

@Composable
private fun ContinueButton(
    enabled: Boolean,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(modifier = modifier) {
        WirePrimaryButton(
            text = stringResource(R.string.settings_enter_lock_screen_unlock_button_label),
            onClick = onContinue,
            state = if (enabled) WireButtonState.Default else WireButtonState.Disabled,
            interactionSource = interactionSource,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("continue_button")
        )
    }
}

@Composable
@PreviewMultipleThemes
fun PreviewEnterLockCodeScreen() {
    WireTheme {
        EnterLockCodeScreenContent(
            state = EnterLockCodeViewState(),
            passwordTextState = TextFieldState(),
            scrollState = rememberScrollState(),
            onContinue = {},
            onForgotCodeClicked = {}
        )
    }
}
