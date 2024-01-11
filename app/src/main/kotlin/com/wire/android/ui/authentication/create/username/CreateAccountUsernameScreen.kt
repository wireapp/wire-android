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

package com.wire.android.ui.authentication.create.username

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import com.wire.android.ui.common.scaffold.WireScaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.wire.android.R
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.ui.authentication.create.common.handle.UsernameTextField
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.destinations.InitialSyncScreenDestination
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@RootNavGraph
@Destination
@Composable
fun CreateAccountUsernameScreen(
    navigator: Navigator,
    viewModel: CreateAccountUsernameViewModel = hiltViewModel()
) {
    UsernameContent(
        state = viewModel.state,
        onUsernameChange = viewModel::onUsernameChange,
        onContinuePressed = {
            viewModel.onContinue {
                navigator.navigate(NavigationCommand(InitialSyncScreenDestination, BackStackMode.CLEAR_WHOLE))
            }
        },
        onErrorDismiss = viewModel::onErrorDismiss,
        onUsernameErrorAnimated = viewModel::onUsernameErrorAnimated
    )
}

@Composable
private fun UsernameContent(
    state: CreateAccountUsernameViewState,
    onUsernameChange: (TextFieldValue) -> Unit,
    onContinuePressed: () -> Unit,
    onErrorDismiss: () -> Unit,
    onUsernameErrorAnimated: () -> Unit
) {
    WireScaffold(
        topBar = {
            WireCenterAlignedTopAppBar(
                elevation = 0.dp,
                title = stringResource(id = R.string.create_account_username_title),
                navigationIconType = null
            )
        },
    ) { internalPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier.padding(internalPadding)
        ) {
            Text(
                text = stringResource(id = R.string.create_account_username_text),
                style = MaterialTheme.wireTypography.body01,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = MaterialTheme.wireDimensions.spacing16x,
                        vertical = MaterialTheme.wireDimensions.spacing24x
                    )
            )

            UsernameTextField(
                username = state.username,
                errorState = state.error,
                animateUsernameError = state.animateUsernameError,
                onUsernameChange = onUsernameChange,
                onUsernameErrorAnimated = onUsernameErrorAnimated,
                onErrorDismiss = onErrorDismiss,
            )

            Spacer(modifier = Modifier.weight(1f))
            WirePrimaryButton(
                text = stringResource(R.string.label_confirm),
                onClick = onContinuePressed,
                fillMaxWidth = true,
                loading = state.loading,
                state = if (state.continueEnabled) WireButtonState.Default else WireButtonState.Disabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.wireDimensions.spacing16x)
            )
        }
    }
}

@Composable
@Preview
private fun PreviewCreateAccountUsernameScreen() {
    UsernameContent(CreateAccountUsernameViewState(), {}, {}, {}, {})
}
