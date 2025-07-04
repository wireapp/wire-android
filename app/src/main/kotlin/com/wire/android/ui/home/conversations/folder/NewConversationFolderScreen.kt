/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.ui.home.conversations.folder

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.result.ResultBackNavigator
import com.ramcosta.composedestinations.spec.DestinationStyle
import com.wire.android.R
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.annotation.app.WireDestination
import com.wire.android.ui.common.ShakeAnimation
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
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.home.settings.account.displayname.ChangeDisplayNameViewModel.Companion.NAME_MAX_COUNT
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.util.ui.SnackBarMessageHandler

@WireDestination(
    style = DestinationStyle.Runtime::class, // default should be SlideNavigationAnimation
)
@Composable
fun NewConversationFolderScreen(
    navigator: Navigator,
    resultNavigator: ResultBackNavigator<NewConversationFolderNavBackArgs>,
    viewModel: NewFolderViewModel = hiltViewModel()
) {

    LaunchedEffect(viewModel.folderNameState.folderId) {
        if (viewModel.folderNameState.folderId != null) {
            resultNavigator.navigateBack(
                NewConversationFolderNavBackArgs(
                    viewModel.textState.text.toString(),
                    viewModel.folderNameState.folderId!!
                )
            )
        }
    }

    Content(
        textState = viewModel.textState,
        state = viewModel.folderNameState,
        onContinuePressed = {
            viewModel.createFolder(viewModel.textState.text.toString())
        },
        onBackPressed = navigator::navigateBack
    )

    SnackBarMessageHandler(viewModel.infoMessage)
}

@Composable
private fun Content(
    textState: TextFieldState,
    state: FolderNameState,
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
                    navigationIconType = NavigationIconType.Back(),
                    title = stringResource(id = R.string.label_new_folder)
                )
            }
        ) { internalPadding ->
            Column(
                modifier = Modifier
                    .padding(internalPadding)
                    .fillMaxSize()
            ) {
                val keyboardController = LocalSoftwareKeyboardController.current

                Box(
                    modifier = Modifier
                        .weight(weight = 1f, fill = true)
                        .fillMaxWidth()
                ) {
                    ShakeAnimation(modifier = Modifier.align(Alignment.Center)) { animate ->
                        WireTextField(
                            textState = textState,
                            labelText = stringResource(R.string.new_folder_folder_name).uppercase(),
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

                Surface(
                    shadowElevation = scrollState.rememberBottomBarElevationState().value,
                    color = MaterialTheme.wireColorScheme.background
                ) {
                    Box(modifier = Modifier.padding(MaterialTheme.wireDimensions.spacing16x)) {
                        WirePrimaryButton(
                            text = stringResource(R.string.new_folder_create_folder),
                            onClick = onContinuePressed,
                            fillMaxWidth = true,
                            state = if (buttonEnabled) Default else Disabled,
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
private fun computeNameErrorState(error: FolderNameState.NameError) =
    if (error is FolderNameState.NameError.TextFieldError) {
        when (error) {
            FolderNameState.NameError.TextFieldError.NameEmptyError -> WireTextFieldState.Error(
                stringResource(id = R.string.new_folder_error_name_empty)
            )

            FolderNameState.NameError.TextFieldError.NameExceedLimitError -> WireTextFieldState.Error(
                stringResource(id = R.string.new_folder_error_name_exceeded_limit_error)
            )

            FolderNameState.NameError.TextFieldError.NameAlreadyExistError -> WireTextFieldState.Error(
                stringResource(id = R.string.new_folder_error_name_exist)
            )
        }
    } else {
        WireTextFieldState.Default
    }

@PreviewMultipleThemes
@Composable
fun PreviewNewConversationFolder() = WireTheme {
    Content(TextFieldState("Secret group"), FolderNameState(), {}, {})
}

@PreviewMultipleThemes
@Composable
fun PreviewNewConversationFolderErrorNameExist() = WireTheme {
    Content(
        TextFieldState("Secret group"),
        FolderNameState(error = FolderNameState.NameError.TextFieldError.NameAlreadyExistError),
        {},
        {}
    )
}
