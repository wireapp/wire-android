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

package com.wire.android.ui.home.settings

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.navigation.NavigationItem
import com.wire.android.ui.common.RowItemTemplate
import com.wire.android.ui.common.clickable
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.util.ui.UIText

@Composable
fun SettingsItem(
    title: String? = null,
    text: String,
    @DrawableRes trailingIcon: Int? = null,
    onRowPressed: Clickable = Clickable(false),
    onIconPressed: Clickable = Clickable(false)
) {
    RowItemTemplate(
        title = {
            if (!title.isNullOrBlank()) {
                Text(
                    style = MaterialTheme.wireTypography.label01,
                    color = MaterialTheme.wireColorScheme.secondaryText,
                    text = title,
                    modifier = Modifier.padding(start = dimensions().spacing8x)
                )
            }
            Text(
                style = MaterialTheme.wireTypography.body01,
                color = MaterialTheme.wireColorScheme.onBackground,
                text = text,
                modifier = Modifier.padding(start = dimensions().spacing8x)
            )
        },
        actions = {
            trailingIcon?.let {
                Icon(
                    painter = painterResource(id = trailingIcon),
                    contentDescription = "",
                    tint = MaterialTheme.wireColorScheme.onSecondaryButtonEnabled,
                    modifier = Modifier
                        .defaultMinSize(80.dp)
                        .clickable(onIconPressed)
                )
            } ?: Icons.Filled.ChevronRight
        },
        clickable = onRowPressed
    )
}

enum class SettingsItem(val id: String, val title: UIText, val navigationItem: NavigationItem) {
    AppSettings(
        id = "general_app_settings",
        title = UIText.StringResource(R.string.app_settings_screen_title),
        navigationItem = NavigationItem.AppSettings
    ),
    YourAccount(
        id = "your_account_settings",
        title = UIText.StringResource(R.string.settings_your_account_label),
        navigationItem = NavigationItem.MyAccount
    ),
    NetworkSettings(
        id = "network_settings",
        title = UIText.StringResource(R.string.settings_network_settings_label),
        navigationItem = NavigationItem.NetworkSettings
    ),
    ManageDevices(
        id = "manage_devices",
        title = UIText.StringResource(R.string.settings_manage_devices_label),
        navigationItem = NavigationItem.SelfDevices
    ),
    PrivacySettings(
        id = "privacy_settings",
        title = UIText.StringResource(R.string.settings_privacy_settings_label),
        navigationItem = NavigationItem.PrivacySettings
    ),
    Licenses(
        id = "other_licenses",
        title = UIText.StringResource(R.string.settings_licenses_settings_label),
        navigationItem = NavigationItem.Licenses
    ),
    BackupAndRestore(
        id = "backups_backup_and_restore",
        title = UIText.StringResource(R.string.backup_and_restore_screen_title),
        navigationItem = NavigationItem.BackupAndRestore
    ),
    Support(
        id = "other_support",
        title = UIText.StringResource(R.string.support_screen_title),
        navigationItem = NavigationItem.Support
    ),
    DebugSettings(
        id = "other_debug_settings",
        title = UIText.StringResource(R.string.debug_settings_screen_title),
        navigationItem = NavigationItem.Debug
    );
}

@PreviewMultipleThemes
@Composable
fun previewFileRestrictionDialog() {
    WireTheme {
        SettingsItem(
            title = "Some Setting",
            text = "This is the value of the setting",
            trailingIcon = R.drawable.ic_copy
        )
    }
}
