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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.ui.common.rowitem.RowItemTemplate
import com.wire.android.ui.common.SurfaceBackgroundWrapper
import com.wire.android.ui.common.button.WireSwitch
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.typography
import com.wire.android.ui.common.rowitem.SectionHeader
import com.wire.android.ui.home.settings.SettingsItem
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun LogOptions(
    isLoggingEnabled: Boolean,
    onLoggingEnabledChange: (Boolean) -> Unit,
    isDBLoggerEnabled: Boolean,
    onDBLoggerEnabledChange: (Boolean) -> Unit,
    onDeleteLogs: () -> Unit,
    onShareLogs: () -> Unit,
    isPrivateBuild: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        SectionHeader(stringResource(R.string.label_logs_option_title))
        EnableLoggingSwitch(
            isEnabled = isLoggingEnabled,
            onCheckedChange = onLoggingEnabledChange
        )

        if (isPrivateBuild) {
            UserDataBaseProfileSwitch(
                isEnabled = isDBLoggerEnabled,
                onCheckedChange = onDBLoggerEnabledChange
            )
        }
        SettingsItem(
            text = stringResource(R.string.label_share_logs),
            trailingIcon = R.drawable.ic_entypo_share,
            onIconPressed = Clickable(
                enabled = true,
                onClick = onShareLogs
            )
        )

        SettingsItem(
            text = stringResource(R.string.label_delete_logs),
            trailingIcon = com.wire.android.ui.common.R.drawable.ic_delete,
            onIconPressed = Clickable(
                enabled = true,
                onClick = onDeleteLogs
            )
        )
    }
}

@Composable
private fun EnableLoggingSwitch(
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = false
) {
    SurfaceBackgroundWrapper {
        Column(Modifier.padding(dimensions().spacing16x)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = modifier
                    .height(MaterialTheme.wireDimensions.conversationItemRowHeight)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        style = MaterialTheme.wireTypography.body01,
                        color = MaterialTheme.wireColorScheme.onBackground,
                        text = stringResource(R.string.label_enable_logging),
                    )
                }
                Box(
                    modifier = Modifier.wrapContentWidth()
                ) {
                    WireSwitch(
                        checked = isEnabled,
                        onCheckedChange = onCheckedChange,
                        modifier = Modifier.size(dimensions().buttonSmallMinSize)
                    )
                }
            }
            Text(
                text = stringResource(R.string.label_log_options_description),
                color = colorsScheme().secondaryText,
                style = typography().body01
            )
        }
    }
}

@Composable
private fun UserDataBaseProfileSwitch(
    isEnabled: Boolean,
    onCheckedChange: ((Boolean) -> Unit),
) {
    RowItemTemplate(
        title = {
            Text(
                style = MaterialTheme.wireTypography.body01,
                color = MaterialTheme.wireColorScheme.onBackground,
                text = stringResource(R.string.label_user_database_profile),
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

@PreviewMultipleThemes
@Composable
fun PreviewLoggingOptionsPublicBuild() {
    LogOptions(
        isLoggingEnabled = true,
        onLoggingEnabledChange = {},
        isDBLoggerEnabled = true,
        onDBLoggerEnabledChange = {},
        onDeleteLogs = {},
        onShareLogs = {},
        isPrivateBuild = false,
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewLoggingOptionsPrivateBuild() {
    LogOptions(
        isLoggingEnabled = true,
        onLoggingEnabledChange = {},
        isDBLoggerEnabled = true,
        onDBLoggerEnabledChange = {},
        onDeleteLogs = {},
        onShareLogs = {},
        isPrivateBuild = true,
    )
}
