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
