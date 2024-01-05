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

package com.wire.android.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.ramcosta.composedestinations.spec.Direction
import com.wire.android.R
import com.wire.android.ui.common.WireBottomNavigationItemData
import com.wire.android.ui.destinations.AllConversationScreenDestination
import com.wire.android.ui.destinations.ArchiveScreenDestination
import com.wire.android.ui.destinations.CallsScreenDestination
import com.wire.android.ui.destinations.MentionScreenDestination
import com.wire.android.ui.destinations.SettingsScreenDestination
import com.wire.android.ui.destinations.VaultScreenDestination
import com.wire.android.ui.destinations.WhatsNewScreenDestination

@Suppress("LongParameterList")
sealed class HomeDestination(
    @StringRes val title: Int,
    @StringRes val tabName: Int = title,
    @DrawableRes val icon: Int,
    val isSearchable: Boolean = false,
    val withNewConversationFab: Boolean = false,
    val direction: Direction
) {
    data object Conversations : HomeDestination(
        title = R.string.conversations_screen_title,
        tabName = R.string.conversations_all_tab_title,
        icon = R.drawable.ic_conversation,
        isSearchable = true,
        withNewConversationFab = true,
        direction = AllConversationScreenDestination
    )

    data object Calls : HomeDestination(
        title = R.string.conversations_calls_tab_title,
        icon = R.drawable.ic_call,
        isSearchable = true,
        withNewConversationFab = true,
        direction = CallsScreenDestination
    )

    data object Mentions : HomeDestination(
        title = R.string.conversations_mentions_tab_title,
        icon = R.drawable.ic_mention,
        isSearchable = true,
        withNewConversationFab = true,
        direction = MentionScreenDestination
    )

    data object Settings : HomeDestination(
        title = R.string.settings_screen_title,
        icon = R.drawable.ic_settings,
        direction = SettingsScreenDestination
    )

    data object Vault : HomeDestination(
        title = R.string.vault_screen_title,
        icon = R.drawable.ic_vault,
        direction = VaultScreenDestination
    )

    data object Archive : HomeDestination(
        title = R.string.archive_screen_title,
        icon = R.drawable.ic_archive,
        isSearchable = true,
        direction = ArchiveScreenDestination
    )

    data object Support : HomeDestination(
        title = R.string.support_screen_title,
        icon = R.drawable.ic_support,
        direction = SupportScreenDestination
    )

    data object WhatsNew : HomeDestination(
        title = R.string.whats_new_screen_title,
        icon = R.drawable.ic_star,
        direction = WhatsNewScreenDestination
    )

    val withBottomTabs: Boolean get() = bottomTabItems.contains(this)

    fun toBottomNavigationItemData(notificationAmount: Long): WireBottomNavigationItemData =
        WireBottomNavigationItemData(icon, tabName, notificationAmount, direction.route)

    val itemName: String get() = ITEM_NAME_PREFIX + this

    companion object {
        // TODO uncomment when CallsScreen and MentionScreen will be implemented
//        val bottomTabItems = listOf(Conversations, Calls, Mentions)
        val bottomTabItems = listOf<HomeDestination>()

        private const val ITEM_NAME_PREFIX = "HomeNavigationItem."
        fun fromRoute(fullRoute: String): HomeDestination? =
            values().find { it.direction.route.getBaseRoute() == fullRoute.getBaseRoute() }
        fun values(): Array<HomeDestination> =
            arrayOf(Conversations, Calls, Mentions, Settings, Vault, Archive, Support, WhatsNew)
    }
}
