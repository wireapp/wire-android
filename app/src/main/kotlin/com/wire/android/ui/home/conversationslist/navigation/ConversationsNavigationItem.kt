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

package com.wire.android.ui.home.conversationslist.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.wire.android.R
import com.wire.android.ui.common.WireBottomNavigationItemData


enum class ConversationsNavigationItem(
    @DrawableRes val icon: Int,
    @StringRes val title: Int,
    val route: String,
) {
    All(
        R.drawable.ic_conversation,
        R.string.conversations_all_tab_title,
        "conversations_all"),

    Calls(
        R.drawable.ic_call,
        R.string.conversations_calls_tab_title,
        "conversations_calls"),

    Mentions(
        R.drawable.ic_mention,
        R.string.conversations_mentions_tab_title,
        "conversations_mentions");

    fun toBottomNavigationItemData(notificationAmount: Long): WireBottomNavigationItemData =
        WireBottomNavigationItemData(icon, title, notificationAmount, route)
}
