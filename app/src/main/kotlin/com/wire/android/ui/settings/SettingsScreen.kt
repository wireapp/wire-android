package com.wire.android.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.ui.common.ArrowRightIcon
import com.wire.android.ui.common.RowItemTemplate
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.debugscreen.ListWithHeader
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography

@Composable
fun SettingsScreen(settingsViewModel: SettingsViewModel = hiltViewModel()) {
    val lazyListState = rememberLazyListState()

    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxWidth()
    ) {
        item {
            ListWithHeader(stringResource(R.string.settings_general_label).uppercase()) {
                settingsItem(stringResource(R.string.settings_your_account_label)) {}
                settingsItem(stringResource(R.string.settings_app_settings_label)) {}
                settingsItem(stringResource(R.string.settings_privacy_settings_label)) {}
                settingsItem(stringResource(R.string.settings_network_settings_label)) {
                    settingsViewModel.navigateToNetworkSettings()
                }
            }
        }
        item {
            ListWithHeader(stringResource(R.string.settings_devices_label).uppercase()) {
                settingsItem(stringResource(R.string.settings_manage_devices_label)) {}
            }
        }

        item {
            ListWithHeader(stringResource(R.string.settings_backups_label).uppercase()) {
                settingsItem(stringResource(R.string.settings_back_up_label)) {}
            }
        }
        item {
            ListWithHeader(stringResource(R.string.settings_others_label).uppercase()) {
                settingsItem(stringResource(R.string.settings_support_label)) {}
                settingsItem(stringResource(R.string.settings_debug_settings_label)) {}
                settingsItem(stringResource(R.string.settings_about_this_app_label)) {}
            }
        }
    }
}

@Composable
fun settingsItem(text: String, onClick: () -> Unit) {
    RowItemTemplate(
        title = {
            Text(
                style = MaterialTheme.wireTypography.body01,
                color = MaterialTheme.wireColorScheme.labelText,
                text = text
            )
        },

        clickable = Clickable(
            enabled = true,
            onClick = onClick,
        ),
        actions = {
            Box(
                modifier = Modifier
                    .wrapContentWidth()
                    .padding(end = dimensions().spacing16x)
            ) {
                ArrowRightIcon(Modifier.align(Alignment.TopEnd))
            }
        }
    )

}

@Preview(showBackground = false)
@Composable
fun SettingsScreenPreview() {
    SettingsScreen()
}
