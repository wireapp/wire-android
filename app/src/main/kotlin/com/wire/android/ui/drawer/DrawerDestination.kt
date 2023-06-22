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
package com.wire.android.ui.drawer

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.ramcosta.composedestinations.spec.Direction
import com.wire.android.BuildConfig
import com.wire.android.R
import com.wire.android.navigation.SupportScreenDestination
import com.wire.android.ui.destinations.AllConversationScreenDestination
import com.wire.android.ui.destinations.ArchiveScreenDestination
import com.wire.android.ui.destinations.SettingsScreenDestination
import com.wire.android.ui.destinations.VaultScreenDestination

enum class DrawerDestination(
    val direction: Direction? = null,
    @DrawableRes val icon: Int,
    @StringRes val label: Int,
    val isVisible: Boolean = true,
    val shouldAddSpacer: Boolean = false // should be true for the last item of top section
) {
    Conversations(
        direction = AllConversationScreenDestination,
        icon = R.drawable.ic_conversation,
        label = R.string.conversations_screen_title,
        shouldAddSpacer = true
    ),
    Settings(
        direction = SettingsScreenDestination,
        icon = R.drawable.ic_settings,
        label = R.string.settings_screen_title
    ),
    Vault(
        direction = VaultScreenDestination,
        icon = R.drawable.ic_vault,
        label = R.string.vault_screen_title,
        isVisible = false
    ),
    Archive(
        direction = ArchiveScreenDestination,
        icon = R.drawable.ic_archive,
        label = R.string.archive_screen_title,
        isVisible = false
    ),
    Support(
        direction = SupportScreenDestination,
        icon = R.drawable.ic_support,
        label = R.string.support_screen_title
    ),
    Feedback(
        icon = R.drawable.ic_emoticon,
        label = R.string.give_feedback_screen_title
    ),
    ReportBug(
        icon = R.drawable.ic_bug,
        label = R.string.report_bug_screen_title,
        isVisible = BuildConfig.REPORT_BUG_MENU_ITEM_ENABLED
    )
}
