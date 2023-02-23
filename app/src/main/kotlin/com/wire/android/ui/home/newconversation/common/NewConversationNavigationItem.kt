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

package com.wire.android.ui.home.newconversation.common

import com.wire.android.navigation.getPrimaryRoute

enum class NewConversationNavigationItem(val route: String) {
    NewGroupNameScreen("new_group_name"),
    SearchListNavHostScreens("search_list_nav_host"),
    GroupOptionsScreen("group_options_screen");

    val itemName: String get() = ITEM_NAME_PREFIX + this.name
    companion object {
        private const val ITEM_NAME_PREFIX = "NewConversationNavigationItem."
        private val map = NewConversationNavigationItem.values().associateBy { it.route }
        fun fromRoute(fullRoute: String): NewConversationNavigationItem? = map[fullRoute.getPrimaryRoute()]
    }
}
