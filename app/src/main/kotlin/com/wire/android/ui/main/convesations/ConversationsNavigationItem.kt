package com.wire.android.ui.main.convesations

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.wire.android.R
import com.wire.android.ui.common.WireBottomNavigationItemData
import com.wire.android.ui.main.convesations.all.AllConversationsTab
import com.wire.android.ui.main.convesations.calls.CallConversationsTab
import com.wire.android.ui.main.convesations.mentions.MentionsConversationsTab

enum class ConversationsNavigationItem(
    @DrawableRes val icon: Int,
    @StringRes val title: Int,
    val route: String,
    val content: @Composable (NavBackStackEntry) -> Unit
) {
    All(
        R.drawable.ic_conversation,
        R.string.conversations_all_tab_title,
        "conversations_all",
        { AllConversationsTab() }),

    Calls(
        R.drawable.ic_call,
        R.string.conversations_calls_tab_title,
        "conversations_calls",
        { CallConversationsTab() }),

    Mentions(
        R.drawable.ic_mention,
        R.string.conversations_mentions_tab_title,
        "conversations_mentions",
        { MentionsConversationsTab() });

    fun intoBottomNavigationItemData(notificationAmount: Int): WireBottomNavigationItemData =
        WireBottomNavigationItemData(icon, title, notificationAmount, route, content)
}

@Composable
fun ConversationsNavigationGraph(navController: NavHostController) {
    NavHost(navController, startDestination = ConversationsNavigationItem.All.route) {

        ConversationsNavigationItem.values().forEach { item ->
            composable(route = item.route, content = item.content)
        }
    }
}
