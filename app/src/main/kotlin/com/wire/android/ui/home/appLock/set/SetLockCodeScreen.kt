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
package com.wire.android.ui.home.appLock.set

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.wire.android.R
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.rememberNavigator
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.rememberBottomBarElevationState
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.spacers.HorizontalSpace
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.common.textfield.WirePasswordTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.toTimeLongLabelUiText
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.feature.auth.ValidatePasswordResult
import java.util.Locale

@RootNavGraph
@Destination
@Composable
fun SetLockCodeScreen(
    viewModel: SetLockScreenViewModel = hiltViewModel(),
    navigator: Navigator
) {
    SetLockCodeScreenContent(
        navigator = navigator,
        state = viewModel.state,
        scrollState = rememberScrollState(),
        onPasswordChanged = viewModel::onPasswordChanged,
        onBackPress = navigator::navigateBack,
        onContinue = viewModel::onContinue
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SetLockCodeScreenContent(
    navigator: Navigator,
    state: SetLockCodeViewState,
    scrollState: ScrollState,
    onPasswordChanged: (TextFieldValue) -> Unit,
    onBackPress: () -> Unit = {},
    onContinue: () -> Unit
) {
    LaunchedEffect(state.done) {
        if (state.done) {
            navigator.navigateBack()
        }
    }

    WireScaffold(
        snackbarHost = {},
        topBar = {
            WireCenterAlignedTopAppBar(
                onNavigationPressed = onBackPress,
                navigationIconType = if (state.isEditable) NavigationIconType.Back else null,
                elevation = dimensions().spacing0x,
                title = stringResource(id = R.string.settings_set_lock_screen_title)
            )
    }) { internalPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(internalPadding)
        ) {
            Column(
                modifier = Modifier
                    .weight(weight = 1f, fill = true)
                    .verticalScroll(scrollState)
                    .padding(MaterialTheme.wireDimensions.spacing16x)
                    .semantics {
                        testTagsAsResourceId = true
                    }
            ) {
                Text(
                    text = stringResource(
                        id = R.string.settings_set_lock_screen_description,
                        state.timeout.toTimeLongLabelUiText().asString()
                    ),
                    style = MaterialTheme.wireTypography.body01,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            vertical = MaterialTheme.wireDimensions.spacing24x
                        )
                        .testTag("registerText")
                )
                WirePasswordTextField(
                    value = state.password,
                    onValueChange = onPasswordChanged,
                    labelMandatoryIcon = true,
                    imeAction = ImeAction.Done,
                    modifier = Modifier
                        .testTag("password"),
                    state = WireTextFieldState.Default,
                    autofill = false,
                    placeholderText = stringResource(R.string.settings_set_lock_screen_passcode_label),
                    labelText = stringResource(R.string.settings_set_lock_screen_passcode_label).uppercase(Locale.getDefault())
                )
                VerticalSpace.x24()
                PasswordVerificationGroup(state.passwordValidation)
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
                    val enabled = state.password.text.isNotBlank() && state.passwordValidation.isValid
                    ContinueButton(
                        enabled = enabled,
                        onContinue = onContinue
                    )
                }
            }
        }

        BackHandler {
            if (state.isEditable) {
                onBackPress()
            }
        }
    }
}

@Composable
private fun PasswordVerificationGroup(validatePasswordResult: ValidatePasswordResult) {
    Column(
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.wireDimensions.spacing2x),
    ) {
        PasswordVerificationItem(
            isInvalid = (validatePasswordResult as? ValidatePasswordResult.Invalid)?.tooShort ?: false,
            text = stringResource(id = R.string.password_validation_length)
        )
        PasswordVerificationItem(
            isInvalid = (validatePasswordResult as? ValidatePasswordResult.Invalid)?.missingLowercaseCharacter ?: false,
            text = stringResource(id = R.string.password_validation_lowercase)
        )
        PasswordVerificationItem(
            isInvalid = (validatePasswordResult as? ValidatePasswordResult.Invalid)?.missingUppercaseCharacter ?: false,
            text = stringResource(id = R.string.password_validation_uppercase)
        )
        PasswordVerificationItem(
            isInvalid = (validatePasswordResult as? ValidatePasswordResult.Invalid)?.missingDigit ?: false,
            text = stringResource(id = R.string.password_validation_digit)
        )
        PasswordVerificationItem(
            isInvalid = (validatePasswordResult as? ValidatePasswordResult.Invalid)?.missingSpecialCharacter ?: false,
            text = stringResource(id = R.string.password_validation_special_character)
        )
    }
}

@Composable
private fun PasswordVerificationItem(isInvalid: Boolean, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(id = if (isInvalid) R.drawable.ic_validation_block else R.drawable.ic_validation_check),
            tint = if (isInvalid) MaterialTheme.wireColorScheme.secondaryText else MaterialTheme.wireColorScheme.positive,
            contentDescription = null,
        )
        HorizontalSpace.x8()
        Text(
            text = text,
            style = MaterialTheme.wireTypography.label04,
            color = MaterialTheme.wireColorScheme.secondaryText,
        )
    }
}

@Composable
private fun ContinueButton(
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean,
    onContinue: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(modifier = modifier) {
        WirePrimaryButton(
            text = stringResource(R.string.settings_set_lock_screen_continue_button_label),
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
fun PreviewPasswordVerificationGroup() {
    WireTheme {
        PasswordVerificationGroup(
            ValidatePasswordResult.Invalid(
                tooShort = true,
                missingLowercaseCharacter = false,
                missingUppercaseCharacter = false,
                missingDigit = true,
                missingSpecialCharacter = false,
            )
        )
    }
}

@Composable
@PreviewMultipleThemes
fun PreviewSetLockCodeScreen() {
    WireTheme {
        SetLockCodeScreenContent(
            navigator = rememberNavigator {},
            state = SetLockCodeViewState(),
            scrollState = rememberScrollState(),
            onPasswordChanged = {},
            onBackPress = {},
            onContinue = {}
        )
    }
}
