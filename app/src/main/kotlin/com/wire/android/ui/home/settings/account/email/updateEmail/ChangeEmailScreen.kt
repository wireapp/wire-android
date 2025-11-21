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
package com.wire.android.ui.home.settings.account.email.updateEmail
import com.ramcosta.composedestinations.annotation.Destination
import com.wire.android.navigation.WireRootNavGraph

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.spec.DestinationStyle
import com.wire.android.R
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.style.SlideNavigationAnimation
import com.wire.android.ui.common.Icon
import com.wire.android.ui.common.button.WireButtonState.Default
import com.wire.android.ui.common.button.WireButtonState.Disabled
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.rememberBottomBarElevationState
import com.wire.android.ui.common.rememberTopBarElevationState
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.textfield.DefaultEmailDone
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.common.textfield.forceLowercase
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.destinations.VerifyEmailScreenDestination
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes

@Destination<WireRootNavGraph>(
    style = SlideNavigationAnimation::class,
)
@Composable
fun ChangeEmailScreen(
    navigator: Navigator,
    viewModel: ChangeEmailViewModel = hiltViewModel()
) {
    when (val flowState = viewModel.state.flowState) {
        is ChangeEmailState.FlowState.NoChange,
        is ChangeEmailState.FlowState.Error.SelfUserNotFound -> navigator.navigateBack()

        is ChangeEmailState.FlowState.Success ->
            navigator.navigate(
                NavigationCommand(
                    VerifyEmailScreenDestination(flowState.newEmail),
                    BackStackMode.REMOVE_CURRENT
                )
            )

        else ->
            ChangeEmailContent(
                textState = viewModel.textState,
                state = viewModel.state,
                onBackPressed = navigator::navigateBack,
                onSaveClicked = viewModel::onSaveClicked
            )
    }
}

@Composable
fun ChangeEmailContent(
    textState: TextFieldState,
    state: ChangeEmailState,
    onSaveClicked: () -> Unit,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    WireScaffold(modifier = modifier, topBar = {
        WireCenterAlignedTopAppBar(
            elevation = scrollState.rememberTopBarElevationState().value,
            onNavigationPressed = onBackPressed,
            title = stringResource(id = R.string.create_account_email_title)
        )
    }) { internalPadding ->
        Column(
            modifier = Modifier
                .padding(internalPadding)
                .fillMaxSize()
        ) {

            Column(
                modifier = Modifier
                    .weight(weight = 1f, fill = true)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                val keyboardController = LocalSoftwareKeyboardController.current
                Text(
                    text = stringResource(id = R.string.settings_myaccount_email_description),
                    style = MaterialTheme.wireTypography.body01,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = MaterialTheme.wireDimensions.spacing16x,
                            vertical = MaterialTheme.wireDimensions.spacing16x
                        )
                )

                Spacer(modifier = Modifier.weight(0.5f))

                Box {
                    WireTextField(
                        textState = textState,
                        labelText = stringResource(R.string.email_label).uppercase(),
                        inputTransformation = InputTransformation.forceLowercase(),
                        state = computeEmailErrorState(state.flowState),
                        keyboardOptions = KeyboardOptions.DefaultEmailDone,
                        onKeyboardAction = { keyboardController?.hide() },
                        modifier = Modifier.padding(
                            horizontal = MaterialTheme.wireDimensions.spacing16x
                        )
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
            }

            Surface(
                shadowElevation = scrollState.rememberBottomBarElevationState().value,
                color = MaterialTheme.wireColorScheme.background
            ) {
                Box(modifier = Modifier.padding(MaterialTheme.wireDimensions.spacing16x)) {
                    WirePrimaryButton(
                        text = stringResource(R.string.label_save),
                        onClick = onSaveClicked,
                        fillMaxWidth = true,
                        trailingIcon = Icons.Filled.ChevronRight.Icon(),
                        state = if (state.saveEnabled) Default else Disabled,
                        loading = state.flowState is ChangeEmailState.FlowState.Loading,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun computeEmailErrorState(state: ChangeEmailState.FlowState): WireTextFieldState =
    when (state) {
        ChangeEmailState.FlowState.Error.TextFieldError.AlreadyInUse -> WireTextFieldState.Error(
            stringResource(id = R.string.settings_myaccount_email_already_in_use_error)
        )

        ChangeEmailState.FlowState.Error.TextFieldError.InvalidEmail -> WireTextFieldState.Error(
            stringResource(id = R.string.settings_myaccount_email_invalid_imail_error)
        )

        ChangeEmailState.FlowState.Error.TextFieldError.Generic -> WireTextFieldState.Error(
            stringResource(id = R.string.settings_myaccount_email_generic_error)
        )

        ChangeEmailState.FlowState.Loading -> WireTextFieldState.ReadOnly

        else -> WireTextFieldState.Default
    }

@PreviewMultipleThemes
@Composable
fun PreviewChangeEmailName() = WireTheme {
    ChangeEmailContent(
        textState = TextFieldState(),
        state = ChangeEmailState(),
        onBackPressed = { },
        onSaveClicked = { },
    )
}
