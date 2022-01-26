package com.wire.android.ui.main.conversations.navigation

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

    fun toBottomNavigationItemData(notificationAmount: Int): WireBottomNavigationItemData =
        WireBottomNavigationItemData(icon, title, notificationAmount, route)
}
