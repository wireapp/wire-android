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
package com.wire.android.feature.cells.ui.create.folder

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.result.ResultBackNavigator
import com.wire.android.feature.cells.R
import com.wire.android.feature.cells.ui.common.FileNameError
import com.wire.android.navigation.PreviewNavigator
import com.wire.android.navigation.PreviewResultBackNavigator
import com.wire.android.navigation.WireNavigator
import com.wire.android.navigation.annotation.features.cells.WireCellsDestination
import com.wire.android.navigation.style.PopUpNavigationAnimation
import com.wire.android.ui.common.HandleActions
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import java.util.Locale

@WireCellsDestination(
    style = PopUpNavigationAnimation::class,
    navArgs = CreateFolderScreenNavArgs::class,
)
@Composable
fun CreateFolderScreen(
    navigator: WireNavigator,
    resultNavigator: ResultBackNavigator<Boolean>,
    modifier: Modifier = Modifier,
    createFolderViewModel: CreateFolderViewModel = hiltViewModel()
) {
    val showErrorDialog = remember { mutableStateOf(false) }

    if (showErrorDialog.value) {
        WireDialog(
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false),
            title = stringResource(id = R.string.cells_create_folder),
            text = stringResource(id = R.string.create_folder_error),
            onDismiss = { showErrorDialog.value = false },
            dismissButtonProperties = WireDialogButtonProperties(
                onClick = { showErrorDialog.value = false },
                text = stringResource(id = R.string.cancel),
                type = WireDialogButtonType.Secondary,
            )
        )
    }

    WireScaffold(
        modifier = modifier,
        topBar = {
            WireCenterAlignedTopAppBar(
                onNavigationPressed = { navigator.navigateBack() },
                navigationIconType = NavigationIconType.Close(),
                elevation = dimensions().spacing0x,
                title = stringResource(id = R.string.cells_create_folder),
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + expandVertically(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Surface(
                    color = MaterialTheme.wireColorScheme.background,
                    shadowElevation = MaterialTheme.wireDimensions.bottomNavigationShadowElevation
                ) {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .padding(dimensions().spacing16x)
                    ) {
                        with(createFolderViewModel) {
                            WirePrimaryButton(
                                text = stringResource(R.string.cells_create_folder),
                                onClick = {
                                    createFolder(
                                        folderName = folderNameTextFieldState.text.toString()
                                    )
                                },
                                state = if (viewState.saveEnabled && !viewState.loading) {
                                    WireButtonState.Default
                                } else {
                                    WireButtonState.Disabled
                                },
                                loading = viewState.loading
                            )
                        }
                    }
                }
            }
        }
    ) {
        WireTextField(
            textState = createFolderViewModel.folderNameTextFieldState,
            placeholderText = stringResource(R.string.cells_folder_name),
            labelText = stringResource(R.string.cells_folder_name).uppercase(Locale.getDefault()),
            modifier = Modifier
                .padding(it)
                .padding(
                    top = dimensions().spacing32x,
                    start = dimensions().spacing16x,
                    end = dimensions().spacing16x
                ),
            state = computeNameErrorState(createFolderViewModel.viewState.error),
        )
    }

    HandleActions(createFolderViewModel.actions) { action ->
        when (action) {
            CreateFolderViewModelAction.Success -> {
                resultNavigator.setResult(true)
                resultNavigator.navigateBack()
            }
            CreateFolderViewModelAction.Failure -> {
                showErrorDialog.value = true
            }
        }
    }
}

@Composable
private fun computeNameErrorState(error: FileNameError?): WireTextFieldState {
    val messageRes = when (error) {
        FileNameError.NameEmpty -> R.string.cells_folder_name
        FileNameError.NameExceedLimit -> R.string.rename_long_folder_name_error
        FileNameError.NameAlreadyExist -> R.string.rename_folder_already_exist
        FileNameError.InvalidName -> R.string.rename_invalid_name
        null -> return WireTextFieldState.Default
    }

    return WireTextFieldState.Error(stringResource(id = messageRes))
}

@MultipleThemePreviews
@Composable
fun PreviewCreateFolderScreen() {
    WireTheme {
        CreateFolderScreen(
            navigator = PreviewNavigator,
            resultNavigator = PreviewResultBackNavigator as ResultBackNavigator<Boolean>,
        )
    }
}
