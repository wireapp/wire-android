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
package com.wire.android.ui.debug

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.BuildConfig
import com.wire.android.R
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.button.WireSwitch
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.rowitem.RowItemTemplate
import com.wire.android.ui.common.rowitem.SectionHeader
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@Preview
@Composable
private fun DebugToolsOptionsPreview() {
    WireTheme {
        DebugToolsOptions(
            isEventProcessingEnabled = true,
            onDisableEventProcessingChange = {},
            onRestartSlowSyncForRecovery = {},
            onForceUpdateApiVersions = {},
            checkCrlRevocationList = {},
            onResendFCMToken = {},
            isAsyncNotificationsEnabled = true,
            onEnableAsyncNotificationsChange = {}
        )
    }
}

@Composable
internal fun DebugToolsOptions(
    isEventProcessingEnabled: Boolean,
    onDisableEventProcessingChange: (Boolean) -> Unit,
    onRestartSlowSyncForRecovery: () -> Unit,
    onForceUpdateApiVersions: () -> Unit,
    checkCrlRevocationList: () -> Unit,
    onResendFCMToken: () -> Unit,
    isAsyncNotificationsEnabled: Boolean,
    onEnableAsyncNotificationsChange: (Boolean) -> Unit,
) {
    SectionHeader(stringResource(R.string.label_debug_tools_title))
    Column {
        if (BuildConfig.PRIVATE_BUILD) {
            PrivateBuildDebugToolsOptions(
                isEventProcessingEnabled = isEventProcessingEnabled,
                onDisableEventProcessingChange = onDisableEventProcessingChange,
                onRestartSlowSyncForRecovery = onRestartSlowSyncForRecovery,
                onForceUpdateApiVersions = onForceUpdateApiVersions,
                checkCrlRevocationList = checkCrlRevocationList,
                isAsyncNotificationsEnabled = isAsyncNotificationsEnabled,
                onEnableAsyncNotificationsChange = onEnableAsyncNotificationsChange
            )
        }
        ProductionDebugToolsOptions(onResendFCMToken = onResendFCMToken)
    }
}

@Composable
private fun PrivateBuildDebugToolsOptions(
    isEventProcessingEnabled: Boolean,
    onDisableEventProcessingChange: (Boolean) -> Unit,
    onRestartSlowSyncForRecovery: () -> Unit,
    onForceUpdateApiVersions: () -> Unit,
    checkCrlRevocationList: () -> Unit,
    isAsyncNotificationsEnabled: Boolean,
    onEnableAsyncNotificationsChange: (Boolean) -> Unit,
) {
    Column {
        DisableEventProcessingSwitch(
            isEnabled = isEventProcessingEnabled,
            onCheckedChange = onDisableEventProcessingChange
        )
        RestartSlowSyncButton(onClick = onRestartSlowSyncForRecovery)
        CheckCrlRevocationButton(onClick = checkCrlRevocationList)
        ForceUpdateApiVersionsButton(onClick = onForceUpdateApiVersions)
        EnableAsyncNotifications(isAsyncNotificationsEnabled, onEnableAsyncNotificationsChange)
    }
}

@Composable
private fun ProductionDebugToolsOptions(
    onResendFCMToken: () -> Unit,
) {
    RegisterFCMPushTokenButton(onClick = onResendFCMToken)
}

@Composable
private fun DisableEventProcessingSwitch(
    isEnabled: Boolean = false,
    onCheckedChange: ((Boolean) -> Unit)?,
) {
    RowItemTemplate(
        title = {
            Text(
                style = MaterialTheme.wireTypography.body01,
                color = MaterialTheme.wireColorScheme.onBackground,
                text = stringResource(R.string.label_disable_event_processing),
                modifier = Modifier.padding(start = dimensions().spacing8x)
            )
        },
        actions = {
            WireSwitch(
                checked = isEnabled,
                onCheckedChange = onCheckedChange,
                modifier = Modifier
                    .padding(end = dimensions().spacing8x)
                    .size(
                        width = dimensions().buttonSmallMinSize.width,
                        height = dimensions().buttonSmallMinSize.height
                    )
            )
        }
    )
}

@Composable
private fun RestartSlowSyncButton(
    onClick: () -> Unit,
) {
    RowItemTemplate(
        modifier = Modifier.wrapContentWidth(),
        title = {
            Text(
                style = MaterialTheme.wireTypography.body01,
                color = MaterialTheme.wireColorScheme.onBackground,
                text = stringResource(R.string.label_restart_slowsync_for_recovery),
                modifier = Modifier.padding(start = dimensions().spacing8x)
            )
        },
        actions = {
            WirePrimaryButton(
                minSize = MaterialTheme.wireDimensions.buttonMediumMinSize,
                minClickableSize = MaterialTheme.wireDimensions.buttonMinClickableSize,
                onClick = onClick,
                text = stringResource(R.string.restart_slowsync_for_recovery_button),
                fillMaxWidth = false
            )
        }
    )
}

@Composable
private fun CheckCrlRevocationButton(
    onClick: () -> Unit,
) {
    RowItemTemplate(
        modifier = Modifier.wrapContentWidth(),
        title = {
            Text(
                style = MaterialTheme.wireTypography.body01,
                color = MaterialTheme.wireColorScheme.onBackground,
                text = "CRL revocation check",
                modifier = Modifier.padding(start = dimensions().spacing8x)
            )
        },
        actions = {
            WirePrimaryButton(
                minSize = MaterialTheme.wireDimensions.buttonMediumMinSize,
                minClickableSize = MaterialTheme.wireDimensions.buttonMinClickableSize,
                onClick = onClick,
                text = stringResource(R.string.debug_settings_force_api_versioning_update_button_text),
                fillMaxWidth = false
            )
        }
    )
}

@Composable
private fun ForceUpdateApiVersionsButton(
    onClick: () -> Unit,
) {
    RowItemTemplate(
        modifier = Modifier.wrapContentWidth(),
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
                minSize = MaterialTheme.wireDimensions.buttonMediumMinSize,
                minClickableSize = MaterialTheme.wireDimensions.buttonMinClickableSize,
                onClick = onClick,
                text = stringResource(R.string.debug_settings_force_api_versioning_update_button_text),
                fillMaxWidth = false
            )
        }
    )
}

@Composable
private fun RegisterFCMPushTokenButton(
    onClick: () -> Unit,
) {
    RowItemTemplate(
        modifier = Modifier.wrapContentWidth(),
        title = {
            Text(
                style = MaterialTheme.wireTypography.body01,
                color = MaterialTheme.wireColorScheme.onBackground,
                text = stringResource(R.string.debug_settings_register_fcm_push_token),
                modifier = Modifier.padding(start = dimensions().spacing8x)
            )
        },
        actions = {
            WirePrimaryButton(
                minSize = MaterialTheme.wireDimensions.buttonMediumMinSize,
                minClickableSize = MaterialTheme.wireDimensions.buttonMinClickableSize,
                onClick = onClick,
                text = stringResource(R.string.debug_settings_force_api_versioning_update_button_text),
                fillMaxWidth = false
            )
        }
    )
}

@Composable
private fun EnableAsyncNotifications(
    isEnabled: Boolean = false,
    onCheckedChange: ((Boolean) -> Unit)?,
) {
    RowItemTemplate(
        title = {
            Text(
                style = MaterialTheme.wireTypography.body01,
                color = MaterialTheme.wireColorScheme.onBackground,
                text = stringResource(R.string.label_enable_async_notifications),
                modifier = Modifier.padding(start = dimensions().spacing8x)
            )
        },
        actions = {
            WireSwitch(
                enabled = !isEnabled,
                checked = isEnabled,
                onCheckedChange = onCheckedChange,
                modifier = Modifier
                    .padding(end = dimensions().spacing8x)
                    .size(
                        width = dimensions().buttonSmallMinSize.width,
                        height = dimensions().buttonSmallMinSize.height
                    )
            )
        }
    )
}