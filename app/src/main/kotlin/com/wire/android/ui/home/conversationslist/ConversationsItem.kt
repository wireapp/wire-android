package com.wire.android.ui.home.conversationslist

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.wire.android.R
import com.wire.android.ui.common.WireBottomTabItemData


enum class ConversationsItem(
    @DrawableRes val icon: Int,
    @StringRes val title: Int
) {
    All(
        R.drawable.ic_conversation,
        R.string.conversations_all_tab_title,
),

    Calls(
        R.drawable.ic_call,
        R.string.conversations_calls_tab_title,
),

    Mentions(
        R.drawable.ic_mention,
        R.string.conversations_mentions_tab_title,
);

    fun toBottomNavigationItemData(notificationAmount: Int): WireBottomTabItemData =
        WireBottomTabItemData(icon, title, notificationAmount)
}
