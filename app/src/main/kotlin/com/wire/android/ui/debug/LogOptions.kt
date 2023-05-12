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
package com.wire.android.ui.debug

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.ui.common.RowItemTemplate
import com.wire.android.ui.common.WireSwitch
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversationslist.common.FolderHeader
import com.wire.android.ui.home.settings.SettingsItem
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.getGitBuildId

@Composable
fun LogOptions(
    deviceId: String?,
    isLoggingEnabled: Boolean,
    onLoggingEnabledChange: (Boolean) -> Unit,
    onDeleteLogs: () -> Unit,
    onShareLogs: () -> Unit,
    onCopyDeviceId: (String) -> Unit
) {
    Column {
        FolderHeader(stringResource(R.string.label_logs_option_title))

        EnableLoggingSwitch(
            isEnabled = isLoggingEnabled,
            onCheckedChange = onLoggingEnabledChange
        )

        if (isLoggingEnabled) {
            SettingsItem(
                title = stringResource(R.string.label_share_logs),
                trailingIcon = R.drawable.ic_entypo_share,
                onIconPressed = Clickable(
                    enabled = true,
                    onClick = onShareLogs
                )
            )

            SettingsItem(
                title = stringResource(R.string.label_delete_logs),
                trailingIcon = R.drawable.ic_delete,
                onIconPressed = Clickable(
                    enabled = true,
                    onClick = onDeleteLogs
                )
            )
        }

        val codeBuildNumber = LocalContext.current.getGitBuildId()
        if (codeBuildNumber.isNotBlank()) {
            SettingsItem(title = stringResource(R.string.label_code_commit_id, codeBuildNumber))
        }

        if (deviceId != null) {
            SettingsItem(
                title = stringResource(R.string.label_device_id, deviceId),
                trailingIcon = R.drawable.ic_copy,
                onIconPressed = Clickable(
                    enabled = true,
                    onClick = { onCopyDeviceId(deviceId) }
                )
            )
        }
    }
}

@Composable
fun EnableLoggingSwitch(
    isEnabled: Boolean = false,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier
) {
    RowItemTemplate(
        title = {
            Text(
                style = MaterialTheme.wireTypography.body01,
                color = MaterialTheme.wireColorScheme.onBackground,
                text = stringResource(R.string.label_enable_logging),
                modifier = Modifier.padding(start = dimensions().spacing8x)
            )
        },
        actions = {
            WireSwitch(
                checked = isEnabled,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.padding(end = dimensions().spacing16x)
            )
        }
    )
}
