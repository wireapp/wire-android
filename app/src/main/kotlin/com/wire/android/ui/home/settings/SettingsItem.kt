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

import com.wire.android.R
import com.wire.android.navigation.NavigationItem
import com.wire.android.util.ui.UIText

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
        navigationItem = NavigationItem.RemoveDevices
    ),
    PrivacySettings(
        id = "privacy_settings",
        title = UIText.StringResource(R.string.settings_privacy_settings_label),
        navigationItem = NavigationItem.PrivacySettings
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
    )
    ;
}
