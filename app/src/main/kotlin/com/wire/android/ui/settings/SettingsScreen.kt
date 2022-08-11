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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.ui.common.ArrowRightIcon
import com.wire.android.ui.common.RowItemTemplate
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
            ListWithHeader(stringResource(R.string.general)) {
                settingsItem(stringResource(R.string.your_account_label)) {}
                settingsItem(stringResource(R.string.app_settings_label)) {}
                settingsItem(stringResource(R.string.privacy_settings_label)) {}
                settingsItem(stringResource(R.string.network_settings_label)) {
                    settingsViewModel.navigateToNetworkSettings()
                }
            }

        }
        item {
            ListWithHeader(stringResource(R.string.devices)) {
                settingsItem(stringResource(R.string.manage_devices_label)) {}
            }
        }

        item {
            ListWithHeader(stringResource(R.string.backups)) {
                settingsItem(stringResource(R.string.back_up_label)) {}
            }
        }
        item {
            ListWithHeader(stringResource(R.string.others)) {
                settingsItem(stringResource(R.string.support_label)) {}
                settingsItem(stringResource(R.string.debug_settings_label)) {}
                settingsItem(stringResource(R.string.about_this_app_label)) {}
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
                    .padding(end = 8.dp)
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
