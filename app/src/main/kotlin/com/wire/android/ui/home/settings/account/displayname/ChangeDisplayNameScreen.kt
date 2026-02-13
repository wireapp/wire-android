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

import com.wire.android.navigation.annotation.app.WireRootDestination
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.result.ResultBackNavigator
import com.wire.android.navigation.style.SlideNavigationAnimation
import com.wire.android.R
import com.wire.android.model.DisplayNameState
import com.wire.android.navigation.Navigator
import com.wire.android.ui.common.R as commonR
import com.wire.android.ui.common.animation.ShakeAnimation
import com.wire.android.ui.common.button.WireButtonState.Default
import com.wire.android.ui.common.button.WireButtonState.Disabled
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.rememberBottomBarElevationState
import com.wire.android.ui.common.rememberTopBarElevationState
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.textfield.DefaultText
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.common.textfield.maxLengthWithCallback
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.home.settings.account.displayname.ChangeDisplayNameViewModel.Companion.NAME_MAX_COUNT
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes

@WireRootDestination(
    style = SlideNavigationAnimation::class, // default should be SlideNavigationAnimation
)
@Composable
fun ChangeDisplayNameScreen(
    navigator: Navigator,
    resultNavigator: ResultBackNavigator<Boolean>,
    viewModel: ChangeDisplayNameViewModel = hiltViewModel()
) {
    with(viewModel) {
        LaunchedEffect(viewModel.displayNameState.completed) {
            when (viewModel.displayNameState.completed) {
                DisplayNameState.Completed.Success -> {
                    resultNavigator.setResult(true)
                    resultNavigator.navigateBack()
                }
                DisplayNameState.Completed.Failure -> {
                    resultNavigator.setResult(false)
                    resultNavigator.navigateBack()
                }
                DisplayNameState.Completed.None -> Unit // No action needed
            }
        }
        ChangeDisplayNameContent(
            textState = viewModel.textState,
            state = viewModel.displayNameState,
            onContinuePressed = ::saveDisplayName,
            onBackPressed = navigator::navigateBack
        )
    }
}

@Composable
fun ChangeDisplayNameContent(
    textState: TextFieldState,
    state: DisplayNameState,
    onContinuePressed: () -> Unit,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    with(state) {
        WireScaffold(
            modifier = modifier,
            topBar = {
                WireCenterAlignedTopAppBar(
                    elevation = scrollState.rememberTopBarElevationState().value,
                    onNavigationPressed = onBackPressed,
                    title = stringResource(id = R.string.settings_myaccount_display_name_title)
                )
            }
        ) { internalPadding ->
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
                            WireTextField(
                                textState = textState,
                                labelText = stringResource(R.string.settings_myaccount_display_name).uppercase(),
                                inputTransformation = InputTransformation.maxLengthWithCallback(NAME_MAX_COUNT, animate),
                                lineLimits = TextFieldLineLimits.SingleLine,
                                state = computeNameErrorState(error),
                                keyboardOptions = KeyboardOptions.DefaultText,
                                descriptionText = stringResource(id = R.string.settings_myaccount_display_name_exceeded_limit_error),
                                onKeyboardAction = { keyboardController?.hide() },
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
                            trailingIcon = {
                                Icon(
                                    painter = painterResource(commonR.drawable.ic_chevron_right),
                                    contentDescription = null,
                                )
                            },
                            state = if (saveEnabled) Default else Disabled,
                            loading = loading,
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
            else -> WireTextFieldState.Default
        }
    } else {
        WireTextFieldState.Default
    }

@PreviewMultipleThemes
@Composable
fun PreviewChangeDisplayName() = WireTheme {
    ChangeDisplayNameContent(TextFieldState("Bruce Wayne"), DisplayNameState(), {}, {})
}
