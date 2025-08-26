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
package com.wire.android.feature.cells.ui.rename

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.feature.cells.R
import com.wire.android.feature.cells.ui.common.FullScreenLoading
import com.wire.android.feature.cells.ui.rename.RenameNodeViewModel.Companion.NAME_MAX_COUNT
import com.wire.android.model.ClickBlockParams
import com.wire.android.model.DisplayNameState
import com.wire.android.navigation.PreviewNavigator
import com.wire.android.navigation.WireNavigator
import com.wire.android.navigation.annotation.features.cells.WireDestination
import com.wire.android.navigation.style.PopUpNavigationAnimation
import com.wire.android.ui.common.HandleActions
import com.wire.android.ui.common.animation.ShakeAnimation
import com.wire.android.ui.common.button.WireButtonState.Default
import com.wire.android.ui.common.button.WireButtonState.Disabled
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.textfield.DefaultText
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.common.textfield.maxLengthWithCallback
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions

@WireDestination(
    style = PopUpNavigationAnimation::class,
    navArgsDelegate = RenameNodeNavArgs::class,
)
@Composable
fun RenameNodeScreen(
    navigator: WireNavigator,
    modifier: Modifier = Modifier,
    renameNodeViewModel: RenameNodeViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    WireScaffold(
        modifier = modifier,
        topBar = {
            WireCenterAlignedTopAppBar(
                onNavigationPressed = { navigator.navigateBack() },
                title = if (renameNodeViewModel.isFolder() == true) {
                    stringResource(R.string.rename_folder_label)
                } else {
                    stringResource(R.string.rename_file_label)
                },
                navigationIconType = NavigationIconType.Close(),
                elevation = dimensions().spacing0x,
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.wireColorScheme.background,
                shadowElevation = MaterialTheme.wireDimensions.bottomNavigationShadowElevation
            ) {
                WirePrimaryButton(
                    modifier = Modifier.padding(dimensions().spacing16x),
                    text = stringResource(R.string.rename_label),
                    onClick = { renameNodeViewModel.renameNode(renameNodeViewModel.textState.text.toString()) },
                    state = if (renameNodeViewModel.displayNameState.saveEnabled) Default else Disabled,
                    clickBlockParams = ClickBlockParams(blockWhenSyncing = true, blockWhenConnecting = true),
                )
            }
        }
    ) { innerPadding ->

        val keyboardController = LocalSoftwareKeyboardController.current

        Box(modifier = Modifier.padding(innerPadding)) {
            ShakeAnimation { animate ->
                WireTextField(
                    textState = renameNodeViewModel.textState,
                    labelText = if (renameNodeViewModel.isFolder() == true) {
                        stringResource(R.string.rename_folder_label).uppercase()
                    } else {
                        stringResource(R.string.rename_file_label).uppercase()
                    },
                    inputTransformation = InputTransformation.maxLengthWithCallback(NAME_MAX_COUNT, animate),
                    lineLimits = TextFieldLineLimits.SingleLine,
                    state = computeNameErrorState(renameNodeViewModel.displayNameState.error, renameNodeViewModel.isFolder()),
                    keyboardOptions = KeyboardOptions.DefaultText,
                    descriptionText = if (renameNodeViewModel.isFolder() == true) {
                        stringResource(id = R.string.rename_long_folder_name_error)
                    } else {
                        stringResource(id = R.string.rename_long_file_name_error)
                    },
                    onKeyboardAction = { keyboardController?.hide() },
                    modifier = Modifier.padding(
                        horizontal = MaterialTheme.wireDimensions.spacing16x
                    )
                )
            }
        }
    }

    if (renameNodeViewModel.displayNameState.loading) {
        FullScreenLoading()
    }

    HandleActions(renameNodeViewModel.actions) { action ->
        when (action) {
            is RenameNodeViewModelAction.Success -> {
                val message = if (renameNodeViewModel.isFolder() == true) {
                    context.resources.getString(R.string.rename_folder_renamed)
                } else {
                    context.resources.getString(R.string.rename_file_renamed)
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }

            is RenameNodeViewModelAction.Failure ->
                Toast.makeText(context, context.resources.getString(R.string.rename_failure), Toast.LENGTH_SHORT).show()
        }
        navigator.navigateBack()
    }
}

@Composable
private fun computeNameErrorState(
    error: DisplayNameState.NameError,
    isFolder: Boolean?
): WireTextFieldState {
    return when (error) {
        is DisplayNameState.NameError.TextFieldError -> {
            val messageRes = when (error) {
                DisplayNameState.NameError.TextFieldError.NameEmptyError ->
                    if (isFolder == true) R.string.rename_enter_folder_name else R.string.rename_enter_file_name

                DisplayNameState.NameError.TextFieldError.NameExceedLimitError ->
                    if (isFolder == true) R.string.rename_long_folder_name_error else R.string.rename_long_file_name_error

                DisplayNameState.NameError.TextFieldError.InvalidNameError -> R.string.rename_invalid_name
            }
            WireTextFieldState.Error(stringResource(id = messageRes))
        }

        else -> WireTextFieldState.Default
    }
}

@MultipleThemePreviews
@Composable
fun PreviewRenameNodeScreen() {
    WireTheme {
        RenameNodeScreen(
            navigator = PreviewNavigator
        )
    }
}
