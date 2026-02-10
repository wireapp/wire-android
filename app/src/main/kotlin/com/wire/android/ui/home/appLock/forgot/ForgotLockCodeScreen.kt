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
package com.wire.android.ui.home.appLock.forgot

import android.content.Intent
import com.wire.android.navigation.annotation.app.WireRootDestination
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.LoginTypeSelector
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.annotation.app.WireDestination
import com.wire.android.ui.LocalActivity
import com.wire.android.ui.WireActivity
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.rememberBottomBarElevationState
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.ramcosta.composedestinations.generated.app.destinations.NewLoginScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.WelcomeScreenDestination
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.ui.userprofile.self.dialog.LogoutOptionsDialog
import com.wire.android.ui.userprofile.self.dialog.LogoutOptionsDialogState
import com.wire.android.util.dialogErrorStrings
import com.wire.android.util.ui.PreviewMultipleThemes

@WireRootDestination
@Composable
fun ForgotLockCodeScreen(
    viewModel: ForgotLockScreenViewModel = hiltViewModel(),
) {
    val activity = LocalActivity.current
    val logoutOptionsDialogState = rememberVisibilityState<LogoutOptionsDialogState>()
    with(viewModel.state) {
        LaunchedEffect(completed) {
            if (completed) {
                startLoginActivity(activity)
            }
        }
        ForgotLockCodeScreenContent(
            scrollState = rememberScrollState(),
            isLoggingOut = isLoggingOut,
            onLogout = {
                logoutOptionsDialogState.show(
                    logoutOptionsDialogState.savedState ?: LogoutOptionsDialogState()
                )
            },
        )
        LogoutOptionsDialog(
            dialogState = logoutOptionsDialogState,
            logout = viewModel::onLogoutConfirmed,
        )
        if (error != null) {
            val (title, message) = error.dialogErrorStrings(LocalContext.current.resources)
            WireDialog(
                title = title,
                text = message,
                onDismiss = viewModel::onErrorDismissed,
                optionButton1Properties = WireDialogButtonProperties(
                    onClick = viewModel::onErrorDismissed,
                    text = stringResource(id = R.string.label_ok),
                    type = WireDialogButtonType.Primary,
                ),
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ForgotLockCodeScreenContent(
    scrollState: ScrollState,
    isLoggingOut: Boolean,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WireScaffold { internalPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(internalPadding)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(weight = 1f, fill = true)
                    .verticalScroll(scrollState)
                    .padding(MaterialTheme.wireDimensions.spacing16x)
                    .semantics { testTagsAsResourceId = true }
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_wire_logo),
                    tint = MaterialTheme.colorScheme.onBackground,
                    contentDescription = stringResource(id = R.string.content_description_welcome_wire_logo),
                    modifier = Modifier.padding(top = MaterialTheme.wireDimensions.spacing56x)
                )
                Text(
                    text = stringResource(id = R.string.settings_forgot_lock_screen_title),
                    style = MaterialTheme.wireTypography.title02,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(
                        top = MaterialTheme.wireDimensions.spacing32x,
                        bottom = MaterialTheme.wireDimensions.spacing16x
                    )
                )
                Text(
                    text = stringResource(id = R.string.settings_forgot_lock_screen_description),
                    style = MaterialTheme.wireTypography.body01,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(
                        top = MaterialTheme.wireDimensions.spacing8x,
                        bottom = MaterialTheme.wireDimensions.spacing8x
                    )
                )
                Text(
                    text = stringResource(id = R.string.settings_forgot_lock_screen_warning),
                    style = MaterialTheme.wireTypography.body01,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(
                        top = MaterialTheme.wireDimensions.spacing8x,
                        bottom = MaterialTheme.wireDimensions.spacing8x
                    )
                )
                Spacer(modifier = Modifier.weight(1f))
            }

            Surface(
                shadowElevation = scrollState.rememberBottomBarElevationState().value,
                color = MaterialTheme.wireColorScheme.background,
                modifier = Modifier.semantics { testTagsAsResourceId = true }
            ) {
                Box(modifier = Modifier.padding(MaterialTheme.wireDimensions.spacing16x)) {
                    LogoutButton(isLoggingOut = isLoggingOut, onLogout = onLogout)
                }
            }
        }
    }
}

@Composable
private fun LogoutButton(
    isLoggingOut: Boolean,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(modifier = modifier) {
        WirePrimaryButton(
            text = stringResource(R.string.user_profile_logout),
            onClick = onLogout,
            loading = isLoggingOut,
            state = if (isLoggingOut) WireButtonState.Disabled else WireButtonState.Default,
            interactionSource = interactionSource,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("logout_button")
        )
    }
}

private fun startLoginActivity(activity: androidx.appcompat.app.AppCompatActivity) {
    val intent = Intent(activity, WireActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    activity.startActivity(intent)
    activity.finish()
}

@Composable
@PreviewMultipleThemes
fun PreviewForgotLockCodeScreen() {
    WireTheme {
        ForgotLockCodeScreenContent(rememberScrollState(), false, {})
    }
}
