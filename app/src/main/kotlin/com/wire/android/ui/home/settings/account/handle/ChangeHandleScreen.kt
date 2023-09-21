/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
package com.wire.android.ui.home.settings.account.handle

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.MaterialTheme
import com.wire.android.ui.common.scaffold.WireScaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.result.ResultBackNavigator
import com.wire.android.R
import com.wire.android.navigation.Navigator
import com.wire.android.ui.authentication.create.common.handle.UsernameTextField
import com.wire.android.ui.common.Icon
import com.wire.android.ui.common.button.WireButtonState.Default
import com.wire.android.ui.common.button.WireButtonState.Disabled
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.rememberBottomBarElevationState
import com.wire.android.ui.common.rememberTopBarElevationState
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@RootNavGraph
@Destination
@Composable
fun ChangeHandleScreen(
    navigator: Navigator,
    resultNavigator: ResultBackNavigator<Boolean>,
    viewModel: ChangeHandleViewModel = hiltViewModel()
) {
    ChangeHandleContent(
        state = viewModel.state,
        onHandleChanged = viewModel::onHandleChanged,
        onHandleErrorAnimated = viewModel::onHandleErrorAnimated,
        onBackPressed = navigator::navigateBack,
        onSaveClicked = {
            viewModel.onSaveClicked() {
                resultNavigator.setResult(true)
                resultNavigator.navigateBack()
            }
        },
        onErrorDismiss = viewModel::onErrorDismiss
    )
}

@Composable
fun ChangeHandleContent(
    state: ChangeHandleState,
    onHandleChanged: (TextFieldValue) -> Unit,
    onSaveClicked: () -> Unit,
    onHandleErrorAnimated: () -> Unit,
    onErrorDismiss: () -> Unit,
    onBackPressed: () -> Unit,
) {
    val scrollState = rememberScrollState()
    WireScaffold(topBar = {
        WireCenterAlignedTopAppBar(
            elevation = scrollState.rememberTopBarElevationState().value,
            onNavigationPressed = onBackPressed,
            title = stringResource(id = R.string.create_account_handle_title)
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

                Text(
                    text = stringResource(id = R.string.settings_myaccount_handle_description),
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
                    UsernameTextField(
                        username = state.handle,
                        errorState = state.error,
                        onUsernameChange = onHandleChanged,
                        animateUsernameError = state.animatedHandleError,
                        onUsernameErrorAnimated = onHandleErrorAnimated,
                        onErrorDismiss = onErrorDismiss

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
                        state = if (state.isSaveButtonEnabled) Default else Disabled,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewChangeHandleContent() {
    ChangeHandleContent(
        state = ChangeHandleState(),
        onBackPressed = { },
        onSaveClicked = { },
        onHandleChanged = { },
        onHandleErrorAnimated = { },
        onErrorDismiss = { }
    )
}
