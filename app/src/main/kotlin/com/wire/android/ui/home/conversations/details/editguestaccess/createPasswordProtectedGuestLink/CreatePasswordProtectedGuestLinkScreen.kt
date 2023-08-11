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
package com.wire.android.ui.home.conversations.details.editguestaccess.createPasswordProtectedGuestLink

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.wire.android.R
import com.wire.android.navigation.Navigator
import com.wire.android.ui.common.button.IconAlignment
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.button.wireSecondaryButtonColors
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.rememberTopBarElevationState
import com.wire.android.ui.common.snackbar.SwipeDismissSnackbarHost
import com.wire.android.ui.common.textfield.WirePasswordTextField
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.home.conversations.details.editguestaccess.CreateGuestLinkButton
import com.wire.android.ui.home.conversations.details.editguestaccess.GenerateGuestRoomLinkFailureDialog
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography

@OptIn(ExperimentalComposeUiApi::class)
@RootNavGraph
@Destination(
    navArgsDelegate = CreatePasswordGuestLinkNavArgs::class
)
@Composable
fun CreatePasswordProtectedGuestLinkScreen(
    navigator: Navigator,
    viewModel: CreatePasswordGuestLinkViewModel = hiltViewModel(),
) {
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    if (viewModel.state.isLinkCreationSuccessful) {
        navigator.navigateBack()
    }

    Scaffold(topBar = {
        WireCenterAlignedTopAppBar(
            elevation = scrollState.rememberTopBarElevationState().value,
            onNavigationPressed = navigator::navigateBack,
            title = stringResource(id = R.string.conversation_options_create_password_protected_guest_link_title),
        )
    }, snackbarHost = {
        SwipeDismissSnackbarHost(
            hostState = snackbarHostState, modifier = Modifier.fillMaxWidth()
        )
    }) { internalPadding ->
        Column {
            LazyColumn(
                modifier = Modifier
                    .background(MaterialTheme.wireColorScheme.background)
                    .padding(internalPadding)
                    .padding(
                        start = dimensions().spacing16x,
                        end = dimensions().spacing16x,
                        bottom = dimensions().spacing8x,
                        top = dimensions().spacing8x,
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
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(
                            id = R.string.conversation_options_create_password_protected_guest_link_discrption_2
                        ),
                        style = MaterialTheme.wireTypography.body02
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                }

                item {
                    WirePasswordTextField(
                        labelText = stringResource(
                            id = R.string.conversation_options_create_password_protected_guest_link_password_label
                        ),
                        value = viewModel.state.password,
                        onValueChange = viewModel::onPasswordUpdated,
                        autofillTypes = emptyList()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    WirePasswordTextField(
                        labelText = stringResource(
                            id = R.string.conversation_options_create_confirm_password_protected_guest_link_password_label
                        ),
                        value = viewModel.state.passwordConfirm,
                        onValueChange = viewModel::onPasswordConfirmUpdated,
                        autofillTypes = emptyList()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                item {
                    val clipboardManager = LocalClipboardManager.current
                    val onClick = remember(viewModel.state.password.text) {
                        {
                            if (viewModel.state.isPasswordValid) {
                                clipboardManager.setText(viewModel.state.password.annotatedString)
                            }
                        }
                    }
                    val copyButtonState by remember {
                        derivedStateOf {
                            when {
                                viewModel.state.password.text.isEmpty() -> WireButtonState.Disabled
                                viewModel.state.password != viewModel.state.passwordConfirm -> WireButtonState.Error
                                viewModel.state.isPasswordValid -> WireButtonState.Default
                                else -> WireButtonState.Disabled
                            }
                        }
                    }
                    CreatePasswordProtectedGuestLinkCopyPassword(
                        onClick = onClick,
                        state = copyButtonState
                    )
                }
            }
            Surface(
                modifier = Modifier
                    .background(MaterialTheme.wireColorScheme.background),
                shadowElevation = dimensions().spacing8x
            ) {
                CreateGuestLinkButton(
                    enabled = viewModel.state.isPasswordValid,
                    isLoading = viewModel.state.isLoading,
                    onCreateLink = viewModel::onGenerateLink
                )
            }
        }

        if (viewModel.state.error != null) {
            GenerateGuestRoomLinkFailureDialog(
                onDismiss = viewModel::onErrorDialogDissmissed
            )
        }
    }
}

@Composable
fun CreatePasswordProtectedGuestLinkCopyPassword(
    onClick: () -> Unit,
    state: WireButtonState

) {
    @DrawableRes
    val icon: Int? = remember(state) {
        when (state) {
            WireButtonState.Error -> R.drawable.ic_warning_circle
            WireButtonState.Default -> R.drawable.ic_check_tick
            WireButtonState.Positive,
            WireButtonState.Disabled,
            WireButtonState.Selected -> null
        }
    }
    WireSecondaryButton(
        state = state,
        onClick = onClick,
        text = "Copy",
        colors = wireSecondaryButtonColors(
            onError = MaterialTheme.wireColorScheme.secondaryButtonEnabled
        ),
        leadingIcon = icon?.let {
            {
                Icon(
                    modifier = Modifier.padding(end = 8.dp),
                    painter = painterResource(id = it),
                    tint = if (state == WireButtonState.Error) MaterialTheme.wireColorScheme.error else MaterialTheme.wireColorScheme.positive,
                    contentDescription = null
                )
            }
        },
        leadingIconAlignment = IconAlignment.Center
    )
}

@Preview
@Composable
fun PreviewCreatePasswordProtectedGuestLinkScreen() {
    CreatePasswordProtectedGuestLinkScreen(navigator = Navigator(finish = {}, navController = NavHostController(LocalContext.current)))
}
