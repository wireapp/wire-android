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
package com.wire.android.ui.bottombar

import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.ramcosta.composedestinations.spec.Direction
import com.wire.android.R
import com.wire.android.ui.destinations.AllConversationScreenDestination
import com.wire.android.ui.destinations.CallsScreenDestination
import com.wire.android.ui.destinations.HomeScreenDestination
import com.wire.android.ui.destinations.MentionScreenDestination
import com.wire.android.ui.destinations.WelcomeScreenDestination
import com.wire.android.ui.home.rememberHomeScreenState

enum class BottomBarDestination(
    val direction: Direction,
    @DrawableRes val icon: Int,
    @StringRes val label: Int,
    @StringRes val topAppBarTitle: Int,
    val isVisible: Boolean = true
) {
    Conversations(
        direction = AllConversationScreenDestination,
        icon = R.drawable.ic_conversation,
        label = R.string.conversations_all_tab_title,
        topAppBarTitle = R.string.conversations_screen_title
    ),
    Calls(
        direction = CallsScreenDestination,
        icon = R.drawable.ic_call,
        label = R.string.conversations_calls_tab_title,
        topAppBarTitle = R.string.conversations_calls_tab_title
    ),
    Mentions(
        direction = MentionScreenDestination,
        icon = R.drawable.ic_mention,
        label = R.string.conversations_mentions_tab_title,
        topAppBarTitle = R.string.conversations_mentions_tab_title
    )
}
