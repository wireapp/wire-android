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
package com.wire.android.ui.debug.dev

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.BuildConfig
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.ui.common.RowItemTemplate
import com.wire.android.ui.common.WireSwitch
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.debug.ClientIdOptions
import com.wire.android.ui.debug.DebugContentState
import com.wire.android.ui.debug.LogOptions
import com.wire.android.ui.debug.rememberDebugContentState
import com.wire.android.ui.home.conversationslist.common.FolderHeader
import com.wire.android.ui.home.settings.SettingsItem
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import kotlin.reflect.KFunction1


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevDebugContent() {
    val devDebugViewModel: DevDebugViewModel = hiltViewModel()
    val debugContentState: DebugContentState = rememberDebugContentState(devDebugViewModel.logPath)

    Scaffold(
        topBar = {
            WireCenterAlignedTopAppBar(
                title = stringResource(R.string.label_debug_title),
                elevation = 0.dp,
                navigationIconType = NavigationIconType.Back,
                onNavigationPressed = devDebugViewModel::navigateBack
            )
        }
    ) { internalPadding ->
        with(devDebugViewModel.state) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(debugContentState.scrollState)
                    .padding(internalPadding)
            ) {
                MlsOptions(
                    keyPackagesCount = keyPackagesCount,
                    mlsClientId = mslClientId,
                    mlsErrorMessage = mlsErrorMessage,
                    restartSlowSyncForRecovery = devDebugViewModel::restartSlowSyncForRecovery
                )

                if (BuildConfig.PRIVATE_BUILD) {
                    ProteusOptions(
                        isEncryptedStorageEnabled = isEncryptedProteusStorageEnabled,
                        onEncryptedStorageEnabledChange = { enabled ->
                            if (enabled) {
                                devDebugViewModel.enableEncryptedProteusStorage()
                            }
                        }
                    )
                }

                LogOptions(
                    isLoggingEnabled = isLoggingEnabled,
                    onLoggingEnabledChange = devDebugViewModel::setLoggingEnabledState,
                    onDeleteLogs = devDebugViewModel::deleteLogs,
                    onShareLogs = debugContentState::shareLogs
                )

                DebugDataOptions(
                    deviceId = deviceId
                            onCopyDeviceId = debugContentState ::copyToClipboard
                )


                Text(
                    text = stringResource(R.string.app_version, BuildConfig.VERSION_NAME),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(dimensions().spacing12x)
                )

                Text(
                    text = stringResource(
                        R.string.build_variant_name, "${BuildConfig.FLAVOR}${
                            BuildConfig.BUILD_TYPE.replaceFirstChar {
                                it.uppercase()
                            }
                        }"
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(dimensions().spacing12x)
                )

                DevelopmentApiVersioningOptions(
                    onForceLatestDevelopmentApiChange = devDebugViewModel::forceUpdateApiVersions
                )

                if (isManualMigrationAllowed) {
                    ManualMigrationOptions(
                        onManualMigrationClicked = devDebugViewModel::onStartManualMigration
                    )
                }
            }
        }
    }
}

@Composable
private fun DebugDataOptions(deviceId: String?, onCopyDeviceId: KFunction1<String, Unit>) {
    Column {
        FolderHeader(stringResource(R.string.label_logs_option_title))
        SettingsItem(
            title = currentClientId,
            trailingIcon = R.drawable.ic_copy,
            onIconPressed = Clickable(
                enabled = true,
                onClick = { onCopyClientId(currentClientId) }
            )
        )
        SettingsItem(
            title = deviceId ?: "",
            trailingIcon = R.drawable.ic_copy,
            onIconPressed = Clickable(
                enabled = true,
                onClick = { onCopyDeviceId(deviceId ?: "") }
            )
        )
        SettingsItem(
            title = stringResource(R.string.app_version, BuildConfig.VERSION_NAME),
            trailingIcon = R.drawable.ic_copy,
            onIconPressed = Clickable(
                enabled = true,
                onClick = { onCopyDeviceId(deviceId ?: "") }
            )
        )
        SettingsItem(
            title = stringResource(
                R.string.build_variant_name, "${BuildConfig.FLAVOR}${
                    BuildConfig.BUILD_TYPE.replaceFirstChar {
                        it.uppercase()
                    }
                }"
            ), trailingIcon = R.drawable.ic_copy,
            onIconPressed = Clickable(
                enabled = true,
                onClick = { onCopyDeviceId(deviceId ?: "") }
            )
        )
    }
}


@Composable
private fun ManualMigrationOptions(
    onManualMigrationClicked: () -> Unit,
) {
    Column {
        FolderHeader(stringResource(R.string.label_manual_migration_title))
        SettingsItem(
            title = stringResource(R.string.start_manual_migration),
            onRowPressed = Clickable(
                enabled = true,
                onClick = onManualMigrationClicked
            )
        )
    }
}

@Composable
private fun MlsOptions(
    keyPackagesCount: Int,
    mlsClientId: String,
    mlsErrorMessage: String,
    restartSlowSyncForRecovery: () -> Unit
) {
    if (mlsErrorMessage.isNotEmpty()) {
        SettingsItem(
            title = mlsErrorMessage
        )
    } else {
        Column {
            FolderHeader(
                name = stringResource(R.string.label_mls_option_title)
            )

            SettingsItem(
                title = stringResource(R.string.label_key_packages_count, keyPackagesCount)
            )

            SettingsItem(
                title = stringResource(R.string.label_mls_client_id, mlsClientId)
            )
            SettingsItem(
                title = stringResource(R.string.label_restart_slowsync_for_recovery),
                trailingIcon = R.drawable.ic_input_mandatory,
                onIconPressed = Clickable(
                    enabled = true,
                    onClick = restartSlowSyncForRecovery
                )
            )
        }
    }
}

@Composable
private fun EnableEncryptedProteusStorageSwitch(
    isEnabled: Boolean = false,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier
) {
    RowItemTemplate(
        title = {
            Text(
                style = MaterialTheme.wireTypography.body01,
                color = MaterialTheme.wireColorScheme.onBackground,
                text = stringResource(R.string.label_enable_encrypted_proteus_storage),
                modifier = Modifier.padding(start = dimensions().spacing8x)
            )
        },
        actions = {
            WireSwitch(
                checked = isEnabled,
                onCheckedChange = onCheckedChange,
                enabled = !isEnabled,
                modifier = Modifier.padding(end = dimensions().spacing16x)
            )
        }
    )
}

@Composable
private fun DevelopmentApiVersioningOptions(
    onForceLatestDevelopmentApiChange: () -> Unit
) {
    FolderHeader(stringResource(R.string.debug_settings_api_versioning_title))
    RowItemTemplate(modifier = Modifier.wrapContentWidth(),
        title = {
            Text(
                style = MaterialTheme.wireTypography.body01,
                color = MaterialTheme.wireColorScheme.onBackground,
                text = stringResource(R.string.debug_settings_force_api_versioning_update),
                modifier = Modifier.padding(start = dimensions().spacing8x)
            )
        },
        actions = {
            WirePrimaryButton(
                onClick = onForceLatestDevelopmentApiChange,
                text = stringResource(R.string.debug_settings_force_api_versioning_update_button_text),
                fillMaxWidth = false
            )
        }
    )
}

@Composable
private fun ProteusOptions(
    isEncryptedStorageEnabled: Boolean,
    onEncryptedStorageEnabledChange: (Boolean) -> Unit,
) {
    Column {
        FolderHeader(stringResource(R.string.label_proteus_option_title))

        EnableEncryptedProteusStorageSwitch(
            isEnabled = isEncryptedStorageEnabled,
            onCheckedChange = onEncryptedStorageEnabledChange
        )
    }
}
