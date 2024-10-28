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
import com.wire.android.ui.destinations.FavoritesConversationsScreenDestination
import com.wire.android.ui.destinations.GroupConversationsScreenDestination
import com.wire.android.ui.destinations.OneOnOneConversationsScreenDestination
import com.wire.android.ui.destinations.SettingsScreenDestination
import com.wire.android.ui.destinations.VaultScreenDestination
import com.wire.android.ui.destinations.WhatsNewScreenDestination
import com.wire.kalium.logic.data.conversation.ConversationFilter

@Suppress("LongParameterList")
sealed class HomeDestination(
    @StringRes val title: Int,
    @DrawableRes val icon: Int,
    val isSearchable: Boolean = false,
    val withNewConversationFab: Boolean = false,
    val direction: Direction
) {
    data object Conversations : HomeDestination(
        title = R.string.conversations_screen_title,
        icon = R.drawable.ic_conversation,
        isSearchable = true,
        withNewConversationFab = true,
        direction = AllConversationsScreenDestination
    )

    data object Favorites : HomeDestination(
        title = R.string.label_filter_favorites,
        icon = R.drawable.ic_conversation,
        isSearchable = true,
        withNewConversationFab = true,
        direction = FavoritesConversationsScreenDestination
    )

    data object Group : HomeDestination(
        title = R.string.label_filter_group,
        icon = R.drawable.ic_conversation,
        isSearchable = true,
        withNewConversationFab = true,
        direction = GroupConversationsScreenDestination
    )

    data object OneOnOne : HomeDestination(
        title = R.string.label_filter_one_on_one,
        icon = R.drawable.ic_conversation,
        isSearchable = true,
        withNewConversationFab = true,
        direction = OneOnOneConversationsScreenDestination
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

    val itemName: String get() = ITEM_NAME_PREFIX + this

    companion object {
        private const val ITEM_NAME_PREFIX = "HomeNavigationItem."
        fun fromRoute(fullRoute: String): HomeDestination? =
            values().find { it.direction.route.getBaseRoute() == fullRoute.getBaseRoute() }

        fun values(): Array<HomeDestination> =
            arrayOf(Conversations, Favorites, Group, OneOnOne, Settings, Vault, Archive, Support, WhatsNew)
    }
}

fun HomeDestination.currentFilter(): ConversationFilter {
    return when (this) {
        HomeDestination.Conversations -> ConversationFilter.ALL
        HomeDestination.Favorites -> ConversationFilter.FAVORITES
        HomeDestination.Group -> ConversationFilter.GROUPS
        HomeDestination.OneOnOne -> ConversationFilter.ONE_ON_ONE
        HomeDestination.Archive,
        HomeDestination.Settings,
        HomeDestination.Support,
        HomeDestination.Vault,
        HomeDestination.WhatsNew -> ConversationFilter.ALL
    }
}

fun ConversationFilter.toDestination(): HomeDestination {
    return when (this) {
        ConversationFilter.ALL -> HomeDestination.Conversations
        ConversationFilter.FAVORITES -> HomeDestination.Favorites
        ConversationFilter.GROUPS -> HomeDestination.Group
        ConversationFilter.ONE_ON_ONE -> HomeDestination.OneOnOne
    }
}
