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
package com.wire.android.ui.home.conversations.details.editguestaccess.createPasswordProtectedGuestLink
import com.ramcosta.composedestinations.annotation.Destination
import com.wire.android.navigation.WireRootNavGraph

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.navigation.Navigator
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.rememberTopBarElevationState
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.textfield.DefaultPassword
import com.wire.android.ui.common.textfield.WirePasswordTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.home.conversations.details.editguestaccess.GenerateGuestRoomLinkFailureDialog
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes
import kotlinx.coroutines.launch

@Destination<WireRootNavGraph>(
    navArgs = CreatePasswordGuestLinkNavArgs::class
)
@Composable
fun CreatePasswordProtectedGuestLinkScreen(
    navigator: Navigator,
    viewModel: CreatePasswordGuestLinkViewModel = hiltViewModel(),
) {
    CreatePasswordProtectedGuestLinkScreenContent(
        state = viewModel.state,
        passwordTextState = viewModel.state.passwordTextState,
        confirmPasswordTextState = viewModel.state.confirmPasswordTextState,
        navigateBack = navigator::navigateBack,
        onGenerateRandomPassword = viewModel::onGenerateRandomPassword,
        onGenerateLink = viewModel::onGenerateLink,
        onErrorDialogDismissed = viewModel::onErrorDialogDismissed,
        observePasswordValidation = viewModel::observePasswordValidation,
    )
}

@Composable
fun CreatePasswordProtectedGuestLinkScreenContent(
    state: CreatePasswordGuestLinkState,
    passwordTextState: TextFieldState,
    confirmPasswordTextState: TextFieldState,
    navigateBack: () -> Unit,
    onGenerateRandomPassword: () -> Unit,
    onGenerateLink: () -> Unit,
    onErrorDialogDismissed: () -> Unit,
    observePasswordValidation: suspend () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val passwordCurrentState by rememberUpdatedState(newValue = passwordTextState.text.toString())
    val onCopyClick = remember {
        {
            clipboardManager.setText(AnnotatedString(passwordCurrentState))
            Toast.makeText(
                context,
                context.getString(R.string.conversation_options_create_password_protected_guest_link_password_copied),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    LaunchedEffect(state.isLinkCreationSuccessful) {
        if (state.isLinkCreationSuccessful) {
            onCopyClick()
            navigateBack()
        }
    }

    LaunchedEffect(Unit) {
        launch {
            observePasswordValidation()
        }
    }

    WireScaffold(
        modifier = modifier,
        topBar = {
            WireCenterAlignedTopAppBar(
                elevation = scrollState.rememberTopBarElevationState().value,
                onNavigationPressed = navigateBack,
                title = stringResource(id = R.string.conversation_options_create_password_protected_guest_link_title),
            )
        }
    ) { internalPadding ->
        Column {
            LazyColumn(
                modifier = Modifier
                    .background(MaterialTheme.wireColorScheme.background)
                    .padding(internalPadding)
                    .padding(
                        start = dimensions().spacing16x,
                        end = dimensions().spacing16x,
                        bottom = dimensions().spacing16x,
                        top = dimensions().spacing16x,
                    )
                    .weight(1F)
                    .fillMaxSize()
            ) {

                item {
                    Text(
                        text = stringResource(
                            id = R.string.conversation_options_create_password_protected_guest_link_discrption
                        ),
                        style = MaterialTheme.wireTypography.body02.copy(fontWeight = FontWeight.Normal)
                    )
                    Spacer(modifier = Modifier.height(dimensions().spacing16x))
                    Text(
                        text = stringResource(
                            id = R.string.conversation_options_create_password_protected_guest_link_discrption_2
                        ),
                        style = MaterialTheme.wireTypography.body02
                    )
                    Spacer(modifier = Modifier.height(dimensions().spacing24x))
                }
                item {
                    val onClick = remember {
                        {
                            onGenerateRandomPassword()
                            Toast.makeText(
                                context,
                                context.getString(R.string.conversation_options_create_password_protected_guest_link_password_generated),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    GeneratePasswordButton(
                        onClick = onClick
                    )
                    Spacer(modifier = Modifier.height(dimensions().spacing24x))
                }
                item {
                    WirePasswordTextField(
                        textState = passwordTextState,
                        labelMandatoryIcon = true,
                        labelText = stringResource(
                            R.string.conversation_options_create_password_protected_guest_link_button_placeholder_text
                        ),
                        descriptionText = stringResource(
                            R.string.conversation_options_create_password_protected_guest_link_password_description
                        ),
                        keyboardOptions = KeyboardOptions.DefaultPassword.copy(imeAction = ImeAction.Next),
                        modifier = Modifier
                            .testTag("password"),
                        state = if (state.invalidPassword) {
                            WireTextFieldState.Error()
                        } else {
                            WireTextFieldState.Default
                        },
                        autoFill = false
                    )
                    Spacer(modifier = Modifier.height(dimensions().spacing16x))
                }

                item {
                    WirePasswordTextField(
                        labelText = stringResource(
                            id = R.string.conversation_options_create_confirm_password_protected_guest_link_password_label
                        ),
                        placeholderText = stringResource(
                            id = R.string.conversation_options_create_password_protected_guest_link_button_placeholder_text
                        ),
                        textState = confirmPasswordTextState,
                        state = if (state.invalidPassword) {
                            WireTextFieldState.Error()
                        } else {
                            WireTextFieldState.Default
                        },
                        keyboardOptions = KeyboardOptions.DefaultPassword.copy(imeAction = ImeAction.Done),
                        autoFill = false
                    )
                    Spacer(modifier = Modifier.height(dimensions().spacing24x))
                }
            }
            Surface(
                modifier = Modifier
                    .background(MaterialTheme.wireColorScheme.background),
                shadowElevation = dimensions().spacing8x
            ) {
                CreateButton(
                    enabled = true,
                    isLoading = state.isLoading,
                    onCreateLink = onGenerateLink
                )
            }
        }

        if (state.error != null) {
            GenerateGuestRoomLinkFailureDialog(
                onDismiss = onErrorDialogDismissed
            )
        }
    }
}

@Composable
private fun CreateButton(
    enabled: Boolean,
    isLoading: Boolean,
    onCreateLink: () -> Unit
) {
    WirePrimaryButton(
        text = stringResource(id = R.string.guest_link_button_create_link),
        fillMaxWidth = true,
        onClick = onCreateLink,
        loading = isLoading,
        state = if (!enabled) {
            WireButtonState.Disabled
        } else {
            WireButtonState.Default
        },
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.wireColorScheme.background)
            .padding(MaterialTheme.wireDimensions.spacing16x)

    )
}

@PreviewMultipleThemes
@Composable
fun PreviewCreatePasswordProtectedGuestLinkScreen() = WireTheme {
    CreatePasswordProtectedGuestLinkScreenContent(
        state = CreatePasswordGuestLinkState(),
        passwordTextState = TextFieldState(),
        confirmPasswordTextState = TextFieldState(),
        navigateBack = {},
        onGenerateRandomPassword = {},
        onGenerateLink = {},
        onErrorDialogDismissed = {},
        observePasswordValidation = {}
    )
}
