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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.wire.android.R
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.WireDestination
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.home.conversations.details.options.ArrowType
import com.wire.android.ui.home.conversations.details.options.GroupConversationOptionsItem
import com.wire.android.ui.home.settings.SwitchState
import com.wire.android.util.isWebsocketEnabledByDefault

@RootNavGraph
@WireDestination
@Composable
fun NetworkSettingsScreen(
    navigator: Navigator,
    networkSettingsViewModel: NetworkSettingsViewModel = hiltViewModel()
) {
    NetworkSettingsScreenContent(
        onBackPressed = navigator::navigateBack,
        isWebSocketEnabled = networkSettingsViewModel.networkSettingsState.isPersistentWebSocketConnectionEnabled,
        setWebSocketState = { networkSettingsViewModel.setWebSocketState(it) },
        backendName = networkSettingsViewModel.backendName
    )
}

@Composable
fun NetworkSettingsScreenContent(
    onBackPressed: () -> Unit,
    isWebSocketEnabled: Boolean,
    setWebSocketState: (Boolean) -> Unit,
    backendName: String

) {
    WireScaffold(topBar = {
        WireCenterAlignedTopAppBar(
            onNavigationPressed = onBackPressed,
            elevation = 0.dp,
            title = stringResource(id = R.string.settings_network_settings_label)
        )
    }) { internalPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(internalPadding)
        ) {
            val appContext = LocalContext.current.applicationContext
            val isWebSocketEnforcedByDefault = remember {
                isWebsocketEnabledByDefault(appContext)
            }
            val switchState = remember(isWebSocketEnabled) {
                if (isWebSocketEnforcedByDefault) {
                    SwitchState.TextOnly(true)
                } else {
                    SwitchState.Enabled(
                        value = isWebSocketEnabled,
                        onCheckedChange = setWebSocketState
                    )
                }
            }

            GroupConversationOptionsItem(
                title = stringResource(R.string.settings_keep_connection_to_websocket),
                subtitle = stringResource(
                    R.string.settings_keep_connection_to_websocket_description,
                    backendName
                ),
                switchState = switchState,
                arrowType = ArrowType.NONE
            )
        }
    }
}

@Composable
@Preview
fun PreviewNetworkSettingsScreen() {
    NetworkSettingsScreenContent(
        {}, true, {}, ""
    )
}
