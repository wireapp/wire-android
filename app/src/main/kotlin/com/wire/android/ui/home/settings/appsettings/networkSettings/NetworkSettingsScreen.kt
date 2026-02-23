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

package com.wire.android.ui.home.settings.appsettings.networkSettings

import com.wire.android.navigation.annotation.app.WireRootDestination
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.navigation.Navigator
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.home.conversations.details.options.ArrowType
import com.wire.android.ui.home.conversations.details.options.GroupConversationOptionsItem
import com.wire.android.ui.home.settings.SwitchState
import com.wire.android.ui.theme.WireTheme

@WireRootDestination
@Composable
fun NetworkSettingsScreen(
    navigator: Navigator,
    networkSettingsViewModel: NetworkSettingsViewModel = hiltViewModel()
) {
    NetworkSettingsScreenContent(
        onBackPressed = navigator::navigateBack,
        isWebSocketEnabled = networkSettingsViewModel.networkSettingsState.isPersistentWebSocketConnectionEnabled,
        isEnforcedByMDM = networkSettingsViewModel.networkSettingsState.isEnforcedByMDM,
        isWebSocketEnforcedByDefault = networkSettingsViewModel.networkSettingsState.isWebSocketEnforcedByDefault,
        setWebSocketState = { networkSettingsViewModel.setWebSocketState(it) },
    )
}

@Composable
fun NetworkSettingsScreenContent(
    onBackPressed: () -> Unit,
    isWebSocketEnabled: Boolean,
    isEnforcedByMDM: Boolean,
    isWebSocketEnforcedByDefault: Boolean,
    setWebSocketState: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    WireScaffold(
        modifier = modifier,
        topBar = {
            WireCenterAlignedTopAppBar(
                onNavigationPressed = onBackPressed,
                elevation = dimensions().spacing0x,
                title = stringResource(id = R.string.settings_network_settings_label)
            )
        }
    ) { internalPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(internalPadding)
        ) {
            val switchState = remember(isWebSocketEnabled, isEnforcedByMDM, isWebSocketEnforcedByDefault) {
                when {
                    isEnforcedByMDM -> SwitchState.TextOnly(true)
                    isWebSocketEnforcedByDefault -> SwitchState.TextOnly(true)
                    else -> SwitchState.Enabled(
                        value = isWebSocketEnabled,
                        onCheckedChange = setWebSocketState
                    )
                }
            }

            val subtitle = if (isEnforcedByMDM) {
                stringResource(R.string.settings_keep_websocket_enforced_by_organization)
            } else {
                stringResource(R.string.settings_keep_connection_to_websocket_description)
            }

            GroupConversationOptionsItem(
                title = stringResource(R.string.settings_keep_connection_to_websocket),
                subtitle = subtitle,
                switchState = switchState,
                arrowType = ArrowType.NONE
            )
        }
    }
}

@Composable
@MultipleThemePreviews
fun PreviewNetworkSettingsScreenWebSocketEnabled() = WireTheme {
    NetworkSettingsScreenContent(
        onBackPressed = {},
        isWebSocketEnabled = true,
        isEnforcedByMDM = false,
        isWebSocketEnforcedByDefault = false,
        setWebSocketState = {},
    )
}

@Composable
@MultipleThemePreviews
fun PreviewNetworkSettingsScreenWebSocketDisabled() = WireTheme {
    NetworkSettingsScreenContent(
        onBackPressed = {},
        isWebSocketEnabled = false,
        isEnforcedByMDM = false,
        isWebSocketEnforcedByDefault = false,
        setWebSocketState = {},
    )
}

@Composable
@MultipleThemePreviews
fun PreviewNetworkSettingsScreenEnforcedByMDM() = WireTheme {
    NetworkSettingsScreenContent(
        onBackPressed = {},
        isWebSocketEnabled = true,
        isEnforcedByMDM = true,
        isWebSocketEnforcedByDefault = false,
        setWebSocketState = {},
    )
}

@Composable
@MultipleThemePreviews
fun PreviewNetworkSettingsScreenEnforcedByDefault() = WireTheme {
    NetworkSettingsScreenContent(
        onBackPressed = {},
        isWebSocketEnabled = true,
        isEnforcedByMDM = false,
        isWebSocketEnforcedByDefault = true,
        setWebSocketState = {},
    )
}
