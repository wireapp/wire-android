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
import androidx.navigation.NavBackStackEntry
import com.ramcosta.composedestinations.spec.Direction
import com.wire.android.R
import com.wire.android.ui.destinations.AllConversationsScreenDestination
import com.wire.android.ui.destinations.ArchiveScreenDestination
import com.wire.android.ui.destinations.FavoritesConversationsScreenDestination
import com.wire.android.ui.destinations.FolderConversationsScreenDestination
import com.wire.android.ui.destinations.GroupConversationsScreenDestination
import com.wire.android.ui.destinations.OneOnOneConversationsScreenDestination
import com.wire.android.ui.destinations.SettingsScreenDestination
import com.wire.android.ui.destinations.VaultScreenDestination
import com.wire.android.ui.destinations.WhatsNewScreenDestination
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.conversation.ConversationFilter
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

@Suppress("LongParameterList")
sealed class HomeDestination(
    val title: UIText,
    @DrawableRes val icon: Int,
    val isSearchable: Boolean = false,
    val withNewConversationFab: Boolean = false,
    val withUserAvatar: Boolean = true,
    val direction: Direction
) {

    internal fun NavBackStackEntry.baseRouteMatches(): Boolean = direction.route.getBaseRoute() == destination.route?.getBaseRoute()
    open fun entryMatches(entry: NavBackStackEntry): Boolean = entry.baseRouteMatches()

    data object Conversations : HomeDestination(
        title = UIText.StringResource(R.string.conversations_screen_title),
        icon = R.drawable.ic_conversation,
        isSearchable = true,
        withNewConversationFab = true,
        direction = AllConversationsScreenDestination
    )

    data object Favorites : HomeDestination(
        title = UIText.StringResource(R.string.label_filter_favorites),
        icon = R.drawable.ic_conversation,
        isSearchable = true,
        withNewConversationFab = true,
        direction = FavoritesConversationsScreenDestination
    )

    data class Folder(
        val folderNavArgs: FolderNavArgs
    ) : HomeDestination(
        title = UIText.DynamicString(folderNavArgs.folderName),
        icon = R.drawable.ic_conversation,
        isSearchable = true,
        withNewConversationFab = true,
        direction = FolderConversationsScreenDestination(folderNavArgs)
    ) {
        override fun entryMatches(entry: NavBackStackEntry): Boolean =
            entry.baseRouteMatches() && FolderConversationsScreenDestination.argsFrom(entry).folderId == folderNavArgs.folderId
    }

    data object Group : HomeDestination(
        title = UIText.StringResource(R.string.label_filter_group),
        icon = R.drawable.ic_conversation,
        isSearchable = true,
        withNewConversationFab = true,
        direction = GroupConversationsScreenDestination
    )

    data object OneOnOne : HomeDestination(
        title = UIText.StringResource(R.string.label_filter_one_on_one),
        icon = R.drawable.ic_conversation,
        isSearchable = true,
        withNewConversationFab = true,
        direction = OneOnOneConversationsScreenDestination
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
        isSearchable = true,
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

    val itemName: String get() = ITEM_NAME_PREFIX + this

    companion object {
        private const val ITEM_NAME_PREFIX = "HomeNavigationItem."
        fun values(): PersistentList<HomeDestination> =
            persistentListOf(Conversations, Favorites, Group, OneOnOne, Settings, Vault, Archive, Support, WhatsNew)
    }
}

fun HomeDestination.currentFilter(): ConversationFilter {
    return when (this) {
        HomeDestination.Conversations -> ConversationFilter.All
        HomeDestination.Favorites -> ConversationFilter.Favorites
        HomeDestination.Group -> ConversationFilter.Groups
        HomeDestination.OneOnOne -> ConversationFilter.OneOnOne
        is HomeDestination.Folder -> ConversationFilter.Folder(folderName = folderNavArgs.folderName, folderId = folderNavArgs.folderId)
        HomeDestination.Archive,
        HomeDestination.Settings,
        HomeDestination.Support,
        HomeDestination.Vault,
        HomeDestination.WhatsNew -> ConversationFilter.All
    }
}

fun ConversationFilter.toDestination(): HomeDestination {
    return when (this) {
        ConversationFilter.All -> HomeDestination.Conversations
        ConversationFilter.Favorites -> HomeDestination.Favorites
        ConversationFilter.Groups -> HomeDestination.Group
        ConversationFilter.OneOnOne -> HomeDestination.OneOnOne
        is ConversationFilter.Folder -> HomeDestination.Folder(FolderNavArgs(folderId, folderName))
    }
}
