package com.wire.android.ui.home.conversationslist

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wire.android.R
import com.wire.android.ui.common.FloatingActionButton
import com.wire.android.ui.common.WireBottomNavigationBar
import com.wire.android.ui.common.WireBottomNavigationItemData
import com.wire.android.ui.main.conversationlist.navigation.ConversationsNavigationItem


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationRoute(conversationListViewModel: ConversationListViewModel = hiltViewModel()) {
    val uiState by conversationListViewModel.listState.collectAsState()
    val navController = rememberNavController()

    Scaffold(
        floatingActionButton = { FloatingActionButton(stringResource(R.string.label_new), {}) },
        bottomBar = { WireBottomNavigationBar(ConversationNavigationItems(uiState), navController) }
    ) {
        with(uiState) {
            NavHost(navController, startDestination = ConversationsNavigationItem.All.route) {
                composable(
                    route = ConversationsNavigationItem.All.route,
                    content = {
                        AllConversationScreen(
                            newActivities = newActivities,
                            conversations = conversations
                        ) { conversationId -> conversationListViewModel.openConversation(conversationId) }
                    })
                composable(
                    route = ConversationsNavigationItem.Calls.route,
                    content = {
                        CallScreen(
                            missedCalls = missedCalls,
                            callHistory = callHistory
                        ) { conversationListViewModel.openConversation("someId") }
                    })
                composable(
                    route = ConversationsNavigationItem.Mentions.route,
                    content = {
                        MentionScreen(
                            unreadMentions = unreadMentions,
                            allMentions = allMentions
                        ) { conversationListViewModel.openConversation("someId") }
                    }
                )
            }
        }
    }
}

@Composable
private fun ConversationNavigationItems(
    uiListState: ConversationListState
): List<WireBottomNavigationItemData> {
    return ConversationsNavigationItem.values().map {
        when (it) {
            ConversationsNavigationItem.All -> it.toBottomNavigationItemData(uiListState.newActivityCount)
            ConversationsNavigationItem.Calls -> it.toBottomNavigationItemData(uiListState.missedCallsCount)
            ConversationsNavigationItem.Mentions -> it.toBottomNavigationItemData(uiListState.unreadMentionsCount)
        }
    }
}

