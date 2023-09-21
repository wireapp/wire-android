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
 *
 *
 */

package com.wire.android.ui.home.settings.privacy

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.wire.android.R
import com.wire.android.feature.AppLockConfig
import com.wire.android.model.Clickable
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.destinations.SetLockCodeScreenDestination
import com.wire.android.ui.home.conversations.details.options.ArrowType
import com.wire.android.ui.home.conversations.details.options.GroupConversationOptionsItem
import com.wire.android.ui.home.conversations.details.options.SwitchState

@RootNavGraph
@Destination
@Composable
fun PrivacySettingsConfigScreen(
    navigator: Navigator,
    viewModel: PrivacySettingsViewModel = hiltViewModel()
) {
    with(viewModel) {
        PrivacySettingsScreenContent(
            areReadReceiptsEnabled = state.areReadReceiptsEnabled,
            setReadReceiptsState = ::setReadReceiptsState,
            isTypingIndicatorEnabled = state.isTypingIndicatorEnabled,
            setTypingIndicatorState = ::setTypingIndicatorState,
            screenshotCensoringConfig = state.screenshotCensoringConfig,
            setScreenshotCensoringConfig = ::setScreenshotCensoringConfig,
            appLockConfig = state.appLockConfig,
            onBackPressed = navigator::navigateBack,
            disableAppLock = viewModel::disableAppLock,
            enableAppLock = {
                // navigate to set app lock screen
                navigator.navigate(
                    NavigationCommand(
                        SetLockCodeScreenDestination,
                        backStackMode = BackStackMode.NONE
                    )
                )
            }
        )
    }
}

@Composable
fun PrivacySettingsScreenContent(
    areReadReceiptsEnabled: Boolean,
    setReadReceiptsState: (Boolean) -> Unit,
    isTypingIndicatorEnabled: Boolean,
    setTypingIndicatorState: (Boolean) -> Unit,
    screenshotCensoringConfig: ScreenshotCensoringConfig,
    setScreenshotCensoringConfig: (Boolean) -> Unit,
    appLockConfig: AppLockConfig,
    onBackPressed: () -> Unit,
    disableAppLock: () -> Unit,
    enableAppLock: () -> Unit
) {
    WireScaffold(topBar = {
        WireCenterAlignedTopAppBar(
            onNavigationPressed = onBackPressed,
            elevation = 0.dp,
            title = stringResource(id = R.string.settings_privacy_settings_label)
        )
    }) { internalPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(internalPadding)
        ) {
            GroupConversationOptionsItem(
                title = stringResource(R.string.settings_send_read_receipts),
                switchState = SwitchState.Enabled(value = areReadReceiptsEnabled, onCheckedChange = setReadReceiptsState),
                arrowType = ArrowType.NONE,
                subtitle = stringResource(id = R.string.settings_send_read_receipts_description)
            )
            GroupConversationOptionsItem(
                title = stringResource(R.string.settings_censor_screenshots),
                switchState = when (screenshotCensoringConfig) {
                    ScreenshotCensoringConfig.DISABLED ->
                        SwitchState.Enabled(value = false, onCheckedChange = setScreenshotCensoringConfig)

                    ScreenshotCensoringConfig.ENABLED_BY_USER ->
                        SwitchState.Enabled(value = true, onCheckedChange = setScreenshotCensoringConfig)

                    ScreenshotCensoringConfig.ENFORCED_BY_TEAM ->
                        SwitchState.Disabled(value = true)
                },
                arrowType = ArrowType.NONE,
                subtitle = stringResource(
                    id = when (screenshotCensoringConfig) {
                        ScreenshotCensoringConfig.ENFORCED_BY_TEAM -> R.string.settings_censor_screenshots_enforced_by_team_description
                        else -> R.string.settings_censor_screenshots_description
                    }
                )
            )
            GroupConversationOptionsItem(
                title = stringResource(R.string.settings_show_typing_indicator_title),
                switchState = SwitchState.Enabled(value = isTypingIndicatorEnabled, onCheckedChange = setTypingIndicatorState),
                arrowType = ArrowType.NONE,
                subtitle = stringResource(id = R.string.settings_send_read_receipts_description)
            )

            AppLockItem(
                state = appLockConfig,
                disableAppLock = disableAppLock,
                enableAppLock = enableAppLock
            )
        }
    }
}

@Composable
fun AppLockItem(
    state: AppLockConfig,
    disableAppLock: () -> Unit,
    enableAppLock: () -> Unit,
) {
    val onCLick = remember(state) {
        when (state) {
            is AppLockConfig.EnforcedByTeam -> {
                // do nothing, onClick is disabled anyway
                {}
            }

            is AppLockConfig.Enabled -> {
                // app-lock is not enforced by any of logged accounts, call function to disable the app-lock
                disableAppLock
            }

            is AppLockConfig.Disabled -> {
                // navigate to set app lock screen
                enableAppLock
            }
        }
    }
    GroupConversationOptionsItem(
        title = stringResource(id = R.string.settings_app_lock_title),
        switchState = when (state) {
            is AppLockConfig.EnforcedByTeam -> SwitchState.Disabled(value = true)
            else -> SwitchState.Enabled(
                value = state is AppLockConfig.Enabled,
                onCheckedChange = null
            )
        },
        arrowType = ArrowType.NONE,
        subtitle = stringResource(id = R.string.settings_app_lock_description, state.timeoutInSeconds),
        clickable = Clickable(
            enabled = state !is AppLockConfig.EnforcedByTeam,
            onClick = onCLick
        )
    )
}

@Composable
@Preview
fun PreviewSendReadReceipts() {
    PrivacySettingsScreenContent(
        areReadReceiptsEnabled = true,
        setReadReceiptsState = {},
        isTypingIndicatorEnabled = true,
        setTypingIndicatorState = {},
        screenshotCensoringConfig = ScreenshotCensoringConfig.DISABLED,
        setScreenshotCensoringConfig = {},
        appLockConfig = AppLockConfig.Disabled,
        onBackPressed = {},
        disableAppLock = {},
        enableAppLock = {}
    )
}
