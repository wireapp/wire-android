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
import com.wire.android.ui.destinations.AllConversationsScreenDestination
import com.wire.android.ui.destinations.ArchiveScreenDestination
import com.wire.android.ui.destinations.GlobalCellsScreenDestination
import com.wire.android.ui.destinations.MeetingsScreenDestination
import com.wire.android.ui.destinations.SettingsScreenDestination
import com.wire.android.ui.destinations.VaultScreenDestination
import com.wire.android.ui.destinations.WhatsNewScreenDestination
import com.wire.android.util.ui.UIText

@Suppress("LongParameterList")
sealed class HomeDestination(
    val title: UIText,
    @DrawableRes val icon: Int,
    val withNewConversationFab: Boolean = false,
    val withUserAvatar: Boolean = true,
    val withFilesFilterIcon: Boolean = false,
    val direction: Direction,
    val searchBar: SearchBarOptions? = null,
) {
    data object Conversations : HomeDestination(
        title = UIText.StringResource(R.string.conversations_screen_title),
        icon = R.drawable.ic_conversation,
        searchBar = SearchBarOptions(),
        withNewConversationFab = true,
        direction = AllConversationsScreenDestination
    )

    data object Settings : HomeDestination(
        title = UIText.StringResource(R.string.settings_screen_title),
        icon = R.drawable.ic_settings,
        withUserAvatar = false,
        direction = SettingsScreenDestination
    )

    data object Vault : HomeDestination(
        title = UIText.StringResource(R.string.vault_screen_title),
        icon = R.drawable.ic_vault,
        direction = VaultScreenDestination
    )

    data object Archive : HomeDestination(
        title = UIText.StringResource(R.string.archive_screen_title),
        icon = R.drawable.ic_archive,
        searchBar = SearchBarOptions(),
        direction = ArchiveScreenDestination
    )

    data object Support : HomeDestination(
        title = UIText.StringResource(R.string.support_screen_title),
        icon = R.drawable.ic_support,
        direction = SupportScreenDestination
    )

    data object WhatsNew : HomeDestination(
        title = UIText.StringResource(R.string.whats_new_screen_title),
        icon = R.drawable.ic_star,
        direction = WhatsNewScreenDestination
    )

    data object TeamManagement : HomeDestination(
        title = UIText.StringResource(R.string.team_management_screen_title),
        icon = R.drawable.ic_team_management,
        direction = TeamManagementScreenDestination
    )

    data object Cells : HomeDestination(
        title = UIText.StringResource(R.string.cells_screen_title),
        icon = R.drawable.ic_files,
        searchBar = SearchBarOptions(R.string.cells_screen_search_hint),
        withFilesFilterIcon = true,
        direction = GlobalCellsScreenDestination
    )

    data object Meetings : HomeDestination(
        title = UIText.StringResource(R.string.meetings_screen_title),
        icon = com.wire.android.ui.common.R.drawable.ic_video_call,
        direction = MeetingsScreenDestination
    )

    data class SearchBarOptions(
        @StringRes
        val hint: Int = R.string.search_bar_conversations_hint
    )

    val itemName: String get() = ITEM_NAME_PREFIX + this

    companion object {
        private const val ITEM_NAME_PREFIX = "HomeNavigationItem."

        fun fromRoute(fullRoute: String): HomeDestination? =
            values().find { it.direction.route.getBaseRoute() == fullRoute.getBaseRoute() }

        fun values(): Array<HomeDestination> =
            arrayOf(Conversations, Settings, Vault, Archive, Support, TeamManagement, WhatsNew, Cells, Meetings)
    }
}
