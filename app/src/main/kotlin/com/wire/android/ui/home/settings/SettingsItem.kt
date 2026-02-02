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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import com.ramcosta.composedestinations.spec.Direction
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.navigation.GiveFeedbackDestination
import com.wire.android.navigation.PrivacyPolicyScreenDestination
import com.wire.android.navigation.ReportBugDestination
import com.wire.android.navigation.SupportScreenDestination
import com.wire.android.navigation.TermsOfUseScreenDestination
import com.wire.android.navigation.WireWebsiteScreenDestination
import com.wire.android.ui.common.clickable
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.rowitem.RowItemTemplate
import com.wire.android.ui.destinations.AboutThisAppScreenDestination
import com.wire.android.ui.destinations.AppSettingsScreenDestination
import com.wire.android.ui.destinations.BackupAndRestoreScreenDestination
import com.wire.android.ui.destinations.CustomizationScreenDestination
import com.wire.android.ui.destinations.DebugScreenDestination
import com.wire.android.ui.destinations.DependenciesScreenDestination
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
    text: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    maxTitleLines: Int = 1,
    wrapTitleContentWidth: Boolean = true,
    @DrawableRes trailingIcon: Int? = null,
    trailingText: String? = null,
    switchState: SwitchState = SwitchState.None,
    onRowPressed: Clickable = Clickable(false),
    onIconPressed: Clickable? = null
) {
    RowItemTemplate(
        modifier = modifier,
        wrapTitleContentWidth = wrapTitleContentWidth,
        title = {
            if (!title.isNullOrBlank()) {
                Text(
                    style = MaterialTheme.wireTypography.label01,
                    color = MaterialTheme.wireColorScheme.secondaryText,
                    text = title,
                    maxLines = maxTitleLines,
                    modifier = Modifier.padding(start = dimensions().spacing8x)
                )
            }
            Text(
                style = MaterialTheme.wireTypography.body01,
                color = MaterialTheme.wireColorScheme.onBackground,
                text = text,
                maxLines = 1,
                modifier = Modifier.padding(start = dimensions().spacing8x)
            )
        },
        actions = {
            Row(
                horizontalArrangement = Arrangement.End,
            ) {
                SettingsOptionSwitch(switchState = switchState)
                if (trailingText != null) {
                    Row(
                        Modifier
                            .padding(end = dimensions().spacing12x)
                            .weight(1f),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            trailingText,
                            style = MaterialTheme.wireTypography.body01,
                            color = MaterialTheme.wireColorScheme.secondaryText,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                        )
                    }
                }
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
                }
            }
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

    data object Customization : DirectionItem(
        id = "customization_settings",
        title = UIText.StringResource(R.string.settings_customization_label),
        direction = CustomizationScreenDestination
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

    data object TermsOfUse : DirectionItem(
        id = "terms_of_use",
        title = UIText.StringResource(R.string.settings_terms_of_use_label),
        direction = TermsOfUseScreenDestination
    )

    data object WireWebsite : DirectionItem(
        id = "terms_of_use",
        title = UIText.StringResource(R.string.settings_wire_website_label),
        direction = WireWebsiteScreenDestination
    )

    data object PrivacyPolicy : DirectionItem(
        id = "privacy_policy",
        title = UIText.StringResource(R.string.settings_privacy_policy_label),
        direction = PrivacyPolicyScreenDestination
    )

    data object Licenses : DirectionItem(
        id = "other_licenses",
        title = UIText.StringResource(R.string.settings_licenses_settings_label),
        direction = LicensesScreenDestination
    )

    data object Dependencies : DirectionItem(
        id = "other_licenses",
        title = UIText.StringResource(R.string.settings_dependencies_label),
        direction = DependenciesScreenDestination
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

    data object AboutApp : DirectionItem(
        id = "about_app",
        title = UIText.StringResource(R.string.about_app_screen_title),
        direction = AboutThisAppScreenDestination
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewSettingsItem() {
    WireTheme {
        SettingsItem(
            title = "Some Setting",
            text = "This is the value of the setting",
            trailingIcon = R.drawable.ic_arrow_right
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewSettingsItemTrailingComposable() {
    WireTheme {
        SettingsItem(
            title = "Some Setting",
            text = "This is the value of the setting",
            trailingIcon = R.drawable.ic_arrow_right,
            trailingText = "Longlonglonglonglonglonglonglong Name"
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewSettingsItemTrailingShortComposable() {
    WireTheme {
        SettingsItem(
            title = "Some Setting",
            text = "This is the value of the setting",
            trailingIcon = R.drawable.ic_arrow_right,
            trailingText = "Short Name"
        )
    }
}
