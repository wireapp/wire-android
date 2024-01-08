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
import com.ramcosta.composedestinations.spec.Direction
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.navigation.GiveFeedbackDestination
import com.wire.android.navigation.ReportBugDestination
import com.wire.android.navigation.SupportScreenDestination
import com.wire.android.ui.common.RowItemTemplate
import com.wire.android.ui.common.clickable
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.destinations.AppSettingsScreenDestination
import com.wire.android.ui.destinations.AppearanceScreenDestination
import com.wire.android.ui.destinations.BackupAndRestoreScreenDestination
import com.wire.android.ui.destinations.DebugScreenDestination
import com.wire.android.ui.destinations.LicensesScreenDestination
import com.wire.android.ui.destinations.MyAccountScreenDestination
import com.wire.android.ui.destinations.NetworkSettingsScreenDestination
import com.wire.android.ui.destinations.PrivacySettingsConfigScreenDestination
import com.wire.android.ui.destinations.SelfDevicesScreenDestination
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
    switchState: SwitchState = SwitchState.None,
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
            SettingsOptionSwitch(switchState = switchState)
            trailingIcon?.let {
                Icon(
                    painter = painterResource(id = trailingIcon),
                    contentDescription = "",
                    tint = MaterialTheme.wireColorScheme.onSecondaryButtonEnabled,
                    modifier = Modifier
                        .defaultMinSize(dimensions().wireIconButtonSize)
                        .padding(end = dimensions().spacing8x)
                        .clickable(onIconPressed)
                )
            } ?: Icons.Filled.ChevronRight
        },
        clickable = onRowPressed
    )
}

sealed class SettingsItem(open val id: String, open val title: UIText) {

    sealed class DirectionItem(
        val direction: Direction,
        override val id: String,
        override val title: UIText
    ) : SettingsItem(id, title)

    sealed class SwitchItem(
        open val switchState: SwitchState,
        override val id: String,
        override val title: UIText
    ) : SettingsItem(id, title)

    data object AppSettings : DirectionItem(
        id = "general_app_settings",
        title = UIText.StringResource(R.string.app_settings_screen_title),
        direction = AppSettingsScreenDestination
    )

    data object YourAccount : DirectionItem(
        id = "your_account_settings",
        title = UIText.StringResource(R.string.settings_your_account_label),
        direction = MyAccountScreenDestination
    )

    data object Appearance : DirectionItem(
        id = "appearance_settings",
        title = UIText.StringResource(R.string.settings_appearance_label),
        direction = AppearanceScreenDestination
    )

    data object NetworkSettings : DirectionItem(
        id = "network_settings",
        title = UIText.StringResource(R.string.settings_network_settings_label),
        direction = NetworkSettingsScreenDestination
    )

    data object ManageDevices : DirectionItem(
        id = "manage_devices",
        title = UIText.StringResource(R.string.settings_manage_devices_label),
        direction = SelfDevicesScreenDestination
    )

    data object PrivacySettings : DirectionItem(
        id = "privacy_settings",
        title = UIText.StringResource(R.string.settings_privacy_settings_label),
        direction = PrivacySettingsConfigScreenDestination
    )

    data object Licenses : DirectionItem(
        id = "other_licenses",
        title = UIText.StringResource(R.string.settings_licenses_settings_label),
        direction = LicensesScreenDestination
    )

    data object BackupAndRestore : DirectionItem(
        id = "backups_backup_and_restore",
        title = UIText.StringResource(R.string.backup_and_restore_screen_title),
        direction = BackupAndRestoreScreenDestination
    )

    data object Support : DirectionItem(
        id = "other_support",
        title = UIText.StringResource(R.string.support_screen_title),
        direction = SupportScreenDestination
    )

    data object DebugSettings : DirectionItem(
        id = "other_debug_settings",
        title = UIText.StringResource(R.string.debug_settings_screen_title),
        direction = DebugScreenDestination
    )

    data object GiveFeedback : DirectionItem(
        id = "give_feedback",
        title = UIText.StringResource(R.string.give_feedback_screen_title),
        direction = GiveFeedbackDestination
    )

    data object ReportBug : DirectionItem(
        id = "report_bug",
        title = UIText.StringResource(R.string.report_bug_screen_title),
        direction = ReportBugDestination
    )

    data class AppLock(override val switchState: SwitchState) : SwitchItem(
        switchState = switchState,
        id = "app_lock",
        title = UIText.StringResource(R.string.settings_app_lock_title),
    )
}

@PreviewMultipleThemes
@Composable
fun previewFileRestrictionDialog() {
    WireTheme {
        SettingsItem(
            title = "Some Setting",
            text = "This is the value of the setting",
            trailingIcon = R.drawable.ic_arrow_right
        )
    }
}
