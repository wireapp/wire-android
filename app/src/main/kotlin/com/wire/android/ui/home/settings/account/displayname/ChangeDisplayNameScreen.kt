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

package com.wire.android.ui.home.settings.account.displayname

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.MaterialTheme
import com.wire.android.ui.common.scaffold.WireScaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
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
import com.wire.android.ui.common.Icon
import com.wire.android.ui.common.ShakeAnimation
import com.wire.android.ui.common.button.WireButtonState.Default
import com.wire.android.ui.common.button.WireButtonState.Disabled
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.rememberBottomBarElevationState
import com.wire.android.ui.common.rememberTopBarElevationState
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@RootNavGraph
@Destination
@Composable
fun ChangeDisplayNameScreen(
    viewModel: ChangeDisplayNameViewModel = hiltViewModel(),
    navigator: Navigator,
    resultNavigator: ResultBackNavigator<Boolean>
) {
    with(viewModel) {
        ChangeDisplayNameContent(
            displayNameState,
            ::onNameChange,
            {
                saveDisplayName(
                    onFailure = {
                        resultNavigator.setResult(false)
                        resultNavigator.navigateBack()
                    },
                    onSuccess = {
                        resultNavigator.setResult(true)
                        resultNavigator.navigateBack()
                    }
                )
            },
            ::onNameErrorAnimated,
            navigator::navigateBack
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ChangeDisplayNameContent(
    state: DisplayNameState,
    onNameChange: (TextFieldValue) -> Unit,
    onContinuePressed: () -> Unit,
    onNameErrorAnimated: () -> Unit,
    onBackPressed: () -> Unit
) {
    val scrollState = rememberScrollState()
    with(state) {
        WireScaffold(topBar = {
            WireCenterAlignedTopAppBar(
                elevation = scrollState.rememberTopBarElevationState().value,
                onNavigationPressed = onBackPressed,
                title = stringResource(id = R.string.settings_myaccount_display_name_title)
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
                    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
                    Text(
                        text = stringResource(id = R.string.settings_myaccount_display_name_description),
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
                        ShakeAnimation { animate ->
                            if (animatedNameError) {
                                animate()
                                onNameErrorAnimated()
                            }
                            WireTextField(
                                value = displayName,
                                onValueChange = onNameChange,
                                labelText = stringResource(R.string.settings_myaccount_display_name).uppercase(),
                                state = computeNameErrorState(error),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
                                    imeAction = androidx.compose.ui.text.input.ImeAction.Done
                                ),
                                descriptionText = stringResource(id = R.string.settings_myaccount_display_name_exceeded_limit_error),
                                keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                                modifier = Modifier.padding(
                                    horizontal = MaterialTheme.wireDimensions.spacing16x
                                )
                            )
                        }
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
                            onClick = onContinuePressed,
                            fillMaxWidth = true,
                            trailingIcon = androidx.compose.material.icons.Icons.Filled.ChevronRight.Icon(),
                            state = if (continueEnabled) Default else Disabled,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun computeNameErrorState(error: DisplayNameState.NameError) =
    if (error is DisplayNameState.NameError.TextFieldError) {
        when (error) {
            DisplayNameState.NameError.TextFieldError.NameEmptyError -> WireTextFieldState.Error(
                stringResource(id = R.string.settings_myaccount_display_name_error)
            )

            DisplayNameState.NameError.TextFieldError.NameExceedLimitError -> WireTextFieldState.Error(
                stringResource(id = R.string.settings_myaccount_display_name_exceeded_limit_error)
            )
        }
    } else {
        WireTextFieldState.Default
    }

@Preview
@Composable
fun PreviewChangeDisplayName() {
    ChangeDisplayNameContent(DisplayNameState("Bruce Wayne", TextFieldValue("Bruce Wayne")), {}, {}, {}, {})
}
