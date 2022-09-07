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
    )
}

@OptIn(
    ExperimentalMaterial3Api::class,
)
@Composable
fun NetworkSettingsScreenContent(
    onBackPressed: () -> Unit,
    isWebSocketEnabled: Boolean,
    setWebSocketState: (Boolean) -> Unit

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
private fun NewGroupScreenPreview() {
    NetworkSettingsScreenContent(
        {}, true, {}
    )
}
