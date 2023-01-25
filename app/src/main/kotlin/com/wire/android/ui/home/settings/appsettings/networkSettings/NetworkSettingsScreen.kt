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

package com.wire.android.ui.home.settings.appsettings.networkSettings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.home.conversations.details.options.ArrowType
import com.wire.android.ui.home.conversations.details.options.GroupConversationOptionsItem
import com.wire.android.ui.home.conversations.details.options.SwitchState

@Composable
fun NetworkSettingsScreen(networkSettingsViewModel: NetworkSettingsViewModel = hiltViewModel()) {
    NetworkSettingsScreenContent(
        onBackPressed = networkSettingsViewModel::navigateBack,
        isWebSocketEnabled = networkSettingsViewModel.networkSettingsState.isPersistentWebSocketConnectionEnabled,
        setWebSocketState = { networkSettingsViewModel.setWebSocketState(it) },
        backendName = networkSettingsViewModel.backendName
    )
}

@OptIn(
    ExperimentalMaterial3Api::class,
)
@Composable
fun NetworkSettingsScreenContent(
    onBackPressed: () -> Unit,
    isWebSocketEnabled: Boolean,
    setWebSocketState: (Boolean) -> Unit,
    backendName: String

) {
    Scaffold(topBar = {
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
            GroupConversationOptionsItem(
                title = stringResource(R.string.settings_keep_connection_to_websocket),
                subtitle = stringResource(
                    R.string.settings_keep_connection_to_websocket_description,
                    backendName
                ),
                switchState = SwitchState.Enabled(
                    value = isWebSocketEnabled,
                    onCheckedChange = setWebSocketState
                ),
                arrowType = ArrowType.NONE
            )
        }
    }
}

@Composable
@Preview
private fun NetworkSettingsScreenPreview() {
    NetworkSettingsScreenContent(
        {}, true, {}, ""
    )
}
